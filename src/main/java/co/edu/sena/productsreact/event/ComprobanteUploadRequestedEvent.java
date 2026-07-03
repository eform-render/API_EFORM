package co.edu.sena.productsreact.event;

public record ComprobanteUploadRequestedEvent(
        Long paymentId,
        String paymentMethod,
        Double amount,
        String customerEmail,
        byte[] fileBytes,
        String contentType,
        String filename
) {
}
