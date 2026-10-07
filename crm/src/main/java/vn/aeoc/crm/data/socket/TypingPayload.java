package vn.aeoc.crm.data.socket;

import lombok.Data;

@Data
public class TypingPayload {
    private Integer chatId;
    private boolean typing;
}