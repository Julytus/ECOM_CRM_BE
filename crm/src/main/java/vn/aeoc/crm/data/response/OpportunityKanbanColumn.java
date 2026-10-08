package vn.aeoc.crm.data.response;

import lombok.Data;
import vn.aeoc.entity.data.mysql.customer.Opportunity;

import java.util.List;

@Data
public class OpportunityKanbanColumn {

    private Integer stage;
    private String stageName;
    private Integer count;
    private Integer page;
    private Integer limit;
    private Long total;
    private Boolean hasMore;
    private Double totalExpectedValue;
    private Double totalWeightedValue;
    private List<Opportunity> items;
}