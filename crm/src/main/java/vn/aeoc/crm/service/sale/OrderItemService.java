package vn.aeoc.crm.service.sale;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.data.request.OrderItemRequest;
import vn.aeoc.crm.repository.product.ProductVariantRepository;
import vn.aeoc.crm.repository.sale.OrderItemRepository;
import vn.aeoc.entity.data.mysql.product.ProductVariant;
import vn.aeoc.entity.data.mysql.sale.OrderItem;

import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "ORDER-ITEM-SERVICE")
@Service
public class OrderItemService
        extends BaseService<OrderItemRepository, OrderItem, Integer> {

    private final ProductVariantRepository productVariantRepository;

    public OrderItemService(OrderItemRepository repository, ProductVariantRepository productVariantRepository, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
        this.productVariantRepository = productVariantRepository;
    }

    public Page<OrderItem> getByCriteria(Integer orderId, Pageable pageable) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(orderId, pageable),
                () -> repository.countByCriteria(orderId)
        );
    }

    @Transactional
    public Double createMany(
            Integer orderId,
            List<OrderItemRequest> requests) {

        return createMany(orderId, calculate(requests));
    }

    public Double createMany(Integer orderId, CalculatedItems calculatedItems) {
        List<OrderItem> items = calculatedItems.items().stream().map(item -> {
            OrderItem copy = new OrderItem();
            copy.setOrderId(orderId);
            copy.setProductId(item.getProductId());
            copy.setProductVariantId(item.getProductVariantId());
            copy.setPrice(item.getPrice());
            copy.setQuantity(item.getQuantity());
            copy.setTotalPrice(item.getTotalPrice());
            return copy;
        }).toList();
        repository.insertMany(items);
        return calculatedItems.subtotal();
    }

    public CalculatedItems calculate(List<OrderItemRequest> requests) {

        if ((requests == null) || requests.isEmpty()) {
            throw new AppException(ErrorCode.INPUT_WRONG);
        }

        List<OrderItem> items = new ArrayList<>();

        Double orderPrice = 0.0;

        for (OrderItemRequest req : requests) {

            if (req == null || req.getProductVariantId() == null || req.getProductVariantId() <= 0
                    || req.getQuantity() == null || req.getQuantity() <= 0) {
                throw new AppException(ErrorCode.INPUT_WRONG);
            }

            ProductVariant variant =
                    productVariantRepository.getById(req.getProductVariantId());

            if (variant == null) {
                throw new AppException(ErrorCode.DATA_NOT_FOUND);
            }

            Double price = variant.getPrice();
            Double totalPrice = price * req.getQuantity();

            orderPrice = orderPrice + totalPrice;

            OrderItem item = new OrderItem();
            item.setProductId(variant.getProductId());
            item.setProductVariantId(req.getProductVariantId());
            item.setPrice(price);
            item.setQuantity(req.getQuantity());
            item.setTotalPrice(totalPrice);

            items.add(item);
        }

        return new CalculatedItems(List.copyOf(items), orderPrice);
    }

    public record CalculatedItems(List<OrderItem> items, double subtotal) {}
}
