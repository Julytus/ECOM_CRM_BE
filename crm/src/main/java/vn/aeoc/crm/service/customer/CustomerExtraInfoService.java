package vn.aeoc.crm.service.customer;

import vn.aeoc.base.util.QueryExecutor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.customer.CustomerExtraInfoRepository;
import vn.aeoc.entity.data.mysql.customer.Customer;
import vn.aeoc.entity.data.mysql.customer.CustomerAttribute;
import vn.aeoc.entity.data.mysql.customer.CustomerExtraInfo;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CustomerExtraInfoService extends BaseService<
        CustomerExtraInfoRepository,
        CustomerExtraInfo,
        Integer> {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private final CustomerAttributeService attributeService;

    public CustomerExtraInfoService(
            CustomerExtraInfoRepository repository,
            CustomerAttributeService attributeService
    , QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
        this.attributeService = attributeService;
    }

    public Map<Integer, List<CustomerExtraInfo>> getCustomerExtraInfoByCustomerIds(List<Integer> customerIds) {
        return repository.getExtraInfoByCustomerIds(customerIds);
    }

    public List<CustomerExtraInfo> getCustomerExtraInfoByCustomerId(Integer customerId) {
        return repository.getCustomerExtraInfoByCustomerId(customerId);
    }

    public Integer upsertBatchCustomerExtraInfo(List<CustomerExtraInfo> list, Integer customerId) {
        return repository.upsertBatchCustomerExtraInfo(list, customerId);
    }

    public Integer deleteCustomerExtraInfos(List<CustomerExtraInfo> list, Integer customerId) {
        return repository.deleteCustomerExtraInfos(list, customerId);
    }

    public Integer deleteCustomerExtraInfoByCustomerId(Integer customerId) {
        return repository.deleteCustomerExtraInfoByCustomerId(customerId);
    }

    public List<CustomerExtraInfo> normalizeAndValidateCustomerExtraInfos(Customer customer, Integer customerId) {
        if (customer == null) {
            return List.of();
        }

        Integer branchId = customer.getBranchId();
        Map<Integer, CustomerAttribute> attributeById = loadAttributeMetadata(branchId);
        List<CustomerExtraInfo> submittedInfos = normalizeExtraInfos(customer.getCustomerExtraInfos(), customerId, attributeById);
        validateUniqueExtraInfos(submittedInfos, attributeById, customerId, branchId);
        return submittedInfos;
    }

    /**
     * Đồng bộ thông tin Customer Extra Info (upsert + delete missing)
     * - Dùng lại danh sách đã normalize/validate để tránh xử lý lặp
     */
    public void processCustomerExtraInfo(Integer customerId, List<CustomerExtraInfo> submittedInfos) {
        if (customerId == null || submittedInfos == null) {
            return;
        }

        List<CustomerExtraInfo> normalizedInfos = submittedInfos.stream()
                .filter(Objects::nonNull)
                .peek(info -> info.setCustomerId(customerId))
                .toList();

        List<CustomerExtraInfo> currentInfos = getCustomerExtraInfoByCustomerId(customerId);

        if (normalizedInfos.isEmpty()) {
            if (currentInfos != null && !currentInfos.isEmpty()) {
                deleteCustomerExtraInfos(currentInfos, customerId);
            }
            return;
        }

        if (currentInfos == null || currentInfos.isEmpty()) {
            upsertBatchCustomerExtraInfo(normalizedInfos, customerId);
            return;
        }

        Map<Integer, CustomerExtraInfo> currentByAttributeId = currentInfos.stream()
                .filter(info -> info.getAttributeId() != null)
                .collect(Collectors.toMap(
                        CustomerExtraInfo::getAttributeId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<CustomerExtraInfo> toUpsert = new ArrayList<>();

        for (CustomerExtraInfo submitted : normalizedInfos) {
            CustomerExtraInfo existing = currentByAttributeId.remove(submitted.getAttributeId());
            if (existing == null || !Objects.equals(existing.getAttributeValue(), submitted.getAttributeValue())) {
                toUpsert.add(submitted);
            }
        }

        List<CustomerExtraInfo> deleted = new ArrayList<>(currentByAttributeId.values());

        if (!toUpsert.isEmpty()) {
            upsertBatchCustomerExtraInfo(toUpsert, customerId);
        }
        if (!deleted.isEmpty()) {
            deleteCustomerExtraInfos(deleted, customerId);
        }
    }

    public void processCustomerExtraInfo(Integer customerId, Customer customer) {
        if (customerId == null || customer == null) {
            return;
        }

        processCustomerExtraInfo(customerId, normalizeAndValidateCustomerExtraInfos(customer, customerId));
    }

    private Map<Integer, CustomerAttribute> loadAttributeMetadata(Integer branchId) {
        Map<Integer, List<CustomerAttribute>> grouped = attributeService.getAttributeMap(branchId);
        if (grouped == null || grouped.isEmpty()) {
            return Map.of();
        }

        return grouped.values().stream()
                .flatMap(List::stream)
                .filter(attr -> attr.getId() != null)
                .collect(Collectors.toMap(
                        CustomerAttribute::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<CustomerExtraInfo> normalizeExtraInfos(
            List<CustomerExtraInfo> infos,
            Integer customerId,
            Map<Integer, CustomerAttribute> attributeById
    ) {
        if (infos == null || infos.isEmpty()) {
            return List.of();
        }

        return infos.stream()
                .filter(info -> info != null && info.getAttributeId() != null)
                .filter(info -> validateAgainstMetadata(info, attributeById))
                .map(info -> {
                    info.setCustomerId(customerId);
                    return info;
                })
                .collect(Collectors.toMap(
                        CustomerExtraInfo::getAttributeId,
                        Function.identity(),
                        this::chooseBetterExtraInfo,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    private boolean validateAgainstMetadata(CustomerExtraInfo info, Map<Integer, CustomerAttribute> attributeById) {
        CustomerAttribute metadata = attributeById.get(info.getAttributeId());
        if (metadata == null) {
            return false;
        }

        if (Objects.equals(metadata.getReadonly(), 1)) {
            return false;
        }

        String value = StringUtils.trimToNull(info.getAttributeValue());
        if (Objects.equals(metadata.getRequired(), 1) && value == null) {
            return false;
        }

        if (value == null) {
            info.setAttributeValue(null);
            return true;
        }

        return isValidByDatatype(metadata.getDatatype(), value);
    }

    private void validateUniqueExtraInfos(
            List<CustomerExtraInfo> infos,
            Map<Integer, CustomerAttribute> attributeById,
            Integer customerId,
            Integer branchId
    ) {
        if (infos == null || infos.isEmpty()) {
            return;
        }

        Map<Integer, List<CustomerExtraInfo>> groupedByAttribute = infos.stream()
                .filter(info -> info.getAttributeId() != null)
                .filter(info -> StringUtils.trimToNull(info.getAttributeValue()) != null)
                .collect(Collectors.groupingBy(CustomerExtraInfo::getAttributeId));

        for (Map.Entry<Integer, List<CustomerExtraInfo>> entry : groupedByAttribute.entrySet()) {
            Integer attributeId = entry.getKey();
            CustomerAttribute metadata = attributeById.get(attributeId);
            if (metadata == null || !Objects.equals(metadata.getUniqued(), 1)) {
                continue;
            }

            List<CustomerExtraInfo> sameValueItems = entry.getValue().stream()
                    .map(info -> {
                        info.setAttributeValue(StringUtils.trimToNull(info.getAttributeValue()));
                        return info;
                    })
                    .toList();

            if (sameValueItems.size() > 1) {
                throw new AppException(buildUniqueFieldError(metadata, sameValueItems.get(0).getAttributeValue()));
            }

            CustomerExtraInfo submitted = sameValueItems.get(0);
            if (repository.existsByAttributeIdAndValue(branchId, attributeId, submitted.getAttributeValue(), customerId)) {
                throw new AppException(buildUniqueFieldError(metadata, submitted.getAttributeValue()));
            }
        }
    }

    private String buildUniqueFieldError(CustomerAttribute metadata, String value) {
        String fieldName = metadata != null && StringUtils.isNotBlank(metadata.getName())
                ? metadata.getName()
                : "trường dữ liệu";
        String displayValue = StringUtils.isNotBlank(value) ? value : "(rỗng)";
        return "Giá trị của '" + fieldName + "' bị trùng: " + displayValue;
    }

    private boolean isValidByDatatype(String datatype, String value) {
        if (datatype == null) {
            return true;
        }

        String normalized = datatype.trim().toLowerCase();
        return switch (normalized) {
            case "number" -> NUMBER_PATTERN.matcher(value.replace(",", "").trim()).matches();
            case "date" -> isValidDate(value);
            case "text", "textarea", "listbox", "select", "radio", "checkbox" -> true;
            default -> true;
        };
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value.trim());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private CustomerExtraInfo chooseBetterExtraInfo(CustomerExtraInfo left, CustomerExtraInfo right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }

        boolean leftHasId = left.getId() != null;
        boolean rightHasId = right.getId() != null;

        if (leftHasId != rightHasId) {
            return rightHasId ? right : left;
        }

        if (rightHasId && !Objects.equals(left.getId(), right.getId())) {
            return right;
        }

        return right.getAttributeValue() != null ? right : left;
    }
}
