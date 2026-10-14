package vn.aeoc.crm.service.customer;

import vn.aeoc.base.util.QueryExecutor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.constant.StageConstant;
import vn.aeoc.crm.data.request.MoveOpportunityStageRequest;
import vn.aeoc.crm.data.response.OpportunityKanbanColumn;
import vn.aeoc.crm.data.response.OpportunityKanbanResponse;
import vn.aeoc.crm.data.response.StageTransitionMetaResponse;
import vn.aeoc.crm.repository.auth.UserRepository;
import vn.aeoc.crm.repository.customer.OpportunityRepository;
import vn.aeoc.crm.service.other.NotificationService;
import vn.aeoc.entity.data.mysql.Notification;
import vn.aeoc.entity.data.mysql.customer.Opportunity;
import vn.aeoc.entity.data.mysql.auth.User;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OpportunityService extends BaseService<
        OpportunityRepository,
        Opportunity,
        Integer> {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public OpportunityService(
            OpportunityRepository repository,
            UserRepository userRepository,
            NotificationService notificationService
    , QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Page<Opportunity> getByCriteria(
            String keyword, Integer customerId, Integer userId,
            Integer stage, List<Integer> priorities,
            Integer status, Boolean unassignedOnly, Integer branchId, Pageable pageable
    ) {

        return pageQuery(
                pageable,
                () -> repository.getByCriteria(keyword, customerId, userId,
                        stage, priorities, status, unassignedOnly, null, branchId, pageable),
                () -> repository.countByCriteria(keyword, customerId, userId,
                        stage, priorities,  status, unassignedOnly, null, branchId)
        );
    }

    public Integer updateStage(Integer id, Integer stage) {

        Opportunity opp = getById(id);
        validateEditableOpportunity(opp);
        validateStageTransition(opp, stage);

        if (stage.equals(StageConstant.WON)) {
            return repository.markWon(id, opp.getActualCloseDate());
        }
        if (stage.equals(StageConstant.LOST)) {
            throw new AppException("Không thể chuyển trực tiếp sang Lost bằng API updateStage. Hãy dùng API markLost/moveStage và cung cấp lostReason.", ErrorCode.BAD_REQUEST.getCode());
        }

        return repository.updateStage(id, stage);
    }

    public Integer moveStage(Integer id, MoveOpportunityStageRequest request) {

        if (request == null || request.getToStage() == null) {
            throw new AppException("Thiếu dữ liệu bắt buộc: toStage", ErrorCode.BAD_REQUEST.getCode());
        }

        Opportunity opp = getById(id);
        validateEditableOpportunity(opp);

        applyMovePayload(opp, request);
        validateStageTransition(opp, request.getToStage());

        if (request.getToStage().equals(StageConstant.LOST)) {
            if (StringUtils.isBlank(opp.getLostReason())) {
                throw new AppException("Thiếu trường bắt buộc khi chuyển sang Lost: lostReason", ErrorCode.BAD_REQUEST.getCode());
            }
            return repository.markLost(id, opp.getLostReason());
        }

        if (request.getToStage().equals(StageConstant.WON)) {
            return repository.markWon(id, opp.getActualCloseDate());
        }

        Opportunity savePayload = new Opportunity();
        savePayload.setId(id);
        savePayload.setUserId(opp.getUserId());
        savePayload.setExpectedValue(opp.getExpectedValue());
        savePayload.setActualCloseDate(opp.getActualCloseDate());
        savePayload.setProbability(opp.getProbability());
        savePayload.setExpectedCloseDate(opp.getExpectedCloseDate());
        savePayload.setLastActivityDate(opp.getLastActivityDate());
        savePayload.setPriority(opp.getPriority());
        savePayload.setNextActionType(opp.getNextActionType());
        savePayload.setNextActionDate(opp.getNextActionDate());
        savePayload.setStage(request.getToStage());

        repository.update(id, savePayload);

        return 1;
    }

    public OpportunityKanbanResponse getKanban(
            String keyword,
            Integer customerId,
            Integer userId,
            Integer stage,
            List<Integer> stages,
            List<Integer> priorities,
            Integer status,
            Integer page,
            Integer limit,
            Integer branchId
    ) {

        OpportunityKanbanResponse response = new OpportunityKanbanResponse();
        List<OpportunityKanbanColumn> columns = new ArrayList<>();

        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 12 : Math.min(limit, 50);

        List<Integer> stageList;
        if (stages != null && !stages.isEmpty()) {
            stageList = stages.stream().filter(v -> v != null && v > 0).distinct().toList();
        } else if (stage != null) {
            stageList = List.of(stage);
        } else {
            stageList = Arrays.asList(
                    StageConstant.LEAD,
                    StageConstant.CONTACTED,
                    StageConstant.CONSULTING,
                    StageConstant.PROPOSAL,
                    StageConstant.NEGOTIATION,
                    StageConstant.WON,
                    StageConstant.LOST
            );
        }

        for (Integer stageValue : stageList) {
            Pageable pageReq = new Pageable().setPage(safePage).setLimit(safeLimit);
            List<Opportunity> items = repository.getByCriteria(
                    keyword,
                    customerId,
                    userId,
                    stageValue,
                    priorities,
                    status,
                    null,
                    true,
                    branchId,
                    pageReq
            );

            OpportunityRepository.OpportunityMetrics metrics = repository.getMetricsByCriteria(
                    keyword,
                    customerId,
                    userId,
                    stageValue,
                    priorities,
                    status,
                    null,
                    true,
                    branchId
            );

            long total = metrics == null || metrics.total() == null ? 0L : metrics.total();
            double totalExpectedValue = metrics == null || metrics.totalExpectedValue() == null ? 0d : metrics.totalExpectedValue();
            double totalWeightedValue = metrics == null || metrics.totalWeightedValue() == null ? 0d : metrics.totalWeightedValue();

            OpportunityKanbanColumn column = new OpportunityKanbanColumn();
            column.setStage(stageValue);
            column.setStageName(stageName(stageValue));
            column.setItems(items);
            column.setCount(items.size());
            column.setPage(safePage);
            column.setLimit(safeLimit);
            column.setTotal(total);
            column.setHasMore((long) safePage * safeLimit < total);
            column.setTotalExpectedValue(totalExpectedValue);
            column.setTotalWeightedValue(totalWeightedValue);

            columns.add(column);
        }

        response.setColumns(columns);
        return response;
    }

    public StageTransitionMetaResponse getStageTransitionMeta() {

        StageTransitionMetaResponse response = new StageTransitionMetaResponse();

        List<StageTransitionMetaResponse.StageItem> stages = new ArrayList<>();
        stages.add(stageItem(StageConstant.LEAD, "LEAD", "Lead"));
        stages.add(stageItem(StageConstant.CONTACTED, "CONTACTED", "Contacted"));
        stages.add(stageItem(StageConstant.CONSULTING, "CONSULTING", "Consulting"));
        stages.add(stageItem(StageConstant.PROPOSAL, "PROPOSAL", "Proposal"));
        stages.add(stageItem(StageConstant.NEGOTIATION, "NEGOTIATION", "Negotiation"));
        stages.add(stageItem(StageConstant.WON, "WON", "Won"));
        stages.add(stageItem(StageConstant.LOST, "LOST", "Lost"));

        List<StageTransitionMetaResponse.TransitionRule> rules = new ArrayList<>();
        rules.add(rule(StageConstant.CONTACTED, List.of("userId")));
        rules.add(rule(StageConstant.PROPOSAL, List.of("expectedValue", "expectedCloseDate")));
        rules.add(rule(StageConstant.NEGOTIATION, List.of("lastActivityDate")));
        rules.add(rule(StageConstant.WON, List.of("actualCloseDate")));
        rules.add(rule(StageConstant.LOST, List.of("lostReason")));

        response.setStages(stages);
        response.setRules(rules);

        return response;
    }

    public Integer markLost(Integer id, String reason) {

        Opportunity opp = getById(id);
        validateEditableOpportunity(opp);

        if (StringUtils.isBlank(reason)) {
            throw new AppException("Thiếu lý do mất cơ hội: reason", ErrorCode.BAD_REQUEST.getCode());
        }

        return repository.markLost(id, reason);
    }

    public Integer markWon(Integer id) {
        Opportunity opp = getById(id);
        validateEditableOpportunity(opp);

        if (opp.getStage() == null || opp.getStage() < StageConstant.PROPOSAL) {
            throw new AppException("Chỉ được mark Won từ stage Proposal trở lên", ErrorCode.BAD_REQUEST.getCode());
        }

        return repository.markWon(id, opp.getActualCloseDate());
    }

    public Integer assignOpportunity(Integer id, Integer userId, Integer branchId) {
        Opportunity opp = getById(id);
        validateEditableOpportunity(opp);

        if (userId == null || userId <= 0) {
            throw new AppException("Thiếu userId để phân công", ErrorCode.BAD_REQUEST.getCode());
        }

        return repository.assignUser(id, userId);
    }

    public Integer distributeOpportunities(List<Integer> opportunityIds, List<Integer> userIds, Integer branchId) {
        if (opportunityIds == null || opportunityIds.isEmpty()) {
            throw new AppException("Thiếu danh sách cơ hội cần phân bổ", ErrorCode.BAD_REQUEST.getCode());
        }
        if (userIds == null || userIds.isEmpty()) {
            throw new AppException("Thiếu danh sách nhân viên cần phân bổ", ErrorCode.BAD_REQUEST.getCode());
        }

        List<Opportunity> opportunities = opportunityIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(this::getById)
                .filter(Objects::nonNull)
                .toList();

        if (opportunities.isEmpty()) {
            throw new AppException("Không tìm thấy cơ hội hợp lệ", ErrorCode.BAD_REQUEST.getCode());
        }

        List<User> users = userRepository.getUserByCriteria(null, null, new Pageable().setPage(1).setLimit(500));
        List<User> selectedUsers = users.stream()
                .filter(u -> u.getId() != null && userIds.contains(u.getId()))
                .filter(u -> u.getActive() == null || u.getActive() == 1)
                .filter(u -> branchId == null || branchId == 0 || branchId.equals(u.getBranchId()))
                .toList();

        if (selectedUsers.isEmpty()) {
            throw new AppException("Không có nhân viên hợp lệ để phân bổ", ErrorCode.BAD_REQUEST.getCode());
        }

        Map<Integer, Long> loadMap = new HashMap<>();
        for (User user : selectedUsers) {
            loadMap.put(user.getId(), 0L);
        }

        int idx = 0;
        for (Opportunity opp : opportunities) {
            User target = selectedUsers.get(idx % selectedUsers.size());
            repository.assignUser(opp.getId(), target.getId());

            Notification notification = new Notification();
            notification.setUserId(target.getId());
            notification.setBranchId(branchId);
            notification.setType(1);
            notification.setTitle("Điều phối cơ hội");
            notification.setContent("Bạn được phân bổ cơ hội: " + (opp.getName() != null ? opp.getName() : opp.getCode()));
            notification.setRefType("OPPORTUNITY");
            notification.setRefId(opp.getId());
            notification.setIsRead(0);
            notificationService.saveAndNotify(notification);

            loadMap.put(target.getId(), loadMap.getOrDefault(target.getId(), 0L) + 1);
            idx++;
        }

        return opportunities.size();
    }

    public Integer autoAssignOpportunity(Integer id, Integer branchId) {
        Opportunity opp = getById(id);
        validateEditableOpportunity(opp);

        List<User> users = userRepository.getUserByCriteria(null, null, new Pageable().setPage(1).setLimit(500));
        List<User> candidates = users.stream()
                .filter(u -> u.getId() != null)
                .filter(u -> u.getActive() == null || u.getActive() == 1)
                .filter(u -> branchId == null || branchId == 0 || branchId.equals(u.getBranchId()))
                .toList();

        if (candidates.isEmpty()) {
            throw new AppException("Không có nhân viên khả dụng để phân công", ErrorCode.BAD_REQUEST.getCode());
        }

        List<Integer> userIds = candidates.stream().map(User::getId).toList();
        Map<Integer, Integer> loadMap = repository.countActiveOpportunityByUserIds(userIds, branchId);

        User selected = candidates.stream()
                .min(Comparator.comparingInt(u -> loadMap.getOrDefault(u.getId(), 0)))
                .orElse(null);

        if (selected == null) {
            throw new AppException("Không thể xác định nhân viên để phân công", ErrorCode.BAD_REQUEST.getCode());
        }

        return repository.assignUser(id, selected.getId());
    }


    private void validateEditableOpportunity(Opportunity opp) {
        if (opp == null) {
            throw new AppException(ErrorCode.DATA_NOT_FOUND);
        }
        if (opp.getStatus() == 0) {
            throw new AppException(ErrorCode.LOCK_STATUS);
        }
    }

    private void validateStageTransition(Opportunity opp, Integer nextStage) {
        if (nextStage == null || nextStage < StageConstant.LEAD || nextStage > StageConstant.LOST) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Integer currentStage = opp.getStage();
        if (currentStage == null) {
            currentStage = StageConstant.LEAD;
        }

        if (currentStage >= StageConstant.WON) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        if (nextStage < currentStage) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        if (nextStage == StageConstant.CONTACTED && opp.getUserId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        if (nextStage == StageConstant.PROPOSAL) {
            if (opp.getExpectedValue() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            if (opp.getExpectedCloseDate() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        if (nextStage == StageConstant.NEGOTIATION) {
            LocalDateTime lastActivity = opp.getLastActivityDate();
            if (lastActivity == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        if (nextStage == StageConstant.WON) {
            if (opp.getActualCloseDate() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        if (nextStage == StageConstant.LOST) {
            if (StringUtils.isBlank(opp.getLostReason())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }
    }

    private void applyMovePayload(Opportunity opp, MoveOpportunityStageRequest request) {
        if (request.getUserId() != null) {
            opp.setUserId(request.getUserId());
        }
        if (request.getExpectedValue() != null) {
            opp.setExpectedValue(request.getExpectedValue());
        }
        if (request.getProbability() != null) {
            opp.setProbability(request.getProbability());
        }
        if (request.getExpectedCloseDate() != null) {
            opp.setExpectedCloseDate(request.getExpectedCloseDate());
        }
        if (request.getLastActivityDate() != null) {
            opp.setLastActivityDate(request.getLastActivityDate());
        }
        if (request.getActualCloseDate() != null) {
            opp.setActualCloseDate(request.getActualCloseDate());
        }
        if (!StringUtils.isBlank(request.getLostReason())) {
            opp.setLostReason(request.getLostReason());
        }
        if (request.getPriority() != null) {
            opp.setPriority(request.getPriority());
        }
        if (!StringUtils.isBlank(request.getNextActionType())) {
            opp.setNextActionType(request.getNextActionType());
        }
        if (request.getNextActionDate() != null) {
            opp.setNextActionDate(request.getNextActionDate());
        }
    }

    private Double calcWeighted(Opportunity opportunity) {
        double value = opportunity.getExpectedValue() == null ? 0d : opportunity.getExpectedValue();
        double probability = opportunity.getProbability() == null ? 0d : opportunity.getProbability();
        return value * probability / 100d;
    }

    private String stageName(Integer stage) {
        return switch (stage) {
            case StageConstant.LEAD -> "Lead";
            case StageConstant.CONTACTED -> "Contacted";
            case StageConstant.CONSULTING -> "Consulting";
            case StageConstant.PROPOSAL -> "Proposal";
            case StageConstant.NEGOTIATION -> "Negotiation";
            case StageConstant.WON -> "Won";
            case StageConstant.LOST -> "Lost";
            default -> "Unknown";
        };
    }

    private StageTransitionMetaResponse.StageItem stageItem(Integer id, String code, String name) {
        StageTransitionMetaResponse.StageItem item = new StageTransitionMetaResponse.StageItem();
        item.setId(id);
        item.setCode(code);
        item.setName(name);
        return item;
    }

    private StageTransitionMetaResponse.TransitionRule rule(Integer toStage, List<String> requiredFields) {
        StageTransitionMetaResponse.TransitionRule r = new StageTransitionMetaResponse.TransitionRule();
        r.setToStage(toStage);
        r.setRequiredFields(requiredFields);
        return r;
    }
}