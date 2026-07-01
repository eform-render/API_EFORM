package co.edu.sena.productsreact.dto.payment;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailsResponse(
        Long id,
        String customerName,
        String customerEmail,
        String paymentMethod,
        Double amount,
        String status,
        String observation,
        String paymentReferenceCode,
        LocalDateTime createdAt,
        List<OrderItemDTO> items
) {

    public record OrderItemDTO(
            Long productId,
            String productName,
            Integer quantity,
            Double productPrice
    ) {
    }
}
