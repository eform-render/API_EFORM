package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.entity.PaymentRecord;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    public String generateInvoiceHtml(PaymentRecord payment) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDate = payment.getCreatedAt().format(dateFormatter);
        String currencyFormat = String.format("$%,.0f COP", payment.getAmount());

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; color: #333; }
                        .container { max-width: 800px; margin: 0 auto; padding: 20px; }
                        .header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #007bff; padding-bottom: 20px; }
                        .header h1 { color: #007bff; margin: 0; }
                        .header p { margin: 5px 0; }
                        .invoice-details { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }
                        .detail-box { background: #f5f5f5; padding: 15px; border-radius: 5px; }
                        .detail-box h3 { margin-top: 0; color: #007bff; }
                        .detail-box p { margin: 5px 0; }
                        .items-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                        .items-table th { background: #007bff; color: white; padding: 10px; text-align: left; }
                        .items-table td { padding: 10px; border-bottom: 1px solid #ddd; }
                        .items-table tr:nth-child(even) { background: #f9f9f9; }
                        .totals { text-align: right; margin-top: 20px; }
                        .totals .total-row { font-size: 18px; font-weight: bold; color: #007bff; }
                        .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>FACTURA - EFORM</h1>
                            <p>Pedido #%d</p>
                        </div>

                        <div class="invoice-details">
                            <div class="detail-box">
                                <h3>Información del Cliente</h3>
                                <p><strong>Nombre:</strong> %s</p>
                                <p><strong>Email:</strong> %s</p>
                            </div>
                            <div class="detail-box">
                                <h3>Información del Pedido</h3>
                                <p><strong>Fecha:</strong> %s</p>
                                <p><strong>Estado:</strong> %s</p>
                            </div>
                        </div>

                        <table class="items-table">
                            <thead>
                                <tr>
                                    <th>Concepto</th>
                                    <th>Método de Pago</th>
                                    <th>Valor</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>Compra en EFORM</td>
                                    <td>%s</td>
                                    <td style="text-align: right;">%s</td>
                                </tr>
                            </tbody>
                        </table>

                        <div class="totals">
                            <div class="total-row">TOTAL: %s</div>
                        </div>

                        <div class="footer">
                            <p>Gracias por tu compra en EFORM.</p>
                            <p>Esta factura fue generada automáticamente y es válida sin firma.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                payment.getId(),
                payment.getCustomerName(),
                payment.getCustomerEmail(),
                formattedDate,
                payment.getStatus(),
                payment.getPaymentMethod(),
                currencyFormat,
                currencyFormat
        );
    }
}
