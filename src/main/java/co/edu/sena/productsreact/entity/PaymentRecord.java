package co.edu.sena.productsreact.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records")
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "status", length = 50)
    private String status = "PENDIENTE";

    @Column(name = "observation", length = 500)
    private String observation;

    @Column(name = "payment_reference_code", length = 50)
    private String paymentReferenceCode;

    @Column(name = "delivery_method", length = 50)
    private String deliveryMethod = "domicilio";

    @Column(name = "shipping_cost")
    private Double shippingCost = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PaymentRecord() {
    }

    public PaymentRecord(String customerName, String customerEmail, String paymentMethod, Double amount, LocalDateTime createdAt) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.createdAt = createdAt;
        this.status = "PENDIENTE";
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getPaymentReferenceCode() {
        return paymentReferenceCode;
    }

    public void setPaymentReferenceCode(String paymentReferenceCode) {
        this.paymentReferenceCode = paymentReferenceCode;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public Double getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(Double shippingCost) {
        this.shippingCost = shippingCost;
    }
}
