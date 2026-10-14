package vn.aeoc.crm.controller.other;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.request.ScheduleMailRequest;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.other.ScheduleService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.Schedule;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class ScheduleController extends BaseController {

    private static final String URI = "/schedule/";

    private final ScheduleService service;

    public ScheduleController(ScheduleService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    @GetMapping("/schedule/list")
    public ResponseEntity<ApiResponse<Page<Schedule>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) Integer type,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getList(keyword, customerId, type, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/schedule/get")
    public ResponseEntity<ApiResponse<Schedule>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getById(id), ApiResponse::okEntity);
    }

    @PostMapping("/schedule/update")
    public ResponseEntity<ApiResponse<Schedule>> upsert(
            @RequestBody Schedule item,
            Principal principal
    ) {
        String action = (CommonUtils.NVL(item.getId()) <= 0) ? "ADD" : "UPDATE";
        var login = checkPermission(principal, URI, action);
        item.setCreatorId(login.getId());
        item.setBranchId(login.getBranchId());

        Schedule result = (CommonUtils.NVL(item.getId()) <= 0)
                ? service.insert(item)
                : service.update(item);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/schedule/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        return Mapper.map(service.delete(id), ApiResponse::okEntity);
    }

    private Schedule mapToSchedule(ScheduleMailRequest r) {
        Schedule s = new Schedule();
        s.setId(r.getScheduleId());
        s.setTitle(r.getTitle());
        s.setContent(r.getContent());
        s.setStartTime(r.getStartTime());
        s.setEndTime(r.getEndTime());

        // JOIN thêm
        s.setCustomerName(r.getCustomerName());
        s.setCreatorName(r.getCreatorName());
        s.setBranchName(r.getBranchName());

        return s;
    }
}
