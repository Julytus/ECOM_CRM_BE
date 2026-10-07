package vn.aeoc.crm.data.socket;

import lombok.Data;

@Data
public class MarkReadPayload {
    private Integer chatId;
    private Integer messageId;          // null = mark all
}