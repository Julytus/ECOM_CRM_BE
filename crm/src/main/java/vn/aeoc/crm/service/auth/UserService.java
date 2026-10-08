package vn.aeoc.crm.service.auth;

import com.google.gson.Gson;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Cursor;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.constant.EmailContent;
import vn.aeoc.crm.repository.auth.RoleRepository;
import vn.aeoc.crm.repository.auth.UserRepository;
import vn.aeoc.crm.service.RestService;
import vn.aeoc.entity.data.mysql.auth.Role;
import vn.aeoc.entity.data.mysql.auth.User;
import vn.aeoc.entity.data.response.TokenResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static vn.aeoc.crm.util.SecurityUtils.toSimpleSecurityUser;

@Slf4j(topic = "USER-SERVICE")
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final JWTService jwtService;
    private final RoleRepository roleRepository;
    private final JavaMailSender mailSender;
    private final RestService restService;
    private final RoleService roleService;

    public User getUserById(Integer userId) {
        return userRepository.getUserById(userId);
    }

    public User checkPermission(Integer userId, String URI, String action) {
        User user = getUserById(userId);
        return checkUserPermission(URI, action, user);
    }

    private User checkUserPermission(String URI, String action, User user) {
        Integer roleId = user.getRoleId();
        if (CommonUtils.NVL(roleId) <= 0) return new User();

        Role role = roleService.getRoleById(roleId);
        if (role == null) {
            return new User();
        }

        if (Integer.valueOf(1).equals(role.getIsOperator())) return user;

        Long totalPer = permissionService.checkPermission(roleId, URI, action);
        if (CommonUtils.NVL(totalPer) <= 0) {
            log.error("Lỗi không có quyền, roleId {}, URI {}, action {}",
                    roleId, URI, action);
            return new User();
        }
        return user;
    }

    public User register(User user) {
        User userDB = userRepository.getUserByPhone(user.getPhone());

        if (userDB != null && CommonUtils.NVL(userDB.getId()) > 0) {
            throw new AppException(ErrorCode.DUPLICATED_USER);
        }
        User newUser = userRepository.register(user);
        newUser.setType("register");
        newUser.setActionType("register");
        newUser.setTime(System.currentTimeMillis());
        return newUser;
    }

    public User insertUser(User user) {
        if (userRepository.existByPhoneExceptId(user.getPhone(), user.getId())) {
            throw new AppException(ErrorCode.DUPLICATED_USER);
        }

        if (userRepository.existByEmail(user.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATED_EMAIL);
        }

        if (user.getPlainPassword() != null && !user.getPlainPassword().isEmpty()) {
            String encryptedPassword = BCrypt.hashpw(user.getPlainPassword(), BCrypt.gensalt());
            user.setPassword(encryptedPassword);
        }

        return userRepository.insertUser(user);
    }

    public User updateUser(User user) {
        if (!CommonUtils.NVL(user.getPlainPassword()).isEmpty()) {
            String encryptedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            user.setPassword(encryptedPassword);
        }

        if (user.getPlainPassword() != null && !user.getPlainPassword().isEmpty()) {
            String encryptedPassword = BCrypt.hashpw(user.getPlainPassword(), BCrypt.gensalt());
            user.setPassword(encryptedPassword);
        }

        if (userRepository.existByEmailExceptId(user.getEmail(), user.getId())) {
            throw new AppException(ErrorCode.DUPLICATED_EMAIL);
        }

        if (userRepository.existByPhoneExceptId(user.getPhone(), user.getId())) {
            throw new AppException(ErrorCode.DUPLICATED_USER);
        }

        return userRepository.updateUser(user);
    }

    public User updateInfo(User user) {
        if (userRepository.existByEmailExceptId(user.getEmail(), user.getId())) {
            throw new AppException(ErrorCode.DUPLICATED_EMAIL);
        }

        if (userRepository.existByPhoneExceptId(user.getPhone(), user.getId())) {
            throw new AppException(ErrorCode.DUPLICATED_USER);
        }

        return userRepository.updateInfo(user);
    }

    public User updateUserPassword(Integer id, String oldPassword, String newPassword) {
        if (CommonUtils.NVL(id) <= 0) {
            throw new AppException("Người dùng không hợp lệ", BAD_REQUEST.value());
        }

        if (StringUtils.isAnyBlank(oldPassword, newPassword)) {
            throw new AppException("Mật khẩu cũ và mật khẩu mới không được để trống", BAD_REQUEST.value());
        }

        if (newPassword.length() < 3) {
            throw new AppException("Mật khẩu mới phải có ít nhất 3 ký tự", BAD_REQUEST.value());
        }

        if (oldPassword.equals(newPassword)) {
            throw new AppException("Mật khẩu mới phải khác mật khẩu cũ", BAD_REQUEST.value());
        }

        String currentPassword = userRepository.getUserPasswordById(id);
        if (StringUtils.isBlank(currentPassword)) {
            throw new AppException("Không tìm thấy người dùng", BAD_REQUEST.value());
        }

        if (!BCrypt.checkpw(oldPassword, currentPassword)) {
            throw new AppException("Mật khẩu cũ không chính xác", BAD_REQUEST.value());
        }

        String encryptedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        User user = new User();
        user.setId(id);
        user.setPassword(encryptedPassword);
        return userRepository.updatePass(user);
    }

    public Integer deleteUser(Integer id) {
        return userRepository.deleteUser(id);
    }

    public Integer updateUserStatus(Integer id, Integer active) {
        return userRepository.updateUserStatus(id, active);
    }

    public TokenResponse authenticate(User user, User userDb, Boolean isRefresh) {
        userDb.setType("login");
        return this.getTokenResponse(user, userDb, isRefresh);
    }

    private TokenResponse getTokenResponse(User user, User userDb, Boolean isRefresh) {
        return this.processAuthenticate(user, userDb, isRefresh);
    }

    public TokenResponse processAuthenticate(User user, User userDb, Boolean isRefresh) {
        return extractToken(user, userDb, isRefresh);
    }

    public User authByGoogle(String token) {
        String googleAuthUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

        // Tạo header Authorization Bearer token theo format mà RestService yêu cầu
        String httpHeaders = String.format("""
            [
                {
                    "key": "Authorization",
                    "value": "Bearer %s"
                }
            ]
            """, token);

        try {
            // Gọi API Google với header
            String response = restService.callApiFromPartner(
                    googleAuthUrl,
                    "GET",
                    null,           // authentication (không dùng)
                    null,           // defaultJwt (không dùng)
                    httpHeaders,    // ← Truyền header Authorization vào đây
                    null            // jsonObject (GET không cần body)
            );

            System.out.println("Google UserInfo Response: " + response);

            // Parse JSON response thành User object
            User user = new Gson().fromJson(response, User.class);

            return user;

        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Google UserInfo API: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể xác thực với Google", e);
        }
    }

    public TokenResponse extractToken(User user) {
        return new TokenResponse(jwtService.generateJwt(toSimpleSecurityUser(user)));
    }


    /**
     * Kiểm tra có đăng nhập thành công hay không
     *
     * @param user
     * @param userDb
     * @return
     */
    private TokenResponse extractToken(User user, User userDb, Boolean isRefresh) {
        if (isRefresh) {
            return new TokenResponse(jwtService.generateJwt(toSimpleSecurityUser(userDb)), userDb.getId());
        }

        Boolean validPassword = checkValidAuthenticationInfo(user, userDb);

        if (!validPassword) {
            throw new AppException(ErrorCode.INVALID_AUTHENTICATION_INFO);
        }
        return new TokenResponse(jwtService.generateJwt(toSimpleSecurityUser(userDb)), userDb.getId());
    }

    private Boolean checkValidAuthenticationInfo(User user, User userDb) {
            if (user == null || user.getPlainPassword() == null || userDb.getPassword() == null) {
                throw new AppException(ErrorCode.INVALID_AUTHENTICATION_INFO);
            }
            return BCrypt.checkpw(user.getPlainPassword(), userDb.getPassword());
    }

    public Page<User> getListUserByCriteria(List<Integer> lstUserId, String query,
                                            Pageable pageable) {
        CompletableFuture<List<User>> futureLst = CompletableFuture.supplyAsync(
                () -> userRepository.getUserByCriteria(lstUserId, query, pageable)
        );
        CompletableFuture<Long> futureCount = CompletableFuture.supplyAsync(
                () -> userRepository.countUserByCriteria(lstUserId, query)
        );

        List<User> lst = futureLst.join();
        Long count = futureCount.join();

        pageable.setTotal(count);
        return new Page<>(pageable, lst);
    }

    public Page<User> getListUserByCriteria(List<Integer> lstUserId,
                                            Integer branchId,
                                            String query,
                                            Pageable pageable, Integer excludeUserId) {
        CompletableFuture<List<User>> futureLst = CompletableFuture.supplyAsync(
                () -> userRepository.getUserByCriteria(lstUserId, branchId, query, pageable, excludeUserId)
        );
        CompletableFuture<Long> futureCount = CompletableFuture.supplyAsync(
                () -> userRepository.countUserByCriteria(lstUserId, branchId, query, excludeUserId)
        );

        List<User> lst = futureLst.join();
        Long count = futureCount.join();

        pageable.setTotal(count);
        return new Page<>(pageable, lst);
    }

    public User getUserByPhone(String phone) {
        return userRepository.getUserByPhone(phone);
    }

    public User getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    public void forgotPassword(String email) {

        if (StringUtils.isBlank(email)) {
            throw new AppException("Email không được để trống", BAD_REQUEST.value());
        }

        User user = userRepository.getUserByEmail(email);

        if (user == null || CommonUtils.NVL(user.getId()) <= 0) {
            throw new AppException("Email không tồn tại trong hệ thống", BAD_REQUEST.value());
        }

        if (CommonUtils.NVL(user.getActive()) != 1) {
            throw new AppException("Tài khoản đang bị khóa", BAD_REQUEST.value());
        }

        String otp = CommonUtils.randomNumber(6);
        userRepository.updateOtp(user.getId(), otp);

        sendEmail(email,EmailContent.buildForgotPasswordEmail(user.getName(), otp));
    }

    public void resetPassword(String email, String otp, String newPassword) {

        if (StringUtils.isAnyBlank(email, otp, newPassword)) {
            throw new AppException("Thông tin không hợp lệ", BAD_REQUEST.value());
        }

        User user = userRepository.getUserByEmailAndOtp(email, otp);

        if (user == null || CommonUtils.NVL(user.getId()) <= 0) {
            throw new AppException("Sai mã OTP", BAD_REQUEST.value());
        }

        if (newPassword.length() < 3) {
            throw new AppException("Mật khẩu phải có ít nhất 3 ký tự", BAD_REQUEST.value());
        }

        String encryptedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPassword(encryptedPassword);

        userRepository.updatePass(user);
        userRepository.clearOtp(user.getId());
    }

    public void sendEmail(String email, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("CRM System - Khôi phục mật khẩu");
            helper.setText(content, true );

            mailSender.send(message);
            log.info("Sending email to {} with content: {}", email, content);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", email, e.getMessage());
            throw new AppException("Gửi email thất bại", BAD_REQUEST.value());
        }
    }

    public Cursor<User> getUserCursor(Integer beforeUserId, String keyword, int limit, Integer excludeUserId) {
        List<User> users = userRepository.getUserCursor(beforeUserId, keyword, limit + 1, excludeUserId);
        return new Cursor<>(users, limit, User::getId);
    }
}
