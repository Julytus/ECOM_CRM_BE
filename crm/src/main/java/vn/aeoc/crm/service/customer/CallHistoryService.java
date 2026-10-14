package vn.aeoc.crm.service.customer;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.customer.CallHistoryRepository;
import vn.aeoc.entity.data.mysql.customer.CallHistory;

@Slf4j(topic = "CALL-HISTORY-SERVICE")
@Service
public class CallHistoryService extends
        BaseService<CallHistoryRepository, CallHistory, Integer> {

    protected CallHistoryService(CallHistoryRepository repository, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
    }

    public Page<CallHistory> getByCriteria(
            Integer userId,
            Integer customerId,
            Integer callType,
            Integer status,
            Integer branchId,
            Pageable pageable) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(userId, customerId, callType, status, branchId, pageable),
                () -> repository.countByCriteria(userId, customerId, callType, status, branchId)
        );
    }

    public Integer updateStatus(Integer id, Integer status) {
        return repository.updateStatus(id, status);
    }
}
