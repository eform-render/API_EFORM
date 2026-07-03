package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.dto.product.ProductRequest;
import co.edu.sena.productsreact.dto.product.ProductResponse;
import co.edu.sena.productsreact.entity.Product;
import co.edu.sena.productsreact.exception.ResourceNotFoundException;
import co.edu.sena.productsreact.repository.ProductRepository;
import co.edu.sena.productsreact.repository.ReservationRepository;
import co.edu.sena.productsreact.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    /**
     * Obtener lista de productos activos
     */
    public List<ProductResponse> findAll() {
        return productRepository.findAllByIsDeletedFalse()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Buscar producto por ID
     */
    public ProductResponse findById(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto con id " + id + " no encontrado"));

        return toResponse(product);
    }

    /**
     * Crear un nuevo producto
     */
    @Transactional
    public ProductResponse create(ProductRequest request) {

        Product product = Product.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .tipoTela(request.tipoTela())
                .imageUrl(request.imageUrl())
                .tallasDisponibles(joinTallas(request.tallasDisponibles()))
                .precio(request.precio())
                .stock(request.stock())
                .isDeleted(false)
                .build();

        Product saved = productRepository.save(product);

        return toResponse(saved);
    }

    /**
     * Actualizar producto existente
     */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto con id " + id + " no encontrado"));

        product.setNombre(request.nombre());
        product.setDescripcion(request.descripcion());
        product.setTipoTela(request.tipoTela());
        product.setImageUrl(request.imageUrl());
        product.setTallasDisponibles(joinTallas(request.tallasDisponibles()));
        product.setPrecio(request.precio());
        product.setStock(request.stock());

        Product updated = productRepository.save(product);

        return toResponse(updated);
    }

    private String joinTallas(List<String> tallas) {
        if (tallas == null || tallas.isEmpty()) {
            return null;
        }

        return tallas.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private List<String> parseTallas(String tallas) {
        if (tallas == null || tallas.isBlank()) {
            return List.of();
        }

        return List.of(tallas.split("\\s*,\\s*"));
    }

    /**
     * Eliminar producto lógicamente
     */
    @Transactional
    public void delete(Long id) {

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto con id " + id + " no encontrado"));

        product.setIsDeleted(true);

        productRepository.save(product);
    }

    @Transactional
    public void reserveStock(Long id, int quantity, String sessionId, String reservedBy) {
        if (quantity <= 0) throw new IllegalArgumentException("La cantidad a reservar debe ser mayor que cero");

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto con id " + id + " no encontrado"));

        int current = product.getStock() == null ? 0 : product.getStock();
        if (current < quantity) {
            throw new IllegalArgumentException("Stock insuficiente para reservar. Disponible: " + current);
        }

        // create reservation and decrement stock
        Reservation r = new Reservation(product, quantity, co.edu.sena.productsreact.util.AppClock.nowBogota(), sessionId, reservedBy);
        reservationRepository.save(r);

        int newStock = current - quantity;
        product.setStock(newStock);
        productRepository.save(product);

        // Notificar a los admins solo al cruzar el umbral de stock bajo (evita spam en cada reserva)
        if (current > LOW_STOCK_THRESHOLD && newStock <= LOW_STOCK_THRESHOLD) {
            notificationService.notifyAdmins(
                    "STOCK_BAJO",
                    "Stock bajo",
                    "El producto \"" + product.getNombre() + "\" quedo con " + newStock + " unidades disponibles",
                    product.getId()
            );
        }
    }

    @Transactional
    public void releaseStock(Long id, int quantity, String sessionId) {
        if (quantity <= 0) throw new IllegalArgumentException("La cantidad a liberar debe ser mayor que cero");

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto con id " + id + " no encontrado"));

        int remaining = quantity;
        if (sessionId != null && !sessionId.isBlank()) {
            // release reservations matching the sessionId first
            var reservations = reservationRepository.findByProductOrderByCreatedAtAsc(product);
            for (Reservation r : reservations) {
                if (remaining <= 0) break;
                if (sessionId.equals(r.getSessionId())) {
                    int removeQty = Math.min(remaining, r.getQuantity());
                    r.setQuantity(r.getQuantity() - removeQty);
                    remaining -= removeQty;
                    if (r.getQuantity() <= 0) {
                        reservationRepository.delete(r);
                    } else {
                        reservationRepository.save(r);
                    }
                }
            }
        }

        if (remaining > 0) {
            // remove oldest reservations for this product until we've released remaining
            var reservations = reservationRepository.findByProductOrderByCreatedAtAsc(product);
            for (Reservation r : reservations) {
                if (remaining <= 0) break;
                int removeQty = Math.min(remaining, r.getQuantity());
                r.setQuantity(r.getQuantity() - removeQty);
                remaining -= removeQty;
                if (r.getQuantity() <= 0) {
                    reservationRepository.delete(r);
                } else {
                    reservationRepository.save(r);
                }
            }
        }

        // increase product stock by how many were actually released
        int released = quantity - remaining;
        int current = product.getStock() == null ? 0 : product.getStock();
        product.setStock(current + released);
        productRepository.save(product);
    }

    /**
     * Convertir entidad Product a ProductResponse
     */
    private ProductResponse toResponse(Product p) {

        return new ProductResponse(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getTipoTela(),
                p.getImageUrl(),
                parseTallas(p.getTallasDisponibles()),
                p.getPrecio(),
                p.getStock(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}