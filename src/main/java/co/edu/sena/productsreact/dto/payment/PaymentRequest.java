package co.edu.sena.productsreact.dto.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record PaymentRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        String customerName,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser válido")
        String customerEmail,
        @NotBlank(message = "El método de pago es obligatorio")
        String paymentMethod,
        @Positive(message = "El monto debe ser mayor que cero")
        Double amount,
        List<CartItemRequest> items,
        String deliveryMethod
) {

    public record CartItemRequest(
            Long productId,
            Integer quantity
    ) {
    }
}
