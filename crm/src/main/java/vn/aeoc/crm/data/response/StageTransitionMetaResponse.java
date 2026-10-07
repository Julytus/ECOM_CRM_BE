package vn.aeoc.crm.data.response;

import lombok.Data;

import java.util.List;

@Data
public class StageTransitionMetaResponse {

    private List<StageItem> stages;
    private List<TransitionRule> rules;

    @Data
    public static class StageItem {
        private Integer id;
        private String code;
        private String name;
    }

    @Data
    public static class TransitionRule {
        private Integer toStage;
        private List<String> requiredFields;
    }
}