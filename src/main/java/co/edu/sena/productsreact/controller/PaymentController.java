package co.edu.sena.productsreact.controller;

// Controlador de pagos actualizado
import co.edu.sena.productsreact.dto.payment.PaymentRequest;
import co.edu.sena.productsreact.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Void> confirmPayment(@Valid @RequestBody PaymentRequest request) {
        paymentService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<java.util.List<co.edu.sena.productsreact.entity.PaymentRecord>> listPayments() {
        var list = paymentService.listAll();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePayments(@RequestBody java.util.List<Long> ids) {
        paymentService.deleteByIds(ids);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<co.edu.sena.productsreact.entity.PaymentRecord> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody co.edu.sena.productsreact.dto.payment.UpdatePaymentStatusRequest request) {
        var updated = paymentService.updateStatus(id, request.status(), request.observation());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/my-orders")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<co.edu.sena.productsreact.entity.PaymentRecord>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        var orders = paymentService.getOrdersByEmail(userDetails.getUsername());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}/details")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<co.edu.sena.productsreact.dto.payment.OrderDetailsResponse> getOrderDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var orderDetails = paymentService.getOrderDetails(id, userDetails.getUsername());
        return ResponseEntity.ok(orderDetails);
    }
}
