package com.venkatesh.orderservice.service;

import com.venkatesh.orderservice.dto.*;
import com.venkatesh.orderservice.exception.InvalidOrderStatusTransitionException;
import com.venkatesh.orderservice.exception.OrderNotFoundException;
import com.venkatesh.orderservice.model.Order;
import com.venkatesh.orderservice.model.OrderItem;
import com.venkatesh.orderservice.model.OrderStatus;
import com.venkatesh.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // defines which status can move to which - keeps the logic in one place
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,   Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,   Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.debug("Creating order for userId={}", request.getUserId());

        Order order = Order.builder()
                .userId(request.getUserId())
                .shippingAddress(request.getShippingAddress())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> items = request.getItems().stream().map(itemReq -> {
            BigDecimal subtotal = itemReq.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            return OrderItem.builder()
                    .order(order)
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .subtotal(subtotal)
                    .build();
        }).toList();

        order.getItems().addAll(items);

        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        return toPagedResponse(orderPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByUserAndStatus(Long userId, OrderStatus status,
                                                                  int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        return toPagedResponse(orderPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return toPagedResponse(orderPage);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus newStatus = request.getStatus();
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(order.getStatus());

        if (!allowed.contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(
                    order.getStatus().name(), newStatus.name());
        }

        log.debug("order {} moving from {} to {}", id, order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (!ALLOWED_TRANSITIONS.get(order.getStatus()).contains(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStatusTransitionException(
                    order.getStatus().name(), OrderStatus.CANCELLED.name());
        }

        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    private PagedResponse<OrderResponse> toPagedResponse(Page<Order> page) {
        List<OrderResponse> content = page.getContent().stream()
                .map(OrderResponse::from)
                .toList();
        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

