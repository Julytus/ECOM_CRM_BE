package vn.aeoc.crm.controller.other;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.data.request.SendMailRequest;
import vn.aeoc.crm.service.other.EmailTemplateService;
import vn.aeoc.crm.service.other.MailService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.EmailTemplate;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@AllArgsConstructor
public class MailController {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*\\}\\}");

    private final MailService mailService;
    private final EmailTemplateService emailTemplateService;
    private final ObjectMapper objectMapper;

    @PostMapping("/mail/send")
    public ResponseEntity<ApiResponse<String>> send(@RequestBody SendMailRequest req) {
        String html = resolveHtml(req);
        mailService.send(req.getTo(), req.getSubject(), html);
        return Mapper.map("Gửi mail thành công!", ApiResponse::okEntity);
    }

    private String resolveHtml(SendMailRequest req) {
        String html = req.getHtml() == null ? "" : req.getHtml();

        if (req.getTemplateId() != null && req.getTemplateId() > 0) {
            EmailTemplate template = emailTemplateService.getById(req.getTemplateId());
            if (template != null && template.getDesignJson() != null && !template.getDesignJson().isBlank()) {
                DesignParts parts = extractDesignParts(template.getDesignJson());
                html = buildEmailDocument(parts.htmlContent, parts.cssContent);
            }
        } else {
            // Fallback an toàn: nếu FE lỡ gửi designJson thay vì full HTML thì vẫn render đúng.
            DesignParts maybeDesign = extractDesignParts(html);
            if (maybeDesign.isStructured) {
                html = buildEmailDocument(maybeDesign.htmlContent, maybeDesign.cssContent);
            }
        }

        return replaceVariables(html, req.getVariables());
    }

    private DesignParts extractDesignParts(String raw) {
        if (raw == null || raw.isBlank()) {
            return new DesignParts("", "", false);
        }

        try {
            JsonNode node = objectMapper.readTree(raw);

            // Trường hợp JSON bị double-encode: "{\"htmlContent\":...}"
            if (node.isTextual()) {
                String inner = node.asText("");
                if (inner != null && !inner.isBlank()) {
                    node = objectMapper.readTree(inner);
                }
            }

            if (node != null && node.isObject() && (node.has("htmlContent") || node.has("cssContent"))) {
                String htmlContent = node.path("htmlContent").asText("");
                String cssContent = node.path("cssContent").asText("");
                return new DesignParts(htmlContent, cssContent, true);
            }
        } catch (Exception ignored) {
            // Không phải JSON cấu trúc => coi như HTML thô.
        }

        return new DesignParts(raw, "", false);
    }

    private String buildEmailDocument(String body, String css) {
        String safeBody = body == null ? "" : body;
        String safeCss = css == null ? "" : css;

        return "<!doctype html>" +
                "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"utf-8\" />" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />" +
                "  <style>" + safeCss + "  </style>" +
                "</head>" +
                "<body>" + safeBody +"</body>" +
                "</html>";
    }

    private String replaceVariables(String html, Map<String, String> variables) {
        if (html == null || html.isBlank()) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return html;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(html);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String rawLabel = matcher.group(1) == null ? "" : matcher.group(1).trim();
            String normalized = normalize(rawLabel);
            String replacement = variables.getOrDefault(rawLabel, variables.getOrDefault(normalized, ""));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement == null ? "" : replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static class DesignParts {
        private final String htmlContent;
        private final String cssContent;
        private final boolean isStructured;

        private DesignParts(String htmlContent, String cssContent, boolean isStructured) {
            this.htmlContent = htmlContent;
            this.cssContent = cssContent;
            this.isStructured = isStructured;
        }
    }
}
