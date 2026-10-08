package vn.aeoc.crm.service.sale;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.repository.sale.BoughtServiceItemRepository;
import vn.aeoc.entity.data.mysql.sale.BoughtService;

import java.util.List;

@Service
public class BoughtServiceItemService extends BaseService<BoughtServiceItemRepository, BoughtService, Integer> {

     public BoughtServiceItemService(BoughtServiceItemRepository repository) {
        super(repository);
    }

    public Page<BoughtService> getByCriteria(
            Integer invoiceId,
            Integer status,
            Integer branchId,
            Pageable pageable
    ) {
        List<BoughtService> list = repository.getByCriteria(invoiceId, status, branchId, pageable);
        Long total = repository.countByCriteria(invoiceId, status, branchId);
        pageable.setTotal(total);
        return new Page<>(pageable, list);
    }

    public List<BoughtService> upsertBatch(List<BoughtService> services) {
        return repository.upsertBatch(services);
    }

    public BoughtService getById(Integer id) {
        return repository.getById(id);
    }

    public List<BoughtService> getByInvoiceId(Integer invoiceId) {
        return repository.getByInvoiceId(invoiceId);
    }

    public Integer updateDoneStatus (Integer invoiceId) {
        return repository.updateStatusByInvoiceId(invoiceId, InvoiceConstant.STATUS_DONE);
     }

}
