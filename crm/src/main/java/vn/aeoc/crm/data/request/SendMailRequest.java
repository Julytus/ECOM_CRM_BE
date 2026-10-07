package vn.aeoc.crm.data.request;

import lombok.Data;

import java.util.Map;

@Data
public class SendMailRequest {
    private String to;
    private String subject;
    private String html;
    private Integer templateId;
    private Map<String, String> variables;
}
