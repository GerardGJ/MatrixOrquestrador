package com.matrix.orquestrador.service;

import com.matrix.orquestrador.model.OrchestrationReport;
import com.matrix.orquestrador.model.ProcessExecution;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExecutionReportPdfWriter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int LINES_PER_PAGE = 44;

    public Path write(OrchestrationReport report, Path outputPath) {
        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputPath, buildPdf(report));
            return outputPath;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write execution report PDF", exception);
        }
    }

    private byte[] buildPdf(OrchestrationReport report) throws IOException {
        List<List<String>> pages = paginate(buildReportLines(report));
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        int objectCount = 3 + pages.size() * 2;

        write(pdf, "%PDF-1.4\n");
        offsets.add(0);
        writeObject(pdf, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        writeObject(pdf, offsets, 2, buildPagesObject(pages.size()));
        writeObject(pdf, offsets, 3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        for (int index = 0; index < pages.size(); index++) {
            int pageObjectNumber = 4 + index * 2;
            int contentObjectNumber = pageObjectNumber + 1;
            String pageObject = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R >> >> "
                    + "/Contents " + contentObjectNumber + " 0 R >>";
            writeObject(pdf, offsets, pageObjectNumber, pageObject);
            writeStreamObject(pdf, offsets, contentObjectNumber, buildContentStream(pages.get(index)));
        }

        int xrefStart = pdf.size();
        write(pdf, "xref\n");
        write(pdf, "0 " + (objectCount + 1) + "\n");
        write(pdf, "0000000000 65535 f \n");
        for (int index = 1; index <= objectCount; index++) {
            write(pdf, String.format("%010d 00000 n %n", offsets.get(index)));
        }
        write(pdf, "trailer\n");
        write(pdf, "<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\n");
        write(pdf, "startxref\n");
        write(pdf, xrefStart + "\n");
        write(pdf, "%%EOF\n");
        return pdf.toByteArray();
    }

    private List<String> buildReportLines(OrchestrationReport report) {
        List<String> lines = new ArrayList<>();
        lines.add("Execution Report");
        lines.add("");
        lines.add("Date of execution: " + DATE_FORMAT.format(report.getStartTime()));
        lines.add("Execution ID: " + report.getExecutionId());
        lines.add("");
        lines.add("Execution");
        lines.add("Started: " + DATE_TIME_FORMAT.format(report.getStartTime()));
        lines.add("Ended: " + DATE_TIME_FORMAT.format(report.getEndTime()));
        lines.add("Total duration: " + formatDuration(report.getDurationMillis()));
        lines.add("");
        lines.add("Step Details");

        for (ProcessExecution execution : report.getExecutions()) {
            lines.add("");
            lines.add(execution.getProcessName() + " (" + execution.getProcessId() + ")");
            lines.add("Status: " + execution.getStatus());
            lines.add("Started: " + formatDateTime(execution.getStartTime()));
            lines.add("Ended: " + formatDateTime(execution.getEndTime()));
            lines.add("Duration: " + formatDuration(execution.getDurationMillis()));
            lines.add("Matrix length: " + execution.getMatrixLengthDescription());
            lines.add("Message: " + nullToEmpty(execution.getMessage()));
        }

        lines.add("");
        lines.add("Overall execution time: " + formatDuration(report.getDurationMillis()));
        return lines;
    }

    private List<List<String>> paginate(List<String> lines) {
        List<List<String>> pages = new ArrayList<>();
        for (int index = 0; index < lines.size(); index += LINES_PER_PAGE) {
            pages.add(lines.subList(index, Math.min(index + LINES_PER_PAGE, lines.size())));
        }
        if (pages.isEmpty()) {
            pages.add(List.of(""));
        }
        return pages;
    }

    private String buildPagesObject(int pageCount) {
        StringBuilder kids = new StringBuilder();
        for (int index = 0; index < pageCount; index++) {
            kids.append(4 + index * 2).append(" 0 R ");
        }
        return "<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>";
    }

    private String buildContentStream(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        content.append("/F1 11 Tf\n");
        content.append("50 792 Td\n");
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                content.append("0 -16 Td\n");
            }
            content.append("(").append(escape(lines.get(index))).append(") Tj\n");
        }
        content.append("ET\n");
        return content.toString();
    }

    private void writeObject(ByteArrayOutputStream pdf, List<Integer> offsets, int number, String body) throws IOException {
        offsets.add(number, pdf.size());
        write(pdf, number + " 0 obj\n");
        write(pdf, body + "\n");
        write(pdf, "endobj\n");
    }

    private void writeStreamObject(ByteArrayOutputStream pdf, List<Integer> offsets, int number, String stream) throws IOException {
        byte[] streamBytes = stream.getBytes(StandardCharsets.ISO_8859_1);
        offsets.add(number, pdf.size());
        write(pdf, number + " 0 obj\n");
        write(pdf, "<< /Length " + streamBytes.length + " >>\n");
        write(pdf, "stream\n");
        pdf.write(streamBytes);
        write(pdf, "\nendstream\n");
        write(pdf, "endobj\n");
    }

    private void write(ByteArrayOutputStream pdf, String value) throws IOException {
        pdf.write(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return DATE_TIME_FORMAT.format(dateTime);
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long remainingMillis = millis % 1000;
        return seconds + "." + String.format("%03d", remainingMillis) + " seconds (" + millis + " ms)";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
