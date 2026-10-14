package vn.aeoc.crm.service.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.aeoc.crm.data.request.ProductAggregateRequest;
import vn.aeoc.crm.data.response.ProductAggregateResult;
import java.util.*;
import java.util.stream.Collectors;

import static vn.entity.backend.tables.Product.PRODUCT;
import static vn.entity.backend.tables.ProductAttribute.PRODUCT_ATTRIBUTE;
import static vn.entity.backend.tables.AttributeValue.ATTRIBUTE_VALUE;
import static vn.entity.backend.tables.ProductVariant.PRODUCT_VARIANT;
import static vn.entity.backend.tables.ProductVariantAttribute.PRODUCT_VARIANT_ATTRIBUTE;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProductAggregateService {
    private static final int MAX_VARIANTS = 5000;
    private final ObjectMapper objectMapper;
    private final DSLContext dsl;

    public ProductAggregateService(ObjectMapper objectMapper, DSLContext dsl) {
        this.objectMapper = objectMapper;
        this.dsl = dsl; }

    /** Create when request.id is null; otherwise synchronize the complete aggregate. */
    @Transactional
    public ProductAggregateResult save(ProductAggregateRequest request, Integer branchId) {
        validate(request);
        Integer productId = request.getId();
        boolean creating = productId == null || productId <= 0;
        Double minPrice = safe(request.getVariants()).stream()
                .map(ProductAggregateRequest.VariantInput::getPrice)
                .min(Double::compareTo).orElse(0.0);
        Double maxPrice = safe(request.getVariants()).stream()
                .map(ProductAggregateRequest.VariantInput::getPrice)
                .max(Double::compareTo).orElse(0.0);
        Double originalMinPrice = safe(request.getVariants()).stream()
                .map(ProductAggregateRequest.VariantInput::getOriginalPrice)
                .min(Double::compareTo).orElse(0.0);
        Double originalMaxPrice = safe(request.getVariants()).stream()
                .map(ProductAggregateRequest.VariantInput::getOriginalPrice)
                .max(Double::compareTo).orElse(0.0);

        if (creating) {
            productId = dsl.insertInto(PRODUCT)
                    .set(PRODUCT.NAME, request.getName().trim())
                    .set(PRODUCT.DESCRIPTION, request.getDescription())
                    .set(PRODUCT.CATEGORY_ID, request.getCategoryId())
                    .set(PRODUCT.STATUS, request.getStatus())
                    .set(PRODUCT.THUMBNAIL, request.getThumbnail())
                    .set(PRODUCT.IMAGE, toJson(request.getImage()))
                    .set(PRODUCT.BRANCH_ID, branchId)
                    .returningResult(PRODUCT.ID).fetchOne().get(PRODUCT.ID);
        } else {
            var product = dsl.select(PRODUCT.ID)
                    .from(PRODUCT)
                    .where(PRODUCT.ID.eq(productId))
                    .and(branchId == null ? org.jooq.impl.DSL.trueCondition() : PRODUCT.BRANCH_ID.eq(branchId))
                    .fetchOne();
            if (product == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm hoặc không thuộc chi nhánh hiện tại");
            dsl.update(PRODUCT)
                    .set(PRODUCT.NAME, request.getName().trim())
                    .set(PRODUCT.DESCRIPTION, request.getDescription())
                    .set(PRODUCT.CATEGORY_ID, request.getCategoryId())
                    .set(PRODUCT.STATUS, request.getStatus())
                    .set(PRODUCT.MIN_PRICE, minPrice)
                    .set(PRODUCT.MAX_PRICE, maxPrice)
                    .set(PRODUCT.ORIGINAL_MIN_PRICE, originalMinPrice)
                    .set(PRODUCT.ORIGINAL_MAX_PRICE, originalMaxPrice)
                    .set(PRODUCT.THUMBNAIL, request.getThumbnail())
                    .set(PRODUCT.IMAGE, toJson(request.getImage()))
                    .where(PRODUCT.ID.eq(productId)).execute();
        }

        // Existing child IDs are only accepted when they belong to this product.
        Map<Integer, Integer> existingAttributeIds = dsl.select(PRODUCT_ATTRIBUTE.ID, PRODUCT_ATTRIBUTE.ID)
                .from(PRODUCT_ATTRIBUTE).where(PRODUCT_ATTRIBUTE.PRODUCT_ID.eq(productId))
                .fetchMap(PRODUCT_ATTRIBUTE.ID, PRODUCT_ATTRIBUTE.ID);
        Map<String, Integer> attributeIdsByField = new LinkedHashMap<>();
        Set<Integer> retainedAttributeIds = new HashSet<>();
        Map<Integer, Set<Integer>> retainedOptionIdsByAttribute = new HashMap<>();
        int optionCount = 0;

        for (var attr : safe(request.getAttributes())) {
            String fieldName = attr.getFieldName().trim();
            Integer attributeId = attr.getId();
            if (attributeId != null && attributeId > 0) {
                if (!existingAttributeIds.containsKey(attributeId)) throw new IllegalArgumentException("Attribute không thuộc sản phẩm: " + attributeId);
                dsl.update(PRODUCT_ATTRIBUTE).set(PRODUCT_ATTRIBUTE.NAME, attr.getName().trim())
                        .set(PRODUCT_ATTRIBUTE.FIELD_NAME, fieldName)
                        .where(PRODUCT_ATTRIBUTE.ID.eq(attributeId)).execute();
            } else {
                attributeId = dsl.insertInto(PRODUCT_ATTRIBUTE)
                        .set(PRODUCT_ATTRIBUTE.PRODUCT_ID, productId)
                        .set(PRODUCT_ATTRIBUTE.NAME, attr.getName().trim())
                        .set(PRODUCT_ATTRIBUTE.FIELD_NAME, fieldName)
                        .returningResult(PRODUCT_ATTRIBUTE.ID).fetchOne().get(PRODUCT_ATTRIBUTE.ID);
            }
            if (attributeIdsByField.put(fieldName, attributeId) != null) throw new IllegalArgumentException("fieldName bị trùng: " + fieldName);
            retainedAttributeIds.add(attributeId);

            Map<Integer, Integer> existingOptions = dsl.select(ATTRIBUTE_VALUE.ID, ATTRIBUTE_VALUE.ID)
                    .from(ATTRIBUTE_VALUE).where(ATTRIBUTE_VALUE.ATTRIBUTE_ID.eq(attributeId))
                    .fetchMap(ATTRIBUTE_VALUE.ID, ATTRIBUTE_VALUE.ID);
            Set<Integer> retainedOptionIds = new HashSet<>();
            retainedOptionIdsByAttribute.put(attributeId, retainedOptionIds);

            for (var option : safe(attr.getOptions())) {
                String optionValue = option.getValue().trim();
                Integer optionId = option.getId();
                if (optionId != null && optionId > 0) {
                    if (!existingOptions.containsKey(optionId)) throw new IllegalArgumentException("Option không thuộc attribute " + fieldName + ": " + optionId);
                    dsl.update(ATTRIBUTE_VALUE).set(ATTRIBUTE_VALUE.VALUE, optionValue)
                            .where(ATTRIBUTE_VALUE.ID.eq(optionId)).execute();
                } else {
                    optionId = dsl.insertInto(ATTRIBUTE_VALUE)
                            .set(ATTRIBUTE_VALUE.ATTRIBUTE_ID, attributeId)
                            .set(ATTRIBUTE_VALUE.VALUE, optionValue)
                            .returningResult(ATTRIBUTE_VALUE.ID).fetchOne().get(ATTRIBUTE_VALUE.ID);
                }
                retainedOptionIds.add(optionId);
                optionCount++;
            }
            if (retainedOptionIds.isEmpty()) throw new IllegalArgumentException("Attribute phải có ít nhất một option: " + fieldName);
        }

        // Build the canonical fieldName/value -> optionId map after all option inserts/updates.
        Map<String, Integer> optionIds = dsl.select(
                        PRODUCT_ATTRIBUTE.FIELD_NAME, ATTRIBUTE_VALUE.VALUE, ATTRIBUTE_VALUE.ID)
                .from(ATTRIBUTE_VALUE)
                .join(PRODUCT_ATTRIBUTE).on(PRODUCT_ATTRIBUTE.ID.eq(ATTRIBUTE_VALUE.ATTRIBUTE_ID))
                .where(PRODUCT_ATTRIBUTE.PRODUCT_ID.eq(productId))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        r -> optionKey(r.get(PRODUCT_ATTRIBUTE.FIELD_NAME), r.get(ATTRIBUTE_VALUE.VALUE)),
                        r -> r.get(ATTRIBUTE_VALUE.ID),
                        (first, duplicate) -> first,
                        LinkedHashMap::new
                ));

        Set<Integer> retainedVariantIds = new HashSet<>();
        Set<String> incomingSkus = new HashSet<>();
        List<Query> linkQueries = new ArrayList<>();

        Map<Integer, Integer> existingVariantIds = dsl.select(PRODUCT_VARIANT.ID, PRODUCT_VARIANT.ID)
                .from(PRODUCT_VARIANT).where(PRODUCT_VARIANT.PRODUCT_ID.eq(productId))
                .fetchMap(PRODUCT_VARIANT.ID, PRODUCT_VARIANT.ID);

        for (var variant : safe(request.getVariants())) {
            String sku = variant.getSku().trim();
            if (!incomingSkus.add(sku.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("SKU bị trùng trong request: " + sku);
            Integer variantId = variant.getId();
            if (variantId != null && variantId > 0) {
                if (!existingVariantIds.containsKey(variantId)) throw new IllegalArgumentException("Variant không thuộc sản phẩm: " + variantId);
                dsl.update(PRODUCT_VARIANT)
                        .set(PRODUCT_VARIANT.SKU, sku)
                        .set(PRODUCT_VARIANT.PRICE, variant.getPrice())
                        .set(PRODUCT_VARIANT.ORIGINAL_PRICE, variant.getOriginalPrice())
                        .set(PRODUCT_VARIANT.STOCK, variant.getStock())
                        .set(PRODUCT_VARIANT.IMAGE, variant.getImage())
                        .where(PRODUCT_VARIANT.ID.eq(variantId)).execute();
            } else {
                variantId = dsl.insertInto(PRODUCT_VARIANT)
                        .set(PRODUCT_VARIANT.PRODUCT_ID, productId)
                        .set(PRODUCT_VARIANT.SKU, sku)
                        .set(PRODUCT_VARIANT.PRICE, variant.getPrice())
                        .set(PRODUCT_VARIANT.ORIGINAL_PRICE, variant.getOriginalPrice())
                        .set(PRODUCT_VARIANT.STOCK, variant.getStock())
                        .set(PRODUCT_VARIANT.IMAGE, variant.getImage())
                        .returningResult(PRODUCT_VARIANT.ID).fetchOne().get(PRODUCT_VARIANT.ID);
            }
            retainedVariantIds.add(variantId);

            Map<String, String> selectedOptions = variant.getOptions() == null ? Map.of() : variant.getOptions();
            if (selectedOptions.size() != attributeIdsByField.size()) {
                throw new IllegalArgumentException("Variant " + sku + " phải có đúng một option cho mỗi attribute");
            }
            // Replace links for this variant, preserving the variant ID itself.
            dsl.deleteFrom(PRODUCT_VARIANT_ATTRIBUTE)
                    .where(PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.eq(variantId)).execute();
            for (String fieldName : attributeIdsByField.keySet()) {
                String value = selectedOptions.get(fieldName);
                if (value == null) throw new IllegalArgumentException("Thiếu option " + fieldName + " cho SKU " + sku);
                Integer optionId = optionIds.get(optionKey(fieldName, value));
                if (optionId == null) throw new IllegalArgumentException("Option không hợp lệ: " + fieldName + "=" + value);
                linkQueries.add(dsl.insertInto(PRODUCT_VARIANT_ATTRIBUTE)
                        .set(PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID, variantId)
                        .set(PRODUCT_VARIANT_ATTRIBUTE.ATTRIBUTE_VALUE_ID, optionId));
            }
        }
        if (!linkQueries.isEmpty()) dsl.batch(linkQueries).execute();

        // Delete variants omitted from the submitted complete aggregate. Keep this behavior explicit in the UI docs.
        List<Integer> removedVariantIds = existingVariantIds.keySet().stream()
                .filter(id -> !retainedVariantIds.contains(id)).toList();
        if (!removedVariantIds.isEmpty()) {
            dsl.deleteFrom(PRODUCT_VARIANT_ATTRIBUTE).where(PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.in(removedVariantIds)).execute();
            dsl.deleteFrom(PRODUCT_VARIANT).where(PRODUCT_VARIANT.ID.in(removedVariantIds)).execute();
        }

        // Remove options omitted from the request, unlinking them first.
        for (var entry : retainedOptionIdsByAttribute.entrySet()) {
            List<Integer> oldOptionIds = dsl.select(ATTRIBUTE_VALUE.ID).from(ATTRIBUTE_VALUE)
                    .where(ATTRIBUTE_VALUE.ATTRIBUTE_ID.eq(entry.getKey()))
                    .fetch(ATTRIBUTE_VALUE.ID);
            List<Integer> removed = oldOptionIds.stream().filter(id -> !entry.getValue().contains(id)).toList();
            deleteOptions(removed);
        }

        // Remove attributes omitted from the request, after their option links are cleaned up.
        List<Integer> removedAttributeIds = existingAttributeIds.keySet().stream()
                .filter(id -> !retainedAttributeIds.contains(id)).toList();
        if (!removedAttributeIds.isEmpty()) {
            List<Integer> optionIdsToDelete = dsl.select(ATTRIBUTE_VALUE.ID).from(ATTRIBUTE_VALUE)
                    .where(ATTRIBUTE_VALUE.ATTRIBUTE_ID.in(removedAttributeIds)).fetch(ATTRIBUTE_VALUE.ID);
            deleteOptions(optionIdsToDelete);
            dsl.deleteFrom(PRODUCT_ATTRIBUTE).where(PRODUCT_ATTRIBUTE.ID.in(removedAttributeIds)).execute();
        }

        return new ProductAggregateResult(productId, retainedAttributeIds.size(), optionCount,
                retainedVariantIds.size(), linkQueries.size());
    }

    private void deleteOptions(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        dsl.deleteFrom(PRODUCT_VARIANT_ATTRIBUTE).where(PRODUCT_VARIANT_ATTRIBUTE.ATTRIBUTE_VALUE_ID.in(ids)).execute();
        dsl.deleteFrom(ATTRIBUTE_VALUE).where(ATTRIBUTE_VALUE.ID.in(ids)).execute();
    }

    private void validate(ProductAggregateRequest request) {
        if (request == null) throw new IllegalArgumentException("Request không được null");
        if (request.getVariants() == null) request.setVariants(new ArrayList<>());
        if (request.getAttributes() == null) request.setAttributes(new ArrayList<>());
        if (request.getVariants().size() > MAX_VARIANTS) throw new IllegalArgumentException("Tối đa " + MAX_VARIANTS + " variants mỗi request");
        Set<String> fields = new HashSet<>();
        for (var attr : request.getAttributes()) {
            if (attr.getFieldName() == null || attr.getFieldName().isBlank()) throw new IllegalArgumentException("fieldName không được trống");
            if (!fields.add(attr.getFieldName().trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("fieldName bị trùng: " + attr.getFieldName());
            Set<String> values = new HashSet<>();
            for (var option : safe(attr.getOptions())) {
                if (option.getValue() == null || option.getValue().isBlank()) throw new IllegalArgumentException("Option không được trống");
                if (!values.add(option.getValue().trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Option bị trùng trong attribute " + attr.getFieldName() + ": " + option.getValue());
            }
        }
        Set<String> skus = new HashSet<>();
        for (var variant : request.getVariants()) {
            if (variant.getSku() == null || variant.getSku().isBlank()) throw new IllegalArgumentException("SKU không được trống");
            if (!skus.add(variant.getSku().trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("SKU bị trùng: " + variant.getSku());
            if (variant.getPrice() == null || variant.getPrice() < 0 || variant.getOriginalPrice() == null || variant.getOriginalPrice() < 0 || variant.getStock() == null || variant.getStock() < 0) throw new IllegalArgumentException("Giá và tồn kho không được âm");
            Map<String, String> options = variant.getOptions() == null ? Map.of() : variant.getOptions();
            if (options.size() != fields.size()) throw new IllegalArgumentException("Số option không khớp số attribute: " + variant.getSku());
            if (!fields.containsAll(options.keySet().stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()))) {
                // Field names are case-sensitive in the payload contract; also reject unknown keys.
                if (!fields.containsAll(options.keySet().stream().map(s -> s.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toSet()))) throw new IllegalArgumentException("Variant chứa fieldName không tồn tại: " + variant.getSku());
            }
        }
    }

    private static <T> List<T> safe(List<T> list) { return list == null ? List.of() : list; }
    private static String optionKey(String fieldName, String value) {
        return fieldName.trim().toLowerCase(Locale.ROOT) + "::" + value.trim().toLowerCase(Locale.ROOT);
    }

    private String toJson(List<String> images) {
        try {
            return objectMapper.writeValueAsString(
                    images == null ? List.of() : images
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize product images", e);
        }
    }
}
