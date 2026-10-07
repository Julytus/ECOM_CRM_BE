package vn.aeoc.crm.data.socket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusEvent {
    private Integer userId;
    private String  status;             // "online" | "offline"
}