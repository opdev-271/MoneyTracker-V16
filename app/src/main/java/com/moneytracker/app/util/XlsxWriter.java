package com.moneytracker.app.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Hand-rolled, single-sheet .xlsx writer using only java.util.zip — no POI or other
 * spreadsheet library dependency. Produces the minimal set of OOXML parts a
 * SpreadsheetML package needs: content types, root relationships, the workbook, its
 * relationships, a styles part (plain + bold), and one worksheet using inline strings
 * (no shared-strings table needed at this size).
 *
 * Each row is a {@code List<Object>} of cells; a cell is a {@link String} (written as
 * an inline string) or a {@link Number} (written as a numeric cell). {@code null}
 * cells are skipped (left blank).
 */
public class XlsxWriter {

    private static final String CONTENT_TYPES_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "</Types>";

    private static final String ROOT_RELS_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>";

    private static final String WORKBOOK_RELS_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                    "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                    "</Relationships>";

    private static final String STYLES_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                    "<fonts count=\"2\">" +
                    "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
                    "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
                    "</fonts>" +
                    "<fills count=\"2\">" +
                    "<fill><patternFill patternType=\"none\"/></fill>" +
                    "<fill><patternFill patternType=\"gray125\"/></fill>" +
                    "</fills>" +
                    "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                    "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                    "<cellXfs count=\"2\">" +
                    "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
                    "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
                    "</cellXfs>" +
                    "</styleSheet>";

    private static final int STYLE_BOLD = 1;

    private XlsxWriter() {
    }

    /** One row of cells; a cell is a String, a Number, or null (blank). Pass {@code bold} for header-style rows. */
    public static class Row {
        public final List<Object> cells;
        public final boolean bold;

        public Row(List<Object> cells, boolean bold) {
            this.cells = cells;
            this.bold = bold;
        }
    }

    public static void write(File outFile, String sheetName, List<Row> rows) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            write(fos, sheetName, rows);
        }
    }

    public static void write(OutputStream out, String sheetName, List<Row> rows) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML);
        writeEntry(zip, "_rels/.rels", ROOT_RELS_XML);
        writeEntry(zip, "xl/workbook.xml", workbookXml(sheetName));
        writeEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML);
        writeEntry(zip, "xl/styles.xml", STYLES_XML);
        writeEntry(zip, "xl/worksheets/sheet1.xml", sheetXml(rows));
        zip.close();
    }

    private static String workbookXml(String sheetName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"" + escapeXml(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
                "</workbook>";
    }

    private static String sheetXml(List<Row> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<cols><col min=\"1\" max=\"12\" width=\"18\" customWidth=\"1\"/></cols>");
        sb.append("<sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = rows.get(rowIndex);
            int rowNumber = rowIndex + 1;
            sb.append("<row r=\"").append(rowNumber).append("\">");
            for (int colIndex = 0; colIndex < row.cells.size(); colIndex++) {
                Object cell = row.cells.get(colIndex);
                if (cell == null) {
                    continue;
                }
                String ref = columnLetters(colIndex) + rowNumber;
                String styleAttr = row.bold ? " s=\"" + STYLE_BOLD + "\"" : "";
                if (cell instanceof Number) {
                    String numberText = String.format(java.util.Locale.US, "%.2f", ((Number) cell).doubleValue());
                    sb.append("<c r=\"").append(ref).append("\"").append(styleAttr).append(">")
                            .append("<v>").append(numberText).append("</v></c>");
                } else {
                    sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"").append(styleAttr).append(">")
                            .append("<is><t xml:space=\"preserve\">").append(escapeXml(String.valueOf(cell))).append("</t></is></c>");
                }
            }
            sb.append("</row>");
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    /** 0-based column index to Excel column letters (0 -> A, 25 -> Z, 26 -> AA, ...). */
    private static String columnLetters(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (true) {
            int remainder = n % 26;
            sb.insert(0, (char) ('A' + remainder));
            n = n / 26 - 1;
            if (n < 0) {
                break;
            }
        }
        return sb.toString();
    }

    private static String escapeXml(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    // Strip control characters invalid in XML 1.0 (keep tab/newline/CR).
                    if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                        break;
                    }
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private static void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
