package vn.aeoc.crm.service.loyalty;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import vn.aeoc.crm.constant.LoyaltyConstant;
import vn.aeoc.crm.repository.loyalty.LoyaltyAccountRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyConfigRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyReconcileRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTierRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTransactionRepository;
import vn.aeoc.crm.repository.sale.CustomerVoucherRepository;
import vn.aeoc.crm.repository.sale.VoucherRepository;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.CustomerVoucher.CUSTOMER_VOUCHER;
import static vn.entity.backend.tables.LoyaltyAccount.LOYALTY_ACCOUNT;
import static vn.entity.backend.tables.LoyaltyTransaction.LOYALTY_TRANSACTION;
import static vn.entity.backend.tables.Voucher.VOUCHER;

@EnabledIfEnvironmentVariable(named = "CRM_IT_DB", matches = "true")
class LoyaltyConcurrencyIT {
    private static final int THREADS = 8;

    @Test
    void concurrentLoyaltyWritesRemainAtomicAndReconciled() throws Exception {
        try (HikariDataSource dataSource = dataSource()) {
            DSLContext dsl = DSL.using(new TransactionAwareDataSourceProxy(dataSource), SQLDialect.MYSQL);
            TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            LoyaltyAccountRepository accounts = new LoyaltyAccountRepository(dsl);
            LoyaltyTransactionRepository transactions = new LoyaltyTransactionRepository(dsl);
            LoyaltyConfigRepository config = new LoyaltyConfigRepository(dsl);
            LoyaltyTierRepository tiers = new LoyaltyTierRepository(dsl);
            LoyaltyLedgerService ledger = new LoyaltyLedgerService(accounts, transactions, tiers, config);
            LoyaltyRedeemService redeem = new LoyaltyRedeemService(dsl, accounts,
                    new VoucherRepository(dsl), ledger);
            CustomerVoucherRepository customerVouchers = new CustomerVoucherRepository(dsl);
            LoyaltyReconcileRepository reconcile = new LoyaltyReconcileRepository(dsl);

            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Integer customerId = uniqueId(dsl, "customer");
            Integer redeemVoucherId = uniqueId(dsl, "voucher");
            Integer stockVoucherId = uniqueId(dsl, "voucher");
            while (stockVoucherId.equals(redeemVoucherId)) stockVoucherId = uniqueId(dsl, "voucher");
            Integer stockVoucherFixtureId = stockVoucherId;
            Integer customerVoucherId = null;
            try {
                dsl.insertInto(CUSTOMER).set(CUSTOMER.ID, customerId).set(CUSTOMER.NAME, "it_" + suffix).execute();
                insertVoucher(dsl, redeemVoucherId, "it_" + suffix + "_redeem", 50L, null);
                insertVoucher(dsl, stockVoucherFixtureId, "it_" + suffix + "_stock", 10L, 1);
                tx.executeWithoutResult(status -> ledger.adjust(customerId, 50, 0, "it_seed_" + suffix, null));

                assertEquals(1, concurrentSuccesses(() -> {
                    tx.execute(status -> redeem.redeem(customerId, redeemVoucherId));
                    return true;
                }));

                tx.executeWithoutResult(status -> ledger.adjust(customerId, 100, 0, "it_stock_seed_" + suffix, null));
                assertEquals(1, concurrentSuccesses(() -> {
                    tx.execute(status -> redeem.redeem(customerId, stockVoucherFixtureId));
                    return true;
                }));
                assertEquals(1, dsl.select(VOUCHER.REDEEMED_COUNT).from(VOUCHER)
                        .where(VOUCHER.ID.eq(stockVoucherFixtureId)).fetchOne(VOUCHER.REDEEMED_COUNT));

                customerVoucherId = dsl.insertInto(CUSTOMER_VOUCHER)
                        .set(CUSTOMER_VOUCHER.CUSTOMER_ID, customerId)
                        .set(CUSTOMER_VOUCHER.VOUCHER_ID, redeemVoucherId)
                        .set(CUSTOMER_VOUCHER.STATUS, "AVAILABLE")
                        .set(CUSTOMER_VOUCHER.POINTS_SPENT, 0L)
                        .returning(CUSTOMER_VOUCHER.ID).fetchOne().getId();
                Integer finalCustomerVoucherId = customerVoucherId;
                assertEquals(1, concurrentSuccesses(() -> tx.execute(status -> customerVouchers
                        .markUsedIfAvailable(finalCustomerVoucherId, customerId, null) == 1)));

                int earnRate = config.getEarnRate();
                assertTrue(earnRate > 0, "dev DB must have a valid loyalty earn rate");
                assertEquals(1, concurrentSuccesses(() -> tx.execute(status -> ledger
                        .earnForOrder(customerId, customerId, earnRate * 100D).applied())));
                assertTrue(reconcile.findMismatches().isEmpty(), "ledger reconciliation returned mismatches");
            } finally {
                if (customerVoucherId != null) dsl.deleteFrom(CUSTOMER_VOUCHER)
                        .where(CUSTOMER_VOUCHER.ID.eq(customerVoucherId)).execute();
                dsl.deleteFrom(LOYALTY_TRANSACTION).where(LOYALTY_TRANSACTION.CUSTOMER_ID.eq(customerId)).execute();
                dsl.deleteFrom(LOYALTY_ACCOUNT).where(LOYALTY_ACCOUNT.CUSTOMER_ID.eq(customerId)).execute();
                dsl.deleteFrom(VOUCHER).where(VOUCHER.ID.in(redeemVoucherId, stockVoucherFixtureId)).execute();
                dsl.deleteFrom(CUSTOMER).where(CUSTOMER.ID.eq(customerId)).execute();
            }
        }
    }

