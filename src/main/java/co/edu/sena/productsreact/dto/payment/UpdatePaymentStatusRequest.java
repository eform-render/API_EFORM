package co.edu.sena.productsreact.dto.payment;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdatePaymentStatusRequest(
        @NotBlank(message = "El estado es obligatorio")
        String status,
        String observation,
        LocalDate estimatedDeliveryDate,
        String estimatedDeliveryTime
) {
}
