package vn.aeoc.crm.util;

import org.apache.commons.lang3.StringUtils;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.config.security.SimpleSecurityUser;
import vn.aeoc.crm.config.security.principal.impl.SimplePrincipal;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

public class SecurityUtils {

    /**
     * Lấy về userId từ thông tin được lưu trong principal
     *
     * @param principal
     * @return
     */
    public static Integer userId(Principal principal) {
        if (principal == null || !(principal instanceof SimplePrincipal simplePrincipal)) {
            return 0;
        }

        Integer userId = (Integer) simplePrincipal.getId();
        return userId == null ? 0 : userId;
    }

    public static boolean isApiKeyPrincipal(Principal principal) {
        if (!(principal instanceof SimplePrincipal simplePrincipal)) {
            return false;
        }
        return vn.aeoc.crm.service.auth.JWTService.PrincipalType.API_KEY_OPERATOR
                .equals(simplePrincipal.getPrincipalType());
    }

    public static String name(Principal principal) {
        if (!(principal instanceof SimplePrincipal simplePrincipal)) {
            return "";
        }

        Object otherInfo = simplePrincipal.otherInfo();

        if (otherInfo instanceof SimpleSecurityUser) {
            String name = ((SimpleSecurityUser) otherInfo).getName();
            if (name != null) {
                return name;
            } return ((SimpleSecurityUser) otherInfo).getUsername();
        }

        return "";
    }

    public static User buildApiKeyOperator() {
        User user = new User();
        user.setId(-1);
        user.setIsOperator(1);
        user.setName("API Key Operator");
        user.setRoleName(vn.aeoc.crm.service.auth.JWTService.PrincipalType.API_KEY_OPERATOR);
        return user;
    }

    public static String userName(Principal principal) {
        if (principal == null) {
            return "Unauthenticated User";
        }

        String name = principal.getName();
        return name == null ? "Unauthenticated User" : name;
    }

    public static String getJwt(Principal principal) {
        if (principal != null) {
            String bearer = ((SimplePrincipal) principal).getClientInfo().get("Authorization");
            if (StringUtils.isBlank(bearer)) {
                return "";
            }

            String jwt = "";
            if (bearer != null && bearer.startsWith("Bearer ")) jwt = bearer.substring("Bearer ".length());
            return jwt;
        }

        return "";
    }

    /**
     * Map qua SimpleSecurityUser để xác thực
     *
     * @param user
     * @return
     */
    public static SimpleSecurityUser toSimpleSecurityUser(User user) {
        String userName = user.getPhone();
        if (StringUtils.isBlank(userName)) {
            userName = CommonUtils.NVL(user.getEmail());
        }

        SimpleSecurityUser simpleSecurityUser = new SimpleSecurityUser();
        simpleSecurityUser.setId(user.getId());
        simpleSecurityUser.setUsername(userName);
        simpleSecurityUser.setName(user.getName());
        simpleSecurityUser.setRoleId(user.getRoleId());

        List<Integer> lstRole = new ArrayList<>();
        if (CommonUtils.NVL(user.getRoleId()) > 0) {
            lstRole.add(user.getRoleId());
        }
        simpleSecurityUser.setRoles(lstRole);

        return simpleSecurityUser;
    }
}
