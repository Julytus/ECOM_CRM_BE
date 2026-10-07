package vn.aeoc.crm.config.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleSecurityUser {
    private Integer id;
    private String username;
    private String password;
    private String email;
    private List<Integer> roles;
    private Integer roleId;
    private String fullName;
    private String phone;
    private Boolean active;
    private String name;
}
