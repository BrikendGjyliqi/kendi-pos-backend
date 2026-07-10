package com.kendi.pos.supplier;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gjeneron PDF me PDFBox — thjeshte, pa nevoje per Chrome.
 * Nese s'osht PDFBox ne pom.xml, do ta shtojme.
 */
public class SupplierOrderPdfGenerator {

    public static byte[] generate(SupplierOrder order, Supplier supplier) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(
                    org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            doc.addPage(page);

            var font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
            var fontBold = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;

            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {

                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float margin = 50;
                float y = pageHeight - margin;

                // HEADER — KENDI CAFE (na jemi ata qe porosisim)
                cs.setNonStrokingColor(new Color(0x1A, 0x3A, 0x5C));
                cs.beginText();
                cs.setFont(fontBold, 22);
                cs.newLineAtOffset(margin, y);
                cs.showText("KENDI CAFE");
                cs.endText();

                y -= 20;
                cs.setNonStrokingColor(new Color(0x66, 0x66, 0x66));
                cs.beginText();
                cs.setFont(font, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Rr. Nena Tereze, nr. 42, 10000 Prishtine, Kosove");
                cs.endText();

                y -= 12;
                cs.beginText();
                cs.setFont(font, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Menaxher: Brikend Gjyliqi · Tel: +383 44 123 456");
                cs.endText();

                // Titull ne djathtas
                cs.setNonStrokingColor(new Color(0x1A, 0x3A, 0x5C));
                cs.beginText();
                cs.setFont(fontBold, 18);
                cs.newLineAtOffset(pageWidth - margin - 130, pageHeight - margin);
                cs.showText("POROSI");
                cs.endText();

                cs.setNonStrokingColor(new Color(0x44, 0x44, 0x44));
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(pageWidth - margin - 130, pageHeight - margin - 20);
                cs.showText("Nr: " + order.getOrderRef());
                cs.endText();

                String dateStr = Instant.ofEpochMilli(order.getCreatedAt())
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("sq")));

                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(pageWidth - margin - 130, pageHeight - margin - 34);
                cs.showText("Data: " + dateStr);
                cs.endText();

                // Separator line
                y -= 20;
                cs.setStrokingColor(new Color(0x1A, 0x3A, 0x5C));
                cs.setLineWidth(1.5f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();

                y -= 25;

                // FURNITORI (kush merr porosine)
                cs.setNonStrokingColor(new Color(0x88, 0x88, 0x88));
                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("POROSI PER:");
                cs.endText();

                y -= 16;
                cs.setNonStrokingColor(new Color(0x1A, 0x3A, 0x5C));
                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.newLineAtOffset(margin, y);
                cs.showText(supplier != null ? supplier.getName() : order.getSupplierName());
                cs.endText();

                if (supplier != null) {
                    if (supplier.getContactPerson() != null && !supplier.getContactPerson().isEmpty()) {
                        y -= 14;
                        cs.setNonStrokingColor(new Color(0x44, 0x44, 0x44));
                        cs.beginText();
                        cs.setFont(font, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Personi kontakt: " + supplier.getContactPerson());
                        cs.endText();
                    }
                    if (supplier.getPhone() != null && !supplier.getPhone().isEmpty()) {
                        y -= 12;
                        cs.beginText();
                        cs.setFont(font, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Telefon: " + supplier.getPhone());
                        cs.endText();
                    }
                    if (supplier.getEmail() != null && !supplier.getEmail().isEmpty()) {
                        y -= 12;
                        cs.beginText();
                        cs.setFont(font, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Email: " + supplier.getEmail());
                        cs.endText();
                    }
                    if (supplier.getAddress() != null && !supplier.getAddress().isEmpty()) {
                        y -= 12;
                        cs.beginText();
                        cs.setFont(font, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Adresa: " + supplier.getAddress());
                        cs.endText();
                    }
                }

                y -= 30;

                // ITEMS TABLE HEADER
                float tableY = y;
                float col1 = margin;            // Nr
                float col2 = margin + 35;       // Emri
                float col3 = pageWidth - margin - 200; // Sasia
                float col4 = pageWidth - margin - 100; // Njesia
                float rowHeight = 24;

                // Header background
                cs.setNonStrokingColor(new Color(0x1A, 0x3A, 0x5C));
                cs.addRect(margin, y - rowHeight + 5, pageWidth - 2 * margin, rowHeight);
                cs.fill();

                cs.setNonStrokingColor(Color.WHITE);
                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(col1 + 5, y - 12);
                cs.showText("Nr.");
                cs.endText();

                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(col2, y - 12);
                cs.showText("Produkti");
                cs.endText();

                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(col3, y - 12);
                cs.showText("Sasia");
                cs.endText();

                cs.beginText();
                cs.setFont(fontBold, 9);
                cs.newLineAtOffset(col4, y - 12);
                cs.showText("Njesia");
                cs.endText();

                y -= rowHeight;

                // ITEMS ROWS
                int idx = 1;
                for (SupplierOrderItem item : order.getItems()) {
                    // Alternate row background
                    if (idx % 2 == 0) {
                        cs.setNonStrokingColor(new Color(0xF8, 0xFA, 0xFC));
                        cs.addRect(margin, y - rowHeight + 5, pageWidth - 2 * margin, rowHeight);
                        cs.fill();
                    }

                    cs.setNonStrokingColor(new Color(0x22, 0x22, 0x22));

                    cs.beginText();
                    cs.setFont(font, 10);
                    cs.newLineAtOffset(col1 + 5, y - 12);
                    cs.showText(String.valueOf(idx));
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 10);
                    cs.newLineAtOffset(col2, y - 12);
                    cs.showText(safeText(item.getProductName()));
                    cs.endText();

                    cs.beginText();
                    cs.setFont(fontBold, 10);
                    cs.newLineAtOffset(col3, y - 12);
                    String qtyStr = item.getStockUnit() != null && item.getStockUnit().equals("KG")
                            ? String.format(Locale.US, "%.2f", item.getQuantity())
                            : String.valueOf((int) item.getQuantity());
                    cs.showText(qtyStr);
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 10);
                    cs.newLineAtOffset(col4, y - 12);
                    cs.showText(item.getStockUnit() != null && item.getStockUnit().equals("KG")
                            ? "kg" : "cope");
                    cs.endText();

                    y -= rowHeight;
                    idx++;
                }

                // Border rreth tabeles
                cs.setStrokingColor(new Color(0xD0, 0xD7, 0xDE));
                cs.setLineWidth(0.5f);
                cs.addRect(margin, y + 5, pageWidth - 2 * margin, tableY - y);
                cs.stroke();

                y -= 20;

                // Notes
                if (order.getNotes() != null && !order.getNotes().isEmpty()) {
                    cs.setNonStrokingColor(new Color(0x88, 0x88, 0x88));
                    cs.beginText();
                    cs.setFont(fontBold, 9);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("SHENIME:");
                    cs.endText();

                    y -= 14;
                    cs.setNonStrokingColor(new Color(0x44, 0x44, 0x44));
                    cs.beginText();
                    cs.setFont(font, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(safeText(order.getNotes()));
                    cs.endText();
                    y -= 20;
                }

                // Signature at bottom
                float sigY = 100;
                cs.setNonStrokingColor(new Color(0x44, 0x44, 0x44));
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(pageWidth - margin - 180, sigY + 20);
                cs.showText("_______________________");
                cs.endText();

                cs.setNonStrokingColor(new Color(0x88, 0x88, 0x88));
                cs.beginText();
                cs.setFont(font, 9);
                cs.newLineAtOffset(pageWidth - margin - 160, sigY + 5);
                cs.showText("Menaxheri, Kendi Cafe");
                cs.endText();

                // Footer
                cs.setNonStrokingColor(new Color(0x99, 0x99, 0x99));
                cs.beginText();
                cs.setFont(font, 7);
                cs.newLineAtOffset(margin, 30);
                cs.showText("Ky dokument osht gjeneruar automatikisht nga sistemi Kendi POS · Porosia " + order.getOrderRef());
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // Convert Albanian chars to safe Latin for PDF (PDFBox Helvetica s'e mbeshtetesh utf-8 sh mire)
    private static String safeText(String s) {
        if (s == null) return "";
        return s.replace("ë", "e").replace("Ë", "E")
                .replace("ç", "c").replace("Ç", "C")
                .replace("ë", "e").replace("Ë", "E");
    }
}