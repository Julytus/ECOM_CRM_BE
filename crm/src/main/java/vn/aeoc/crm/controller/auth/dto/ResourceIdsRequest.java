package vn.aeoc.crm.controller.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResourceIdsRequest {
    private List<Integer> resourceIds;
}
