package co.edu.sena.productsreact.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "reserved_by")
    private String reservedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private PaymentRecord payment;

    public Reservation() {
    }

    public Reservation(Product product, Integer quantity, LocalDateTime createdAt) {
        this.product = product;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    public Reservation(Product product, Integer quantity, LocalDateTime createdAt, String sessionId, String reservedBy) {
        this.product = product;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.sessionId = sessionId;
        this.reservedBy = reservedBy;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getReservedBy() {
        return reservedBy;
    }

    public void setReservedBy(String reservedBy) {
        this.reservedBy = reservedBy;
    }

    public PaymentRecord getPayment() {
        return payment;
    }

    public void setPayment(PaymentRecord payment) {
        this.payment = payment;
    }
}
