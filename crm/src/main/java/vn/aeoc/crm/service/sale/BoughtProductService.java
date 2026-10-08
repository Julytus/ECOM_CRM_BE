package vn.aeoc.crm.service.sale;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.repository.sale.BoughtProductRepository;
import vn.aeoc.entity.data.mysql.sale.BoughtProduct;

import java.util.List;

@Service
public class BoughtProductService extends BaseService<
        BoughtProductRepository,
        BoughtProduct,
        Integer> {

    public BoughtProductService(BoughtProductRepository repository) {
        super(repository);
    }

    public Page<BoughtProduct> getByCriteria(
            Integer invoiceId,
            Integer status,
            Integer branchId,
            Pageable pageable
    ) {

        return pageQuery(
                pageable,
                () -> repository.getByCriteria(invoiceId, status, branchId, pageable),
                () -> repository.countByCriteria(invoiceId, status, branchId)
        );
    }

    public List<BoughtProduct> upsertBatch(List<BoughtProduct> products) {
        return repository.upsertBatch(products);
    }

    public Integer updateDoneStatus (Integer invoiceId) {
        return repository.updateStatusByInvoiceId(invoiceId, InvoiceConstant.STATUS_DONE);
    }

    public List<BoughtProduct> getByInvoiceId(Integer invoiceId) {
        return repository.getByInvoiceId(invoiceId);
    }
}