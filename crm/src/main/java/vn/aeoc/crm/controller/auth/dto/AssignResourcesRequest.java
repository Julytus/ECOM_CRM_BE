package vn.aeoc.crm.controller.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignResourcesRequest {
    private Integer moduleId;
    private List<Integer> resourceIds;
}
