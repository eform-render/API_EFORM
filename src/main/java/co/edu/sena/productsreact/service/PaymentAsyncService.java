package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.entity.PaymentRecord;
import co.edu.sena.productsreact.event.ComprobanteUploadRequestedEvent;
import co.edu.sena.productsreact.repository.PaymentRecordRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

/**
 * Sube el comprobante de pago a Cloudinary y lo registra en la pasarela externa
 * en segundo plano, despues de confirmada la transaccion del checkout, para que
 * el cliente no tenga que esperar estas llamadas HTTP externas para ver la
 * respuesta de su pago.
 */
@Service
@RequiredArgsConstructor
public class PaymentAsyncService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAsyncService.class);

    private final PaymentRecordRepository paymentRecordRepository;
    private final PasarelaGatewayService pasarelaGatewayService;
    private final Cloudinary cloudinary;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onComprobanteUploadRequested(ComprobanteUploadRequestedEvent event) {
        PaymentRecord saved = paymentRecordRepository.findById(event.paymentId()).orElse(null);
        if (saved == null) {
            log.warn("No se encontro el pago id={} para procesar el comprobante en segundo plano", event.paymentId());
            return;
        }

        // Guardamos una copia propia y persistente del comprobante en Cloudinary,
        // independiente de la pasarela externa, para que el admin siempre pueda verlo.
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(event.fileBytes(), ObjectUtils.asMap(
                    "public_id", "recibos/recibo_" + event.paymentId() + "_" + UUID.randomUUID(),
                    "overwrite", true,
                    "resource_type", "image"
            ));
            saved.setReceiptImageUrl((String) uploadResult.get("secure_url"));
        } catch (Exception e) {
            log.error("Error guardando comprobante en Cloudinary para orden={}: {}", event.paymentId(), e.getMessage(), e);
        }

        String externalId = pasarelaGatewayService.crearPago(
                event.paymentId(),
                event.paymentMethod(),
                event.amount(),
                event.customerEmail(),
                event.fileBytes(),
                event.contentType(),
                event.filename()
        );
        if (externalId != null) {
            saved.setExternalPaymentId(externalId);
        }

        paymentRecordRepository.save(saved);
    }
}
