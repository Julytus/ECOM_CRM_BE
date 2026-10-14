package vn.aeoc.crm.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.crm.data.request.AiChatRequest;
import vn.aeoc.crm.data.request.AiIntentResult;
import vn.aeoc.crm.data.request.ProductListRequest;
import vn.aeoc.crm.repository.product.ProductRepository;
import vn.aeoc.crm.service.OllamaService;
import vn.aeoc.entity.data.mysql.product.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AiProductController {

    private final OllamaService ollamaService;
    private final ProductRepository productRepository;

    @PostMapping("/ai/product/search")
    public ResponseEntity<?> search(@RequestBody AiChatRequest request) throws Exception {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "type", "CHAT",
                    "message", "Bạn hãy nhập nhu cầu để mình có thể tư vấn nhé.",
                    "suggestions", List.of("Tìm sản phẩm theo ngân sách", "Gợi ý sản phẩm nổi bật")
            ));
        }

        String conversation = ollamaService.buildConversation(request.getHistory(), request.getMessage());
        AiIntentResult intent = ollamaService.analyzeIntent(conversation);

        if (intent == null || !"SEARCH_PRODUCT".equalsIgnoreCase(intent.getIntent())) {
            return ResponseEntity.ok(Map.of(
                    "type", "CHAT",
                    "message", ollamaService.chat(request.getHistory(), request.getMessage()),
                    "suggestions", List.of("Tìm sản phẩm theo ngân sách", "Gợi ý sản phẩm phù hợp", "So sánh các lựa chọn")
            ));
        }

        ProductListRequest criteria = new ProductListRequest();
        if (intent.getKeywords() != null && !intent.getKeywords().isEmpty()) {
            criteria.setKeyword(String.join(" ", intent.getKeywords()));
        }
        criteria.setCategoryId(intent.getCategoryId());
        criteria.setPriceFrom(intent.getPriceFrom());
        criteria.setPriceTo(intent.getPriceTo());
        criteria.setSortBy(intent.getSortBy());
        criteria.setSortDir(intent.getSortDir());
        criteria.setActive(1);

        Pageable pageable = new Pageable();
        pageable.setPage(1);
        pageable.setLimit(Math.min(Math.max(intent.getLimit() == null ? 12 : intent.getLimit(), 3), 30));

        List<Product> products = productRepository.getByCriteria(criteria, pageable);
        if (products == null || products.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "type", "NO_RESULT",
                    "message", "Mình chưa tìm thấy sản phẩm khớp hoàn toàn với yêu cầu. Bạn thử nới rộng khoảng giá hoặc cho mình biết thêm về mục đích sử dụng nhé.",
                    "suggestions", List.of("Tìm sản phẩm giá thấp hơn", "Bỏ giới hạn ngân sách", "Gợi ý lựa chọn thay thế")
            ));
        }

        OllamaService.ProductSelection selection = ollamaService.pickBestProducts(
                request.getHistory(), request.getMessage(), products, 3
        );

        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Product product : selection.products()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("product", product);
            item.put("reason", selection.reasons().getOrDefault(
                    String.valueOf(product.getId()),
                    "Một lựa chọn đáng cân nhắc dựa trên yêu cầu bạn vừa chia sẻ."
            ));
            recommendations.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "type", "PRODUCTS",
                "message", selection.message(),
                "data", recommendations,
                "suggestions", List.of("Có mẫu rẻ hơn không?", "So sánh các sản phẩm này", "Tìm lựa chọn khác")
        ));
    }
}
