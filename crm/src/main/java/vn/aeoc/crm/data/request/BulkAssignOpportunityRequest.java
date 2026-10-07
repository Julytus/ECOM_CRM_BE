package vn.aeoc.crm.data.request;

import lombok.Data;

import java.util.List;

@Data
public class BulkAssignOpportunityRequest {
    private List<Integer> opportunityIds;
    private List<Integer> userIds;
}