    private int concurrentSuccesses(ConcurrentAction action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(15, TimeUnit.SECONDS)) return false;
                    try { return action.call(); }
                    catch (RuntimeException expectedLoser) { return false; }
                }));
            }
            assertTrue(ready.await(15, TimeUnit.SECONDS), "concurrent workers did not become ready");
            start.countDown();
            int successes = 0;
            for (Future<Boolean> future : futures) if (Boolean.TRUE.equals(future.get(60, TimeUnit.SECONDS))) successes++;
            return successes;
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void insertVoucher(DSLContext dsl, Integer id, String code, Long cost, Integer stock) {
        dsl.insertInto(VOUCHER)
                .set(VOUCHER.ID, id)
                .set(VOUCHER.CODE, code)
                .set(VOUCHER.NAME, code)
                .set(VOUCHER.DISCOUNT_TYPE, 1)
                .set(VOUCHER.DISCOUNT_VALUE, 1D)
                .set(VOUCHER.STATUS, 1)
                .set(VOUCHER.POINTS_COST, cost)
                .set(VOUCHER.REDEEM_STOCK, stock)
                .set(VOUCHER.REDEEMED_COUNT, 0)
                .execute();
    }

    private Integer uniqueId(DSLContext dsl, String table) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int candidate = 100_000_000 + (int) (Math.random() * 900_000_000);
            boolean exists = "customer".equals(table)
                    ? dsl.fetchExists(dsl.selectOne().from(CUSTOMER).where(CUSTOMER.ID.eq(candidate)))
                    : dsl.fetchExists(dsl.selectOne().from(VOUCHER).where(VOUCHER.ID.eq(candidate)));
            if (!exists) return candidate;
        }
        throw new IllegalStateException("Unable to allocate an isolated integration-test ID");
    }

    private HikariDataSource dataSource() {
        Properties dev = devProperties();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(setting("CRM_IT_DB_URL", "SPRING_DATASOURCE_URL", dev.getProperty("spring.datasource.url")));
        config.setUsername(setting("CRM_IT_DB_USERNAME", "SPRING_DATASOURCE_USERNAME",
                dev.getProperty("spring.datasource.username")));
        config.setPassword(setting("CRM_IT_DB_PASSWORD", "MYSQL_ROOT_PASSWORD",
                dev.getProperty("spring.datasource.password")));
        config.setMaximumPoolSize(THREADS + 2);
        return new HikariDataSource(config);
    }

    private Properties devProperties() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-dev.yml"));
        Properties properties = yaml.getObject();
        return properties == null ? new Properties() : properties;
    }

    private String setting(String preferredEnv, String fallbackEnv, String configured) {
        String value = System.getenv(preferredEnv);
        if (value == null || value.isBlank()) value = System.getenv(fallbackEnv);
        if (value != null && !value.isBlank()) return value;
        if (configured != null && configured.startsWith("${") && configured.endsWith("}")) {
            String expression = configured.substring(2, configured.length() - 1);
            int colon = expression.indexOf(':');
            String variable = colon < 0 ? expression : expression.substring(0, colon);
            String defaultValue = colon < 0 ? "" : expression.substring(colon + 1);
            value = System.getenv(variable);
            return value == null || value.isBlank() ? defaultValue : value;
        }
        return configured;
    }

    @FunctionalInterface
    private interface ConcurrentAction { Boolean call(); }
}
