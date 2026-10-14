package vn.aeoc.crm.service.sale;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.sale.UnitRepository;
import vn.aeoc.entity.data.mysql.sale.Unit;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j(topic = "UNIT-SERVICE")
@Service
public class UnitService extends BaseService<
        UnitRepository,
        Unit,
        Integer> {

    protected UnitService(UnitRepository repository, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
    }

    public Page<Unit> getListUnitByCriteria(String name, Integer status, Integer branchId, Pageable pageable) {

        CompletableFuture<List<Unit>> f1 = CompletableFuture.supplyAsync(
                () -> repository.getUnitByCriteria(name, status, branchId, pageable));

        CompletableFuture<Long> f2 = CompletableFuture.supplyAsync(
                () -> repository.countUnitByCriteria(name, status, branchId));

        List<Unit> list = f1.join();
        Long total = f2.join();

        pageable.setTotal(total);
        return new Page<>(pageable, list);
    }

    public Integer updateUnitStatus(Integer id, Integer status) {
        return repository.updateUnitStatus(id, status);
    }
}
