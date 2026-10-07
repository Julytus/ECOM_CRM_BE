package vn.aeoc.crm.controller.sale;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.sale.CampaignSnapshotService;
import vn.aeoc.crm.service.other.RagService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.CampaignSnapshot;

import java.security.Principal;

@RequiredArgsConstructor
@RestController
public class CampaignSnapshotController extends BaseController {
    private final CampaignSnapshotService service;
    private final RagService ragService;

    private static final String URI = "/campaignSnapshot/";

    @GetMapping("/campaign/list")
    public ResponseEntity<ApiResponse<Page<CampaignSnapshot>>> list(
            @RequestParam Integer customerId,
            Pageable pageable,
            Principal principal
    ) {

        checkPermission(principal, URI, "VIEW");

        var result = service.getByCriteria(
                customerId,
                pageable
        );

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/campaignSnapshot/update")
    public ResponseEntity<ApiResponse<CampaignSnapshot>> update(
            @RequestBody CampaignSnapshot campaignSnapshot,
            Principal principal
    ) {

        checkPermission(principal, URI, "ADD");

        var loading = service.getLoading(campaignSnapshot.getCustomerId());

        loading.setStatus(1);

        loading.setCampaign(campaignSnapshot.getCampaign());

        var result = service.update(loading.getId(), loading);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/campaignSnapshot/fail")
    public ResponseEntity<ApiResponse<CampaignSnapshot>> fail(
            @RequestBody CampaignSnapshot campaignSnapshot,
            Principal principal
    ) {

        checkPermission(principal, URI, "UPDATE");

        var loading = service.getLoading(campaignSnapshot.getCustomerId());

        loading.setCampaign("Đã có lỗi xảy ra");

        loading.setStatus(2);

        var result = service.update(loading.getId(), loading);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/campaign/generate/{id}")
    public ResponseEntity<ApiResponse<Object>> saveStrategy(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "UPDATE");

        CampaignSnapshot campaignSnapshot = new CampaignSnapshot(null, "Đang khởi tạo",null, id, 0);

        ragService.generate(id);
        service.insert(campaignSnapshot);
        return Mapper.map("ok", ApiResponse::okEntity);
    }

    @DeleteMapping("/campaign/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteCampaign(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = service.deleteCampaign(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

}
