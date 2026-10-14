package vn.aeoc.crm.data.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatRequest {
    private String message;
    private List<ChatTurn> history = new ArrayList<>();

    @Data
    public static class ChatTurn {
        /** Expected values: user or assistant. */
        private String role;
        private String content;
    }
}
