package vn.aeoc.crm.service.sale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.data.response.InvoiceResponse;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.OrderItemRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;
import vn.aeoc.entity.data.mysql.sale.BoughtProduct;
import vn.aeoc.entity.data.mysql.sale.BoughtService;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.entity.data.mysql.sale.OrderItem;
import vn.aeoc.entity.data.mysql.sale.Payment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvoiceQueryTest {
    private InvoiceRepository invoiceRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private PaymentRepository paymentRepository;
    private BoughtProductService boughtProductService;
    private BoughtServiceItemService boughtServiceItemService;
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        boughtProductService = mock(BoughtProductService.class);
        boughtServiceItemService = mock(BoughtServiceItemService.class);
        service = new InvoiceService(invoiceRepository, mock(InvoiceVoucherRepository.class),
                mock(CustomerVoucherUsageRepository.class), new QueryExecutor(Runnable::run),
                boughtProductService, boughtServiceItemService, mock(ThreeLayerCacheTemplate.class),
                orderRepository, orderItemRepository, paymentRepository, mock(org.springframework.context.ApplicationEventPublisher.class));
    }

    @Test
    void listAcceptsMissingStatusAndPassesAllOptionalFilters() {
        when(invoiceRepository.getInvoiceByCriteria(any(), any())).thenReturn(List.of());
        when(invoiceRepository.countInvoiceByCriteria(any())).thenReturn(0L);

        service.getListByCriteria("buyer", 5, null, "PAYOS", null, null,
                "ORD-1", "createdTime", "asc", 3, new Pageable());

        var captor = org.mockito.ArgumentCaptor.forClass(InvoiceRepository.InvoiceFilter.class);
        verify(invoiceRepository).getInvoiceByCriteria(captor.capture(), any());
        assertNull(captor.getValue().status());
        assertEquals("PAYOS", captor.getValue().paymentMethod());
        assertEquals("ORD-1", captor.getValue().orderCode());
        assertEquals("createdTime", captor.getValue().sortBy());
        assertEquals("asc", captor.getValue().sortDir());
        assertEquals(3, captor.getValue().branchId());
    }

    @Test
    void orderInvoiceDetailContainsOrderItemsPaymentsAndVoucherAlongsideLegacyFields() {
        Invoice invoice = new Invoice();
        invoice.setId(11);
        invoice.setOrderId(22);
        invoice.setDiscount(15D);
        Order order = new Order();
        order.setAddress("Delivery address");
        order.setBuyerName("Buyer");
        order.setReceiverName("Receiver");
        order.setShippingFee(5D);
        order.setVoucherCode("SAVE15");
        OrderItem orderItem = new OrderItem();
        orderItem.setProductName("Product");
        Payment payment = new Payment();
        payment.setAmount(90D);
        when(invoiceRepository.getInvoiceById(11, 4)).thenReturn(invoice);
        when(orderRepository.getById(22)).thenReturn(order);
        when(orderItemRepository.fetchOrderItemDetail(22)).thenReturn(List.of(orderItem));
        when(paymentRepository.findAllByInvoiceId(11)).thenReturn(List.of(payment));
        when(boughtProductService.getByInvoiceId(11)).thenReturn(List.of(new BoughtProduct()));
        when(boughtServiceItemService.getByInvoiceId(11)).thenReturn(List.of(new BoughtService()));

        InvoiceResponse result = service.getDetailById(11, 4);

        assertSame(order, result.getOrder());
        assertEquals("Delivery address", result.getOrder().getAddress());
        assertEquals("Product", result.getOrderItems().get(0).getProductName());
        assertEquals(90D, result.getPayments().get(0).getAmount());
        assertEquals("SAVE15", result.getVoucher().getCode());
        assertEquals(15D, result.getVoucher().getDiscountAmount());
        assertEquals(1, result.getProducts().size());
        assertEquals(1, result.getServices().size());
        verify(invoiceRepository).getInvoiceById(11, 4);
    }

    @Test
    void posInvoiceDetailKeepsBoughtProductsAndServicesWithoutOrderCollections() {
        Invoice invoice = new Invoice();
        invoice.setId(12);
        when(invoiceRepository.getInvoiceById(12, 0)).thenReturn(invoice);
        when(boughtProductService.getByInvoiceId(12)).thenReturn(List.of(new BoughtProduct()));
        when(boughtServiceItemService.getByInvoiceId(12)).thenReturn(List.of(new BoughtService()));

        InvoiceResponse result = service.getDetailById(12, 0);

        assertNull(result.getOrder());
        assertTrue(result.getOrderItems().isEmpty());
        assertTrue(result.getPayments().isEmpty());
        assertEquals(1, result.getProducts().size());
        assertEquals(1, result.getServices().size());
        verifyNoInteractions(orderRepository, orderItemRepository, paymentRepository);
    }
}
