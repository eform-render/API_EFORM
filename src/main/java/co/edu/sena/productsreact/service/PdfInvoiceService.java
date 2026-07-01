package co.edu.sena.productsreact.service;

import co.edu.sena.productsreact.entity.PaymentRecord;
import co.edu.sena.productsreact.entity.Reservation;
import co.edu.sena.productsreact.repository.ReservationRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfInvoiceService {

    @Autowired
    private ReservationRepository reservationRepository;

    private static final String COMPANY_NAME = "E-FORM UNIFORMES SENA SAS";
    private static final String COMPANY_NIT = "900123456-7";
    private static final String COMPANY_EMAIL = "eform.uniformes@gmail.com";
    private static final String COMPANY_PHONE = "300 451 9922";
    private static final String COMPANY_ADDRESS = "SENA – Centro de Formación de la Industria de la Construcción – Ternera, Cartagena de Indias, Bolívar";
    private static final String COMPANY_TAX_REGIME = "Responsable de IVA";
    private static final Double IVA_RATE = 0.19;
    private static final Double SHIPPING_COST = 5500.0;

    public byte[] generateInvoicePdf(PaymentRecord payment) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(20, 20, 20, 20);

        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // Encabezado con logo y datos de la empresa
        addHeader(document, payment, boldFont, regularFont);

        // Número de factura y fechas
        addInvoiceInfo(document, payment, boldFont, regularFont);

        // Datos del emisor y cliente
        addEmitterAndClientData(document, payment, boldFont, regularFont);

        // Tabla de items
        addItemsTable(document, payment, boldFont, regularFont);

        // Totales
        addTotals(document, payment, boldFont, regularFont);

        // Datos para pago
        addPaymentInstructions(document, boldFont, regularFont);

        // Pie de página
        addFooter(document, regularFont);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, PaymentRecord payment, PdfFont boldFont, PdfFont regularFont) throws IOException {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{20, 40, 40}));
        headerTable.setWidth(UnitValue.createPercentValue(100));

        // Logo placeholder (E-FORM)
        Cell logoCell = new Cell()
                .add(new Paragraph("E-FORM").setFont(boldFont).setFontSize(24).setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER);
        headerTable.addCell(logoCell);

        // Nombre empresa
        Cell companyCell = new Cell()
                .add(new Paragraph(COMPANY_NAME).setFont(boldFont).setFontSize(12))
                .add(new Paragraph("Calidad que nos identifica").setFont(regularFont).setFontSize(10))
                .setBorder(Border.NO_BORDER);
        headerTable.addCell(companyCell);

        // Invoice info box
        Cell invoiceCell = new Cell()
                .add(new Paragraph("FACTURA DE VENTA").setFont(boldFont).setFontSize(12).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("No. FV-" + String.format("%04d-%04d", payment.getId() / 10000 + 2026, payment.getId() % 10000))
                        .setFont(regularFont).setFontSize(11).setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER);
        headerTable.addCell(invoiceCell);

        document.add(headerTable);
        document.add(new Paragraph(""));
    }

    private void addInvoiceInfo(Document document, PaymentRecord payment, PdfFont boldFont, PdfFont regularFont) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String emissionDate = payment.getCreatedAt().format(dateFormatter);
        String expirationDate = payment.getCreatedAt().plusDays(30).format(dateFormatter);

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        infoTable.setWidth(UnitValue.createPercentValue(100));

        infoTable.addCell(new Cell().add(new Paragraph("FECHA DE EMISIÓN:").setFont(boldFont).setFontSize(10))
                .add(new Paragraph(emissionDate).setFont(regularFont).setFontSize(10))
                .setBorder(Border.NO_BORDER));

        infoTable.addCell(new Cell().add(new Paragraph("FECHA DE VENCIMIENTO:").setFont(boldFont).setFontSize(10))
                .add(new Paragraph(expirationDate).setFont(regularFont).setFontSize(10))
                .setBorder(Border.NO_BORDER));

        document.add(infoTable);
        document.add(new Paragraph(""));
    }

    private void addEmitterAndClientData(Document document, PaymentRecord payment, PdfFont boldFont, PdfFont regularFont) {
        Table dataTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        dataTable.setWidth(UnitValue.createPercentValue(100));

        // Datos del emisor
        Cell emitterCell = new Cell()
                .add(new Paragraph("DATOS DEL EMISOR").setFont(boldFont).setFontSize(11).setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71)))
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71))
                .setBorder(Border.NO_BORDER)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);
        dataTable.addCell(emitterCell);

        // Datos del cliente
        Cell clientCell = new Cell()
                .add(new Paragraph("DATOS DEL CLIENTE").setFont(boldFont).setFontSize(11).setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71)))
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71))
                .setBorder(Border.NO_BORDER)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);
        dataTable.addCell(clientCell);

        // Detalles del emisor
        Cell emitterDetailsCell = new Cell()
                .add(new Paragraph("Razón Social: " + COMPANY_NAME).setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Email: " + COMPANY_EMAIL).setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Teléfono: " + COMPANY_PHONE).setFont(regularFont).setFontSize(9))
                .add(new Paragraph("NIT: " + COMPANY_NIT).setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Régimen: " + COMPANY_TAX_REGIME).setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Dirección: " + COMPANY_ADDRESS).setFont(regularFont).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setPadding(10);
        dataTable.addCell(emitterDetailsCell);

        // Detalles del cliente
        Cell clientDetailsCell = new Cell()
                .add(new Paragraph("Nombre: " + payment.getCustomerName()).setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Email: " + payment.getCustomerEmail()).setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Método de Pago: " + payment.getPaymentMethod()).setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Método de Entrega: " + formatDeliveryMethod(payment.getDeliveryMethod())).setFont(regularFont).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setPadding(10);
        dataTable.addCell(clientDetailsCell);

        document.add(dataTable);
        document.add(new Paragraph(""));
    }

    private void addItemsTable(Document document, PaymentRecord payment, PdfFont boldFont, PdfFont regularFont) {
        List<Reservation> items = reservationRepository.findByPaymentId(payment.getId());

        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{10, 40, 10, 15, 15, 15}));
        itemsTable.setWidth(UnitValue.createPercentValue(100));

        // Encabezados
        String[] headers = {"ITEM", "DESCRIPCIÓN", "CANT.", "PRECIO UNITARIO", "IVA", "SUBTOTAL"};
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header).setFont(boldFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71))
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);
            itemsTable.addCell(headerCell);
        }

        // Items
        DecimalFormat df = new DecimalFormat("$#,##0");
        double subtotal = 0;
        int itemNum = 1;

        for (Reservation item : items) {
            double unitPrice = item.getProduct().getPrecio().doubleValue();
            double itemTotal = unitPrice * item.getQuantity();
            double itemIva = itemTotal * IVA_RATE;
            subtotal += itemTotal;

            itemsTable.addCell(new Cell().add(new Paragraph(String.valueOf(itemNum)).setFont(regularFont).setFontSize(9)));
            itemsTable.addCell(new Cell().add(new Paragraph(item.getProduct().getNombre()).setFont(regularFont).setFontSize(9)));
            itemsTable.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity())).setFont(regularFont).setFontSize(9).setTextAlignment(TextAlignment.CENTER)));
            itemsTable.addCell(new Cell().add(new Paragraph(df.format(unitPrice)).setFont(regularFont).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
            itemsTable.addCell(new Cell().add(new Paragraph(df.format(itemIva)).setFont(regularFont).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
            itemsTable.addCell(new Cell().add(new Paragraph(df.format(itemTotal)).setFont(regularFont).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));

            itemNum++;
        }

        document.add(itemsTable);
        document.add(new Paragraph(""));
    }

    private void addTotals(Document document, PaymentRecord payment, PdfFont boldFont, PdfFont regularFont) {
        List<Reservation> items = reservationRepository.findByPaymentId(payment.getId());

        DecimalFormat df = new DecimalFormat("$#,##0");
        double subtotal = 0;

        for (Reservation item : items) {
            subtotal += item.getProduct().getPrecio().doubleValue() * item.getQuantity();
        }

        double shippingCost = payment.getShippingCost() != null ? payment.getShippingCost() : 0.0;
        double subtotalWithShipping = subtotal + shippingCost;
        double iva = subtotalWithShipping * IVA_RATE;
        double total = subtotalWithShipping + iva;

        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
        totalsTable.setWidth(UnitValue.createPercentValue(100));
        totalsTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);

        totalsTable.addCell(new Cell().add(new Paragraph("SUBTOTAL").setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
        totalsTable.addCell(new Cell().add(new Paragraph(df.format(subtotal)).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));

        if (shippingCost > 0) {
            totalsTable.addCell(new Cell().add(new Paragraph("DOMICILIO/ENVÍO").setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            totalsTable.addCell(new Cell().add(new Paragraph(df.format(shippingCost)).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
        }

        totalsTable.addCell(new Cell().add(new Paragraph("SUBTOTAL GRAVADO").setFont(boldFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
        totalsTable.addCell(new Cell().add(new Paragraph(df.format(subtotalWithShipping)).setFont(boldFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));

        totalsTable.addCell(new Cell().add(new Paragraph("IVA (19%)").setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
        totalsTable.addCell(new Cell().add(new Paragraph(df.format(iva)).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));

        totalsTable.addCell(new Cell().add(new Paragraph("TOTAL A PAGAR").setFont(boldFont).setFontSize(12).setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71)))
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71))
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE));
        totalsTable.addCell(new Cell().add(new Paragraph(df.format(total)).setFont(boldFont).setFontSize(12).setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71)))
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71))
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE));

        document.add(totalsTable);
        document.add(new Paragraph(""));
    }

    private void addPaymentInstructions(Document document, PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("DATOS PARA PAGO").setFont(boldFont).setFontSize(11).setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(25, 48, 71))
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE));

        Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}));
        paymentTable.setWidth(UnitValue.createPercentValue(100));

        paymentTable.addCell(new Cell()
                .add(new Paragraph("CUENTA BANCARIA").setFont(boldFont).setFontSize(9))
                .add(new Paragraph("Banco: Bancolombia").setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Tipo: Cuenta de Ahorro").setFont(regularFont).setFontSize(9))
                .add(new Paragraph("Número: 08687139152").setFont(regularFont).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setPadding(5));

        paymentTable.addCell(new Cell()
                .add(new Paragraph("NEQUI").setFont(boldFont).setFontSize(9))
                .add(new Paragraph("Número: 3105388662").setFont(regularFont).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setPadding(5));

        paymentTable.addCell(new Cell()
                .add(new Paragraph("BRE-BE").setFont(boldFont).setFontSize(9))
                .add(new Paragraph("Alias: @turizo052").setFont(regularFont).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setPadding(5));

        document.add(paymentTable);
        document.add(new Paragraph("Por favor, envía el comprobante de pago al correo: eform.uniformes@gmail.com").setFont(regularFont).setFontSize(9).setItalic());
        document.add(new Paragraph(""));
    }

    private void addFooter(Document document, PdfFont regularFont) {
        document.add(new Paragraph("Gracias por tu compra en E-FORM").setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("SENA – Centro de Formación de la Industria de la Construcción – Ternera | Cartagena de Indias, Bolívar")
                .setFont(regularFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Teléfono: 300 451 9922 | Email: eform.uniformes@gmail.com")
                .setFont(regularFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
    }

    private String formatDeliveryMethod(String method) {
        if ("recoge".equalsIgnoreCase(method)) {
            return "Cliente Recoge";
        }
        return "Envío a Domicilio";
    }
}
