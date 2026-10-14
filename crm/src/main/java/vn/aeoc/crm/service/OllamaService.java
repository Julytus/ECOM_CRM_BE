package vn.aeoc.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.aeoc.crm.data.request.AiChatRequest;
import vn.aeoc.crm.data.request.AiIntentResult;
import vn.aeoc.entity.data.mysql.product.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.url:https://ollama.com/api/chat}")
    private String ollamaUrl;

    @Value("${ollama.api-key}")
    private String apiKey;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.temperature:0.3}")
    private Double temperature;

    private static final String INTENT_PROMPT = """
        Bạn là bộ phân tích ý định cho trợ lý tư vấn sản phẩm thương mại điện tử.
        Xét cả ngữ cảnh hội thoại trước đó và tin nhắn mới nhất.
        Nếu khách hỏi tiếp như 'có mẫu rẻ hơn không', 'còn loại khác không', 'so sánh chúng',
        hãy coi đó là SEARCH_PRODUCT.
        Nếu là chào hỏi, hỏi cách sử dụng website hoặc trò chuyện chung, chọn CHAT.
        Trích xuất keyword sản phẩm, categoryId nếu chắc chắn, priceFrom/priceTo nếu có,
        sortBy chỉ nhận minPrice, maxPrice, avgRating; sortDir chỉ nhận asc hoặc desc.
        Chỉ trả JSON thuần, không Markdown:
        {"intent":"CHAT hoặc SEARCH_PRODUCT","keywords":[],"categoryId":null,"priceFrom":null,"priceTo":null,"sortBy":null,"sortDir":null,"limit":12}
        Không tự bịa categoryId. Giá trị không xác định để null.
        """;

    private static final String CHAT_PROMPT = """
        Bạn là Personal Concierge, chuyên gia tư vấn sản phẩm trên website thương mại điện tử.
        Trả lời bằng tiếng Việt tự nhiên, ấm áp, tinh tế, ngắn gọn và hữu ích; không lặp lại máy móc.
        Hãy sử dụng ngữ cảnh hội thoại để hiểu các câu hỏi nối tiếp. Khi chưa đủ thông tin,
        hỏi tối đa một câu làm rõ (ví dụ ngân sách, mục đích sử dụng, phong cách hoặc ưu tiên).
        Không bịa tên, giá, tồn kho hay tính năng sản phẩm. Dữ liệu sản phẩm chính thức sẽ được
        truy vấn từ hệ thống riêng. Nếu người dùng muốn tìm sản phẩm, hướng dẫn họ nêu nhu cầu.
        """;

    private static final String PICK_PROMPT = """
        Bạn là chuyên gia chọn sản phẩm. Dựa trên yêu cầu và ngữ cảnh khách hàng, chọn tối đa %d
        sản phẩm phù hợp nhất từ danh sách JSON dưới đây. Chỉ được chọn ID có trong danh sách.
        Trả về JSON thuần đúng cấu trúc:
        {"message":"một đoạn tư vấn tiếng Việt, nêu cách các lựa chọn đáp ứng nhu cầu và khuyến khích so sánh","items":[{"id":123,"reason":"lý do ngắn gọn, cụ thể dựa trên dữ liệu được cung cấp"}]}
        Không bịa thông tin không có trong danh sách. Ưu tiên các tiêu chí người dùng nói rõ.
        Yêu cầu mới nhất: %s
        Ngữ cảnh: %s
        Danh sách sản phẩm: %s
        """;

    public String buildConversation(List<AiChatRequest.ChatTurn> history, String message) {
        StringBuilder result = new StringBuilder();
        if (history != null) {
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                AiChatRequest.ChatTurn turn = history.get(i);
                if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) continue;
                String role = "assistant".equalsIgnoreCase(turn.getRole()) ? "Trợ lý" : "Khách hàng";
                result.append(role).append(": ").append(turn.getContent()).append('\n');
            }
        }
        result.append("Khách hàng: ").append(message == null ? "" : message);
        return result.toString();
    }

    public AiIntentResult analyzeIntent(String conversation) throws Exception {
        String raw = callOllama(INTENT_PROMPT + "\nHội thoại:\n" + conversation, true);
        String json = cleanJson(raw);
        try {
            return objectMapper.readValue(json, AiIntentResult.class);
        } catch (Exception ex) {
            log.warn("Could not parse Ollama intent JSON: {}", json, ex);
            AiIntentResult fallback = new AiIntentResult();
            fallback.setIntent("CHAT");
            return fallback;
        }
    }

    public String chat(List<AiChatRequest.ChatTurn> history, String message) {
        return callOllama(CHAT_PROMPT + "\n\nHội thoại:\n" + buildConversation(history, message), false).trim();
    }

    public ProductSelection pickBestProducts(
            List<AiChatRequest.ChatTurn> history,
            String userMessage,
            List<Product> candidates,
            int maxResults
    ) throws Exception {
        if (candidates == null || candidates.isEmpty()) {
            return new ProductSelection(List.of(), "Mình chưa tìm thấy sản phẩm phù hợp.", Map.of());
        }

        int limit = Math.min(Math.max(maxResults, 1), 5);
        String productsJson = objectMapper.writeValueAsString(candidates);
        String prompt = PICK_PROMPT.formatted(limit, userMessage, buildConversation(history, userMessage), productsJson);
        String raw = callOllama(prompt, true);
        JsonNode root;
        try {
            root = objectMapper.readTree(cleanJson(raw));
        } catch (Exception ex) {
            log.warn("Could not parse product selection from Ollama: {}", raw, ex);
            return fallbackSelection(candidates, limit);
        }

        Set<String> validIds = candidates.stream()
                .map(p -> String.valueOf(p.getId()))
                .collect(Collectors.toSet());
        List<Product> selected = new ArrayList<>();
        Map<String, String> reasons = new LinkedHashMap<>();
        JsonNode items = root.path("items");
        if (items.isArray()) {
            for (JsonNode item : items) {
                String id = item.path("id").asText("");
                if (!validIds.contains(id) || reasons.containsKey(id)) continue;
                candidates.stream()
                        .filter(p -> Objects.equals(String.valueOf(p.getId()), id))
                        .findFirst()
                        .ifPresent(product -> {
                            selected.add(product);
                            String reason = item.path("reason").asText("").trim();
                            reasons.put(id, reason.isBlank()
                                    ? "Một lựa chọn đáng cân nhắc theo nhu cầu của bạn."
                                    : reason);
                        });
                if (selected.size() >= limit) break;
            }
        }

        if (selected.isEmpty()) return fallbackSelection(candidates, limit);
        String message = root.path("message").asText("").trim();
        if (message.isBlank()) {
            message = "Mình đã chọn một vài lựa chọn đáng cân nhắc. Bạn có thể xem điểm phù hợp của từng sản phẩm bên dưới.";
        }
        return new ProductSelection(selected, message, reasons);
    }

    private ProductSelection fallbackSelection(List<Product> candidates, int limit) {
        List<Product> selected = candidates.stream().limit(limit).toList();
        Map<String, String> reasons = new LinkedHashMap<>();
        for (Product p : selected) {
            reasons.put(String.valueOf(p.getId()), "Được chọn từ các kết quả khớp với tiêu chí tìm kiếm của bạn.");
        }
        return new ProductSelection(
                selected,
                "Mình tìm được một vài lựa chọn để bạn tham khảo. Hãy xem giá và thông tin từng sản phẩm để chọn mẫu phù hợp nhất nhé.",
                reasons
        );
    }

    private String callOllama(String prompt, boolean jsonMode) {
        Map<String, Object> options = Map.of("temperature", temperature);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("stream", false);
        body.put("options", options);
        if (jsonMode) body.put("format", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                ollamaUrl,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<?, ?> responseBody = response.getBody();
        if (responseBody == null) throw new IllegalStateException("Ollama Cloud returned an empty response");
        Object message = responseBody.get("message");
        if (message instanceof Map<?, ?> messageMap && messageMap.get("content") instanceof String content) {
            return content;
        }
        // Support proxies which wrap the text as {"response":"..."}.
        Object responseText = responseBody.get("response");
        if (responseText instanceof String text) return text;
        throw new IllegalStateException("Unexpected Ollama Cloud response format");
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int first = cleaned.indexOf('{');
        int last = cleaned.lastIndexOf('}');
        if (first >= 0 && last > first) return cleaned.substring(first, last + 1);
        return cleaned;
    }

    public record ProductSelection(List<Product> products, String message, Map<String, String> reasons) {}
}
