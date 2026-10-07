package vn.aeoc.crm.data.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateChatRequest {
    private Boolean isGroupChat;
    private String  groupChatName;    // required when isGroupChat = true
    private String  groupChatImage;
    private List<Integer> memberIds;  // target user ids (excluding creator)
}
