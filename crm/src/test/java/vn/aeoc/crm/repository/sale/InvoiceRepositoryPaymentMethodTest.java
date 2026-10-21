package vn.aeoc.crm.repository.sale;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceRepositoryPaymentMethodTest {

    @Test
    void cashFilterMatchesLegacyPosPaymentType() {
        var repository = new InvoiceRepository(DSL.using(SQLDialect.MYSQL));
        var filter = new InvoiceRepository.InvoiceFilter(
                null, null, null, "CASH", null, null, null, null, null, 0);

        String sql = DSL.using(SQLDialect.MYSQL)
                .renderInlined(repository.getWhereCondition(filter))
                .toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("coalesce("));
        assertTrue(sql.contains("case"));
        assertTrue(sql.contains("when 1 then 'cash'"));
        assertTrue(sql.contains("when 2 then 'transfer'"));
        assertTrue(sql.contains("= 'cash'"));
    }
}
