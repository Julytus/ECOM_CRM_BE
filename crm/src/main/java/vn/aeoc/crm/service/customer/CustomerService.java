package vn.aeoc.crm.service.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.base.util.Tuple3;
import vn.aeoc.crm.repository.customer.CustomerRepository;
import vn.aeoc.crm.repository.customer.OpportunityRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;
import vn.aeoc.entity.data.mysql.customer.Customer;
import vn.aeoc.entity.data.mysql.customer.CustomerAttribute;
import vn.aeoc.entity.data.mysql.customer.CustomerExtraInfo;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService extends BaseService<
        CustomerRepository,
        Customer,
        Integer> {

    private static final Duration CUSTOMER_LIST_CACHE_TTL = Duration.ofSeconds(45);

    private final CustomerExtraInfoService extraInfoService;
    private final CustomerAttributeService attributeService;
    private final OpportunityRepository opportunityRepository;
    private final ThreeLayerCacheTemplate cacheTemplate;

    public CustomerService(
            CustomerRepository repository,
            CustomerExtraInfoService extraInfoService,
            CustomerAttributeService attributeService,
            OpportunityRepository opportunityRepository,
            ThreeLayerCacheTemplate cacheTemplate
    ) {
        super(repository);
        this.extraInfoService = extraInfoService;
        this.attributeService = attributeService;
        this.opportunityRepository = opportunityRepository;
        this.cacheTemplate = cacheTemplate;
    }

    public Page<Customer> getByCriteria(String keyword, Integer branchId, String segmentCode, Pageable pageable) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(keyword, branchId, segmentCode, pageable),
                () -> repository.countByCriteria(keyword, branchId, segmentCode)
        );
    }

    public Page<Customer> getLstExtraInfoByCriteria(String keyword, Integer branchId, String segmentCode, Pageable pageable) {
        Long cacheVersion = cacheTemplate.getLong(
                CacheKeyFactory.customerListExtraVersion(branchId),
                0L
        );

        String cacheKey = CacheKeyFactory.customerListWithExtra(
                branchId,
                keyword,
                segmentCode,
                pageable.getPage(),
                pageable.getLimit(),
                cacheVersion
        );

        return cacheTemplate.getOrLoad(cacheKey, CUSTOMER_LIST_CACHE_TTL, () -> {
            Page<Customer> page = pageQuery(
                    pageable,
                    () -> repository.getByCriteria(keyword, branchId, segmentCode, pageable),
                    () -> repository.countByCriteria(keyword, branchId, segmentCode)
            );

            List<Customer> customers = page.getItems();
            if (customers.isEmpty()) {
                return page;
            }

            List<Integer> customerIds = customers.stream()
                    .map(Customer::getId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();

            if (customerIds.isEmpty()) {
                return page;
            }

            Tuple3<
                    Map<Integer, List<CustomerExtraInfo>>,
                    Map<Integer, Integer>,
                    Map<Integer, List<CustomerAttribute>>
                    > tuple = queryExecutor.parallel3(
                    () -> extraInfoService.getCustomerExtraInfoByCustomerIds(customerIds),
                    () -> opportunityRepository.countOpportunityByCustomerIds(customerIds),
                    () -> attributeService.getAttributeMap(branchId)
            );

            Map<Integer, List<CustomerExtraInfo>> extraInfoByCustomerId = tuple.v1();
            Map<Integer, Integer> opportunityCountByCustomerId = tuple.v2();
            Map<Integer, List<CustomerAttribute>> attributeMap = tuple.v3();

            for (Customer customer : customers) {
                Integer customerId = customer.getId();
                customer.setCustomerExtraInfos(extraInfoByCustomerId.getOrDefault(customerId, List.of()));
                customer.setMapCustomerAttribute(attributeMap);
                customer.setOpportunityCount(opportunityCountByCustomerId.getOrDefault(customerId, 0));
            }

            return page;
        });
    }

    @Transactional
    public Customer insertCustomer(Customer customer) {
        var submittedExtraInfos = extraInfoService.normalizeAndValidateCustomerExtraInfos(customer, null);

        Customer customerDb = repository.insertReturning(customer);
        extraInfoService.processCustomerExtraInfo(customerDb.getId(), submittedExtraInfos);
        evictCustomerListCache(customerDb.getBranchId());
        return reloadCustomerWithExtraInfo(customerDb.getId());
    }

    @Transactional
    public Customer updateCustomer(Customer customer) {
        var submittedExtraInfos = extraInfoService.normalizeAndValidateCustomerExtraInfos(customer, customer.getId());

        Customer customerDb = repository.update(customer.getId(), customer);
        extraInfoService.processCustomerExtraInfo(customerDb.getId(), submittedExtraInfos);
        evictCustomerListCache(customerDb.getBranchId());
        return reloadCustomerWithExtraInfo(customerDb.getId());
    }

    @Transactional
    public Integer deleteCustomer(Integer id) {
        Customer customer = repository.getById(id);
        Integer branchId = customer == null ? null : customer.getBranchId();
        repository.delete(id);
        Integer deleted = extraInfoService.deleteCustomerExtraInfoByCustomerId(id);
        evictCustomerListCache(branchId);
        return deleted;
    }

    private Customer reloadCustomerWithExtraInfo(Integer customerId) {
        Customer freshCustomer = repository.getById(customerId);
        if (freshCustomer == null) {
            return null;
        }

        freshCustomer.setCustomerExtraInfos(extraInfoService.getCustomerExtraInfoByCustomerId(customerId));
        freshCustomer.setMapCustomerAttribute(attributeService.getAttributeMap(freshCustomer.getBranchId()));
        return freshCustomer;
    }

    /*
     * Bump version để tất cả key list cũ (mọi page/size/keyword) tự hết hiệu lực.
     */
    private void evictCustomerListCache(Integer branchId) {
        int scopedBranchId = branchId == null ? 0 : branchId;
        cacheTemplate.increment(CacheKeyFactory.customerListExtraVersion(scopedBranchId));

        if (scopedBranchId != 0) {
            cacheTemplate.increment(CacheKeyFactory.customerListExtraVersion(0));
        }
    }
}
