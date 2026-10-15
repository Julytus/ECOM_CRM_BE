package vn.aeoc.crm.controller.sale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.sale.BoughtProductService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.sale.BoughtProduct;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.List;

@RestController
public class BoughtProductController extends BaseController {

    private static final String URI = "/sale/";

    private final BoughtProductService service;

    public BoughtProductController(BoughtProductService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    /**
     * Danh sách sản phẩm trong hóa đơn (nháp / đã thanh toán)
     */
    @GetMapping("/boughtProduct/list")
    public ResponseEntity<ApiResponse<Page<BoughtProduct>>> list(
            @RequestParam(required = false) Integer invoiceId,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getByCriteria(invoiceId, status, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Chi tiết sản phẩm đã mua
     */
    @GetMapping("/boughtProduct/get")
    public ResponseEntity<ApiResponse<BoughtProduct>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getById(id), ApiResponse::okEntity);
    }

    /**
     * Thêm / cập nhật sản phẩm vào hóa đơn
     * status = 1 → hóa đơn nháp
     */
    @PostMapping("/boughtProduct/update")
    public ResponseEntity<ApiResponse<BoughtProduct>> update(
            @RequestBody BoughtProduct product,
            Principal principal
    ) {
        String action = CommonUtils.NVL(product.getId()) <= 0 ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(login.getBranchId()) > 0) {
            product.setBranchId(login.getBranchId());
        }

        BoughtProduct result = CommonUtils.NVL(product.getId()) <= 0
                ? service.insert(product)
                : service.update(product.getId(), product);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/boughtProduct/batch-upsert")
    public ResponseEntity<ApiResponse<List<BoughtProduct>>> upsertBatch(
            @RequestBody List<BoughtProduct> products,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "UPDATE");
        if (products != null && CommonUtils.NVL(login.getBranchId()) > 0) {
            Integer bid = login.getBranchId();
            products.forEach(p -> p.setBranchId(bid));
        }

        List<BoughtProduct> result = service.upsertBatch(products);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Xóa sản phẩm khỏi hóa đơn
     */
    @DeleteMapping("/boughtProduct/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        return Mapper.map(service.delete(id), ApiResponse::okEntity);
    }
}
