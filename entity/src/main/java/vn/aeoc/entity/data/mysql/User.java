package vn.aeoc.entity.data.mysql;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.sql.Timestamp;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    private Integer id;

    private Integer isOperator;

    private Integer branchId;
    private String branchName;

    private Integer roleId;
    private String roleName;

    private String name;
    private String alias;

    private String avatar;

    private String phone;
    private Integer phoneVerified;

    private String email;
    private String facebookId;
    private String gplusId;

    //Mật khẩu đã băm
    private String password;

    //Mật khẩu chưa băm
    private String plainPassword;
    private Integer hasPassword;

    private Timestamp regisDate;

    //Sử dụng trong tình huống đổi mật khẩu
    private String newPassword;
    private String retypeNewPassword;

    private Integer active;

    //Là lần đầu đăng nhập
    private Integer firstTime;
    private Long time;

    //Truờng hợp vừa lấy thông tin đủ, vừa lấy về token (Login bằng fb, google)
    private String token;
    private String cityName;
    private String subdistrictName;
    private Integer gender;

    //Dùng cho khôi phục mật khẩu
    private String otp;

    //Dùng cho verify lại số điện thoại
    private String idToken;

    //Dùng để kích hoạt lại thông báo trên thiết bị
    private String deviceId;
    private String actionType;

    //Dùng để tracking login và đăng kí
    private String type; // "login" , "register"
}
