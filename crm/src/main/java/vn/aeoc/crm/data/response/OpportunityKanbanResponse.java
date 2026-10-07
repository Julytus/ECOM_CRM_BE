package vn.aeoc.crm.data.response;

import lombok.Data;

import java.util.List;

@Data
public class OpportunityKanbanResponse {

    private List<OpportunityKanbanColumn> columns;
}