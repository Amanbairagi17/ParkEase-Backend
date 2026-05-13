package com.parkease.receipt_service.utils;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.parkease.receipt_service.dtos.UserResponseDto;
import com.parkease.receipt_service.entity.Receipt;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Component
public class PdfGenerator {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateReceiptPdf(Receipt receipt, UserResponseDto user) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("ParkEase Payment Receipt", titleFont));
            document.add(new Paragraph("Receipt Number: " + receipt.getReceiptNumber(), labelFont));
            document.add(new Paragraph("Generated At: " + formatDate(receipt.getGeneratedAt()), valueFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addRow(table, "User Name", user != null ? user.getFullName() : "N/A", labelFont, valueFont);
            addRow(table, "User Email", user != null ? user.getEmail() : "N/A", labelFont, valueFont);
            addRow(table, "Vehicle Number", receipt.getVehicleNumber(), labelFont, valueFont);
            addRow(table, "Parking Name", receipt.getParkingName(), labelFont, valueFont);
            addRow(table, "Slot Number", receipt.getSlotNumber(), labelFont, valueFont);
            addRow(table, "Check-In Time", formatDate(receipt.getCheckInTime()), labelFont, valueFont);
            addRow(table, "Check-Out Time", formatDate(receipt.getCheckOutTime()), labelFont, valueFont);
            addRow(table, "Duration", receipt.getDuration(), labelFont, valueFont);
            addRow(table, "Amount Paid", receipt.getAmountPaid() != null ? receipt.getAmountPaid().toPlainString() : "0.00", labelFont, valueFont);
            addRow(table, "Payment Status", receipt.getPaymentStatus(), labelFont, valueFont);
            addRow(table, "Payment Method", receipt.getPaymentMethod(), labelFont, valueFont);
            addRow(table, "Transaction ID", receipt.getTransactionId(), labelFont, valueFont);
            addRow(table, "Razorpay Order ID", receipt.getRazorpayOrderId(), labelFont, valueFont);
            addRow(table, "Razorpay Payment ID", receipt.getRazorpayPaymentId(), labelFont, valueFont);

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (DocumentException ex) {
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorderWidth(0.5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value == null ? "" : value, valueFont));
        valueCell.setBorderWidth(0.5f);
        table.addCell(valueCell);
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_TIME);
    }
}
