package vn.aeoc.crm.service.product;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.data.response.ProductVariantResponse;
import vn.aeoc.crm.data.response.VariantAttributeRow;
import vn.aeoc.crm.repository.product.ProductVariantRepository;
import vn.aeoc.entity.data.mysql.product.ProductVariant;

import java.util.*;

@Slf4j(topic = "PRODUCT-VARIANT-SERVICE")
@Service
public class ProductVariantService extends BaseService<
        ProductVariantRepository,
        ProductVariant,
        Integer> {

    public ProductVariantService(ProductVariantRepository repository, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
    }

    public Page<ProductVariant> getByCriteria(
            Integer productId,
            String sku,
            Integer minStock,
            Integer maxStock,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(productId, sku, minStock, maxStock, pageable),
                () -> repository.countByCriteria(productId, sku, minStock, maxStock)
        );
    }

    public List<ProductVariantResponse> getDetailByIds(List<Integer> ids) {

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        // 1. Fetch variants
        List<ProductVariant> variants =
                repository.getByIds(ids);

        if (variants.isEmpty()) {
            throw new AppException(ErrorCode.DATA_NOT_FOUND);
        }

        // 2. Fetch attributes (1 query)
        List<VariantAttributeRow> rows =
                repository.fetchVariantAttributesByVariantIds(ids);

        // 3. Group attribute by variantId
        Map<Integer, Map<String, String>> optionMap = new HashMap<>();

        for (VariantAttributeRow r : rows) {
            optionMap
                    .computeIfAbsent(r.getVariantId(), k -> new LinkedHashMap<>())
                    .put(r.getFieldName(), r.getValue());
        }

        // 4. Build response
        List<ProductVariantResponse> result = new ArrayList<>();

        for (ProductVariant v : variants) {
            Map<String, String> options =
                    optionMap.getOrDefault(v.getId(), Map.of());

            result.add(new ProductVariantResponse(v, options));
        }

        return result;
    }
}
