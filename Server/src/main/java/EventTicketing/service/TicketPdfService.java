package EventTicketing.service;

import EventTicketing.model.Booking;
import EventTicketing.model.Event;
import EventTicketing.model.SeatCategory;
import EventTicketing.model.Ticket;
import EventTicketing.model.User;
import EventTicketing.model.Venue;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Renders a single downloadable PDF for a booking's ticket. Since one
 * ticket now covers every seat purchased in a booking, this PDF is the
 * single artifact a customer downloads for the whole party — it shows the
 * seat category, the quantity of seats it covers, and a QR code encoding
 * the ticket code for check-in at the venue.
 */
@Service
public class TicketPdfService {

    private static final Color BRAND_COLOR = new Color(37, 61, 122);
    private static final Color MUTED_COLOR = new Color(110, 110, 110);

    public byte[] render(Ticket ticket, Booking booking) {
        Event event = booking.getEvent();
        Venue venue = event.getVenue();
        SeatCategory seatCategory = booking.getSeatCategory();
        User holder = booking.getUser();

        try {
            Document document = new Document(PageSize.A5, 40, 40, 50, 40);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, event);
            addQrCode(document, ticket.getTicketCode());
            addTicketCode(document, ticket.getTicketCode());
            addDetailsTable(document, ticket, booking, event, venue, seatCategory, holder);
            addFooter(document);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException | WriterException e) {
            throw new IllegalStateException("Failed to generate ticket PDF", e);
        }
    }

    private void addHeader(Document document, Event event) throws DocumentException {
        Font eyebrowFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, MUTED_COLOR);
        Paragraph eyebrow = new Paragraph("EVENT TICKET", eyebrowFont);
        eyebrow.setAlignment(Element.ALIGN_CENTER);
        document.add(eyebrow);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND_COLOR);
        Paragraph title = new Paragraph(event.getTitle(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(4f);
        title.setSpacingAfter(14f);
        document.add(title);
    }

    private void addQrCode(Document document, String ticketCode) throws WriterException, IOException, DocumentException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(ticketCode, BarcodeFormat.QR_CODE, 220, 220);

        ByteArrayOutputStream qrBytes = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", qrBytes);

        Image qrImage = Image.getInstance(qrBytes.toByteArray());
        qrImage.setAlignment(Element.ALIGN_CENTER);
        qrImage.scaleToFit(150, 150);
        document.add(qrImage);
    }

    private void addTicketCode(Document document, String ticketCode) throws DocumentException {
        Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 13, Color.DARK_GRAY);
        Paragraph code = new Paragraph(ticketCode, codeFont);
        code.setAlignment(Element.ALIGN_CENTER);
        code.setSpacingBefore(8f);
        code.setSpacingAfter(18f);
        document.add(code);
    }

    private void addDetailsTable(Document document, Ticket ticket, Booking booking, Event event,
                                  Venue venue, SeatCategory seatCategory, User holder) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.1f, 1.6f});

        DateTimeFormatter dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);

        addRow(table, "Ticket Holder", nullSafe(holder != null ? holder.getName() : null));
        addRow(table, "Event Date", event.getEventDate() != null ? event.getEventDate().format(dateFmt) : "-");
        addRow(table, "Event Time", event.getEventTime() != null ? event.getEventTime().toString() : "-");
        addRow(table, "Venue", venue != null ? venue.getName() : "-");
        addRow(table, "Venue Address", venue != null ? venue.getAddress() : "-");
        addRow(table, "Seat Category", seatCategory != null ? seatCategory.getName() : "-");
        addRow(table, "Number of Seats", String.valueOf(ticket.getQuantity()));
        addRow(table, "Booking ID", booking.getId().toString());
        addRow(table, "Total Price", ticket.getTotalPrice() != null ? ticket.getTotalPrice().toPlainString() : "-");
        addRow(table, "Status", ticket.getStatus().name());
        addRow(table, "Issued On", ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : "-");

        document.add(table);
    }

    private void addRow(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, MUTED_COLOR);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        labelCell.setBorderColor(new Color(230, 230, 230));
        labelCell.setPaddingTop(6f);
        labelCell.setPaddingBottom(6f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value == null || value.isBlank() ? "-" : value, valueFont));
        valueCell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        valueCell.setBorderColor(new Color(230, 230, 230));
        valueCell.setPaddingTop(6f);
        valueCell.setPaddingBottom(6f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, MUTED_COLOR);
        Paragraph footer = new Paragraph(
                "Present this ticket (printed or on a device) and a valid ID at the venue entrance. "
                        + "The QR code above will be scanned to check in your whole party.",
                footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(16f);
        document.add(footer);
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }
}
