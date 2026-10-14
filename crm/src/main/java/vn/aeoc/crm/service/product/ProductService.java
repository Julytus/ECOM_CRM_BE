package vn.aeoc.crm.service.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.data.request.ProductListRequest;
import vn.aeoc.crm.data.response.*;
import vn.aeoc.crm.repository.product.ProductRepository;
import vn.aeoc.crm.repository.product.ProductVariantRepository;
import vn.aeoc.entity.data.mysql.product.AttributeValue;
import vn.aeoc.entity.data.mysql.product.Product;

import java.util.*;

@Slf4j(topic = "PRODUCT-SERVICE")
@Service
public class ProductService extends BaseService<
        ProductRepository,
        Product,
        Integer> {

    private final ProductVariantRepository productVariantRepository;

    public ProductService(ProductRepository repository, ProductVariantRepository productVariantRepository) {
        super(repository);
        this.productVariantRepository = productVariantRepository;
    }

    public Page<Product> getByCriteria(
            Integer id,
            String name,
            Integer status,
            Integer categoryId,
            Integer branchId,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(id, name, status, categoryId, branchId, pageable),
                () -> repository.countByCriteria(id, name, status, categoryId, branchId)
        );
    }

    public ProductDetailResponse getDetailById(Integer id) {

        // 1. Get product
        Product product = repository.getById(id);
        if (product == null || product.getId() == null) {
            throw new AppException(ErrorCode.DATA_NOT_FOUND);
        }

        // 2. Init response
        ProductDetailResponse response = new ProductDetailResponse(product);

        // 3. Fetch attributes + options
        List<ProductDetailRow> attributeRows =
                repository.fetchProductDetailByProductId(id);

        Map<Integer, ProductAttributeResponse> attributeMap = new LinkedHashMap<>();

        for (ProductDetailRow r : attributeRows) {

            ProductAttributeResponse attr = attributeMap.computeIfAbsent(
                    r.getAttributeId(),
                    k -> {
                        ProductAttributeResponse a = new ProductAttributeResponse();
                        a.setId(r.getAttributeId());
                        a.setFieldName(r.getAttributeCode());
                        a.setName(r.getAttributeName());
                        a.setOptions(new ArrayList<>());
                        response.getAttributes().add(a);
                        return a;
                    }
            );

            AttributeValue opt = new AttributeValue();
            opt.setId(r.getValueId());
            opt.setValue(r.getValue());
            opt.setAttributeId(r.getAttributeId());

            attr.getOptions().add(opt);
        }

        // 4. Fetch variants + options
        List<ProductVariantRow> variantRows =
                productVariantRepository.fetchVariantDetailByProductId(id);

        Map<Integer, ProductVariantResponse> variantMap = new LinkedHashMap<>();

        for (ProductVariantRow r : variantRows) {

            ProductVariantResponse variant = variantMap.computeIfAbsent(
                    r.getId(),
                    k -> {
                        ProductVariantResponse v =
                                new ProductVariantResponse(r, new LinkedHashMap<>());
                        response.getVariants().add(v);
                        return v;
                    }
            );

            variant.getOptions().put(
                    r.getFieldName(),
                    r.getValue()
            );
        }

        return response;
    }

    public Page<ProductDetailResponse> getProductDetailList(
            ProductListRequest request, Pageable pageable) {

        // 1. Query product list
        List<Product> products = repository.getByCriteria(request, pageable);

        Long total = repository.countByCriteria(request);

        if (products.isEmpty()) {
            pageable.setTotal(total);
            return new Page<>(pageable, List.of());
        }

        List<Integer> productIds = products.stream().map(Product::getId).toList();

        // 2. Query attributes + options (ALL products)
        List<ProductDetailRow> attributeRows = repository.fetchProductDetailByProductIds(productIds);

        // 3. Query variants + options (ALL products)
        List<ProductVariantRow> variantRows = productVariantRepository.fetchVariantDetailByProductIds(productIds);

        // 4. Build response
        Map<Integer, ProductDetailResponse> productMap = new LinkedHashMap<>();

        // 4.1 Init product base
        for (Product p : products) {
            ProductDetailResponse r = new ProductDetailResponse(p);
            productMap.put(p.getId(), r);
        }

        // 4.2 Attributes
        Map<Integer, Map<Integer, ProductAttributeResponse>> attrMap = new HashMap<>();

        for (ProductDetailRow r : attributeRows) {

            ProductDetailResponse product = productMap.get(r.getProductId());

            if (product == null) continue;

            Map<Integer, ProductAttributeResponse> attrs = attrMap.computeIfAbsent(
                    r.getProductId(), k -> new LinkedHashMap<>()
            );

            ProductAttributeResponse attr = attrs.computeIfAbsent(
                    r.getAttributeId(), k -> {
                        ProductAttributeResponse a = new ProductAttributeResponse();
                        a.setId(r.getAttributeId());
                        a.setFieldName(r.getAttributeCode());
                        a.setName(r.getAttributeName());
                        a.setOptions(new ArrayList<>());
                        product.getAttributes().add(a);
                        return a;
                    }
            );

            AttributeValue opt = new AttributeValue();
            opt.setId(r.getValueId());
            opt.setValue(r.getValue());
            opt.setAttributeId(r.getAttributeId());

            attr.getOptions().add(opt);
        }

        // 4.3 Variants
        Map<Integer, Map<Integer, ProductVariantResponse>> variantMap = new HashMap<>();

        for (ProductVariantRow r : variantRows) {

            ProductDetailResponse product = productMap.get(r.getProductId());

            if (product == null) continue;

            Map<Integer, ProductVariantResponse> variants = variantMap.computeIfAbsent(
                    r.getProductId(), k -> new LinkedHashMap<>()
            );

            ProductVariantResponse v = variants.computeIfAbsent(
                    r.getId(), k -> {
                        ProductVariantResponse vr = new ProductVariantResponse(r, new LinkedHashMap<>());
                        product.getVariants().add(vr);
                        return vr;
                    }
            );

            v.getOptions().put(
                    r.getFieldName(), r.getValue()
            );
        }

        pageable.setTotal(total);
        return new Page<>(pageable, new ArrayList<>(productMap.values()));
    }
}
