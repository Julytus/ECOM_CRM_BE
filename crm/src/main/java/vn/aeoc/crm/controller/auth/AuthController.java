package vn.aeoc.crm.controller.auth;

import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.config.property.SystemProperties;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.response.AuthResponse;
import vn.aeoc.crm.service.auth.RoleService;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.auth.Role;
import vn.aeoc.entity.data.mysql.auth.User;
import vn.aeoc.entity.data.response.TokenResponse;

import java.util.concurrent.CompletableFuture;

@RestController
public class AuthController extends BaseController {
    @Autowired
    private UserService userService;

    @Autowired
    private SystemProperties systemProperties;

    @Autowired
    private RoleService roleService;

    @PostMapping("/user/authenticate")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody User user) {
        User userDb;
        if (!CommonUtils.NVL(user.getToken()).isEmpty()) {
            userDb = validateGooleLogin(userService.authByGoogle(user.getToken()));
        } else
            userDb = validateUserAuthenticate(user);
        if (CommonUtils.NVL(userDb.getId()) <= 0) {
            throw new AppException(ErrorCode.WRONG_LOGIN_INFO);
        }

        if (userDb.getActive() == 0) throw new AppException(ErrorCode.LOCK_ACCOUNT);

        if (!CommonUtils.NVL(user.getToken()).isEmpty()) {
            var result = userService.extractToken(userDb);
            return Mapper.map(result, ApiResponse::okEntity);
        }

        var result = userService.authenticate(user, userDb, false);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Nhân viên tạo nhanh tài khoản người dùng, từ thông tin khách hàng mua hàng
     *
     * @param user
     * @return
     */


    @PostMapping("/user/create")
    public ResponseEntity<ApiResponse<AuthResponse>> create(
            @RequestBody User user) {
        if (!systemProperties.getEnabledRegister()) {
            throw new AppException(ErrorCode.REGISTRATION_DISABLED);
        }

        if (!StringUtils.isBlank(user.getPlainPassword())) {
            String password = BCrypt.hashpw(user.getPlainPassword(), BCrypt.gensalt());
            user.setPassword(password);
            user.setFirstTime(0);
        }

        Role defaultRole = roleService.getDefaultRole();
        user.setRoleId(defaultRole.getId());
        userService.register(user);
        AuthResponse authResponse = new AuthResponse(true, "Đăng ký thành công");
        return Mapper.map(authResponse, ApiResponse::okEntity);
    }


    private User validateUserAuthenticate(User user) {
        if (StringUtils.isBlank(user.getPhone()) && StringUtils.isBlank(user.getEmail())) {
            throw new AppException(ErrorCode.MISSING_EMAIL_OR_PHONE);
        }
        if (StringUtils.isBlank(user.getPlainPassword())) {
            throw new AppException(ErrorCode.MISSING_PASSWORD);
        }

        CompletableFuture<User> futureEmail = CompletableFuture.supplyAsync(
                () -> userService.getUserByEmail(user.getEmail())
        );
        CompletableFuture<User> futurePhone = CompletableFuture.supplyAsync(
                () -> userService.getUserByPhone(user.getPhone())
        );

        User userByEmail = futureEmail.join();
        User userByPhone = futurePhone.join();

        if (CommonUtils.NVL(userByEmail.getId()) > 0) return userByEmail;
        if (CommonUtils.NVL(userByPhone.getId()) > 0) return userByPhone;
        return new User();
    }

    private User validateGooleLogin(User user) {
        User userByEmail = userService.getUserByEmail(user.getEmail());

        if (CommonUtils.NVL(userByEmail.getId()) > 0) return userByEmail;
        return new User();
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody User request) {

        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Gửi mã OTP thành công"));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody User request) {

        userService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công"));
    }
}
