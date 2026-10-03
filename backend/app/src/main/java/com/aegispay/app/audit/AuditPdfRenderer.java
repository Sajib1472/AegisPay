package com.aegispay.app.audit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic PDF-1.4 writer. No clocks, no random IDs — regenerating a locked
 * run must hash to the same SHA-256.
 */
public final class AuditPdfRenderer {

    private static final int PAGE_WIDTH = 612;
    private static final int PAGE_HEIGHT = 792;
    private static final int LINES_PER_PAGE = 56;

    private AuditPdfRenderer() {
    }

    public static byte[] render(List<String> lines) {
        List<String> safe = lines == null || lines.isEmpty() ? List.of("(empty audit pack)") : lines;
        List<List<String>> pages = paginate(safe);
        List<byte[]> objects = new ArrayList<>();
        objects.add(obj(1, "<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder("[");
        int pageObj = 3;
        int contentObj = 3 + pages.size();
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) {
                kids.append(" ");
            }
            kids.append(pageObj + i).append(" 0 R");
        }
        kids.append("]");
        objects.add(obj(2, "<< /Type /Pages /Kids " + kids + " /Count " + pages.size() + " >>"));
        for (int i = 0; i < pages.size(); i++) {
            objects.add(obj(pageObj + i,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "] "
                            + "/Contents " + (contentObj + i) + " 0 R /Resources << /Font << /F1 "
                            + (contentObj + pages.size()) + " 0 R >> >> >>"));
        }
        for (int i = 0; i < pages.size(); i++) {
            byte[] stream = contentStream(pages.get(i));
            objects.add(streamObj(contentObj + i, stream));
        }
        objects.add(obj(contentObj + pages.size(), "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>"));
        return assemble(objects);
    }

    private static List<List<String>> paginate(List<String> lines) {
        List<List<String>> pages = new ArrayList<>();
        List<String> page = new ArrayList<>();
        for (String raw : lines) {
            for (String wrapped : wrap(raw, 86)) {
                if (page.size() >= LINES_PER_PAGE) {
                    pages.add(page);
                    page = new ArrayList<>();
                }
                page.add(wrapped);
            }
        }
        if (!page.isEmpty()) {
            pages.add(page);
        }
        return pages;
    }

    static List<String> wrap(String line, int width) {
        String value = line == null ? "" : line.replace("\t", "  ");
        if (value.length() <= width) {
            return List.of(value);
        }
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < value.length()) {
            out.add(value.substring(i, Math.min(value.length(), i + width)));
            i += width;
        }
        return out;
    }

    private static byte[] contentStream(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        sb.append("BT\n/F1 10 Tf\n72 740 Td\n12 TL\n");
        for (String line : lines) {
            sb.append("(").append(escape(line)).append(") '\n");
        }
        sb.append("ET\n");
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static byte[] obj(int id, String body) {
        return (id + " 0 obj\n" + body + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] streamObj(int id, byte[] stream) {
        byte[] header = (id + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1);
        byte[] tail = "\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1);
        byte[] out = new byte[header.length + stream.length + tail.length];
        System.arraycopy(header, 0, out, 0, header.length);
        System.arraycopy(stream, 0, out, header.length, stream.length);
        System.arraycopy(tail, 0, out, header.length + stream.length, tail.length);
        return out;
    }

    private static byte[] assemble(List<byte[]> objects) {
        byte[] header = "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n".getBytes(StandardCharsets.ISO_8859_1);
        int[] offsets = new int[objects.size() + 1];
        int pos = header.length;
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = pos;
            pos += objects.get(i).length;
        }
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(objects.size() + 1).append("\n");
        xref.append("0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            xref.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        xref.append("trailer << /Size ").append(objects.size() + 1)
                .append(" /Root 1 0 R /Info << /Producer (AegisPay Audit Pack) /Creator (AegisPay) >> >>\n")
                .append("startxref\n").append(pos).append("\n%%EOF\n");
        byte[] xrefBytes = xref.toString().getBytes(StandardCharsets.ISO_8859_1);
        byte[] out = new byte[pos + xrefBytes.length];
        System.arraycopy(header, 0, out, 0, header.length);
        int cursor = header.length;
        for (byte[] object : objects) {
            System.arraycopy(object, 0, out, cursor, object.length);
            cursor += object.length;
        }
        System.arraycopy(xrefBytes, 0, out, cursor, xrefBytes.length);
        return out;
    }
}
