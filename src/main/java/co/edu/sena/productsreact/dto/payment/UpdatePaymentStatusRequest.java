package co.edu.sena.productsreact.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record UpdatePaymentStatusRequest(
        @NotBlank(message = "El estado es obligatorio")
        String status,
        String observation
) {
}
