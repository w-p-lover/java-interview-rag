package com.javainterview.rag.interview.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExtractionBaselineTest {

    private static final String TEXT_LAYER_MISSING = "TEXT_LAYER_MISSING";
    private static final Path OUTPUT_DIRECTORY = Path.of("target", "pdf-extraction-baseline");
    private static final Path EXPECTED_REPORT = Path.of(
            "evaluation", "results", "2026-08-12-pdf-extraction-baseline.json");

    private final PdfInterviewDocumentTextExtractor extractor = new PdfInterviewDocumentTextExtractor();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void recordsDeterministicPdfExtractionBaseline() throws IOException {
        Files.createDirectories(OUTPUT_DIRECTORY.resolve("fixtures"));

        List<PdfExtractionCaseResult> cases = List.of(
                evaluateTextCase(singleColumnCase()),
                evaluateTextCase(twoColumnCase()),
                evaluateTextlessCase(textlessCase()));
        PdfExtractionBaselineReport report = new PdfExtractionBaselineReport(
                "pdfbox-position-sort-v1",
                "repository-deterministic-v1",
                cases.size(),
                (int) cases.stream().filter(PdfExtractionCaseResult::passed).count(),
                averageKeyPhraseHitRate(cases),
                cases.stream()
                        .filter(result -> "two-column".equals(result.caseId()))
                        .findFirst()
                        .orElseThrow()
                        .readingOrderPassed(),
                cases.stream()
                        .filter(result -> "textless".equals(result.caseId()))
                        .findFirst()
                        .orElseThrow()
                        .passed(),
                cases);

        Path actualReport = OUTPUT_DIRECTORY.resolve("report.json");
        objectMapper.writeValue(actualReport.toFile(), report);

        assertEquals(3, report.caseCount());
        assertEquals(2, report.passedCaseCount());
        assertEquals(1.0, report.keyPhraseHitRate());
        assertFalse(report.readingOrderPassed());
        assertTrue(report.textlessRejected());
        assertTrue(Files.exists(EXPECTED_REPORT), "缺少已审核的 PDF 解析基线报告");
        assertEquals(objectMapper.readTree(EXPECTED_REPORT.toFile()), objectMapper.readTree(actualReport.toFile()));
    }

    private PdfExtractionCaseResult evaluateTextCase(PdfBaselineCase baselineCase) throws IOException {
        writeFixture(baselineCase);
        InterviewDocumentExtraction extraction = extractor.extract(file(baselineCase));
        List<String> observedOrder = observedOrder(extraction.content(), baselineCase.expectedOrder());
        int hitCount = (int) baselineCase.keyPhrases().stream()
                .filter(extraction.content()::contains)
                .count();
        double hitRate = (double) hitCount / baselineCase.keyPhrases().size();
        Boolean orderPassed = baselineCase.expectedOrder().isEmpty()
                ? null
                : observedOrder.equals(baselineCase.expectedOrder());
        boolean passed = extraction.pageCount() == baselineCase.expectedPageCount()
                && hitCount == baselineCase.keyPhrases().size()
                && !Boolean.FALSE.equals(orderPassed);
        return new PdfExtractionCaseResult(
                baselineCase.caseId(),
                baselineCase.expectedPageCount(),
                extraction.pageCount(),
                baselineCase.keyPhrases().size(),
                hitCount,
                hitRate,
                orderPassed,
                baselineCase.expectedOrder(),
                observedOrder,
                null,
                null,
                passed);
    }

    private PdfExtractionCaseResult evaluateTextlessCase(PdfBaselineCase baselineCase) throws IOException {
        writeFixture(baselineCase);
        String actualFailureType = null;
        try {
            extractor.extract(file(baselineCase));
        } catch (IllegalArgumentException exception) {
            if ("未提取到可用文字，可能是扫描件；当前暂不支持 OCR".equals(exception.getMessage())) {
                actualFailureType = TEXT_LAYER_MISSING;
            }
        }
        boolean passed = TEXT_LAYER_MISSING.equals(actualFailureType);
        return new PdfExtractionCaseResult(
                baselineCase.caseId(),
                baselineCase.expectedPageCount(),
                null,
                0,
                0,
                null,
                null,
                List.of(),
                List.of(),
                TEXT_LAYER_MISSING,
                actualFailureType,
                passed);
    }

    private PdfBaselineCase singleColumnCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            addSingleColumnPage(document, List.of(
                    "JAVA_INTERVIEW_RESUME",
                    "NAME ALICE",
                    "EMAIL alice@example.test",
                    "SKILLS Java Spring MySQL"));
            addSingleColumnPage(document, List.of(
                    "PROJECT RAG KNOWLEDGE BASE",
                    "RESULT retrieval baseline"));
            return new PdfBaselineCase(
                    "single-column",
                    save(document),
                    2,
                    List.of("JAVA_INTERVIEW_RESUME", "alice@example.test", "Java Spring MySQL",
                            "PROJECT RAG KNOWLEDGE BASE", "retrieval baseline"),
                    List.of("JAVA_INTERVIEW_RESUME", "NAME ALICE", "EMAIL alice@example.test",
                            "SKILLS Java Spring MySQL", "PROJECT RAG KNOWLEDGE BASE",
                            "RESULT retrieval baseline"));
        }
    }

    private PdfBaselineCase twoColumnCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 770, 16, "TWO_COLUMN_RESUME");
                writeLine(stream, 72, 700, 12, "LEFT_SKILLS Java Spring");
                writeLine(stream, 72, 670, 12, "LEFT_EDUCATION Computer Science");
                writeLine(stream, 330, 700, 12, "RIGHT_EXPERIENCE Backend Engineer");
                writeLine(stream, 330, 670, 12, "RIGHT_PROJECT RAG Platform");
            }
            return new PdfBaselineCase(
                    "two-column",
                    save(document),
                    1,
                    List.of("TWO_COLUMN_RESUME", "LEFT_SKILLS", "LEFT_EDUCATION",
                            "RIGHT_EXPERIENCE", "RIGHT_PROJECT"),
                    List.of("TWO_COLUMN_RESUME", "LEFT_SKILLS", "LEFT_EDUCATION",
                            "RIGHT_EXPERIENCE", "RIGHT_PROJECT"));
        }
    }

    private PdfBaselineCase textlessCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            BufferedImage image = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
            graphics.drawString("SCANNED RESUME IMAGE", 90, 160);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 30));
            graphics.drawString("No PDF text operators on this page", 90, 250);
            graphics.dispose();
            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.drawImage(pdfImage, 72, 72, 451, 601);
            }
            return new PdfBaselineCase("textless", save(document), 1, List.of(), List.of());
        }
    }

    private void addSingleColumnPage(PDDocument document, List<String> lines) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            float y = 760;
            for (String line : lines) {
                writeLine(stream, 72, y, 12, line);
                y -= 30;
            }
        }
    }

    private void writeLine(PDPageContentStream stream, float x, float y, float fontSize, String text)
            throws IOException {
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private byte[] save(PDDocument document) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.save(output);
            return output.toByteArray();
        }
    }

    private void writeFixture(PdfBaselineCase baselineCase) throws IOException {
        Files.write(OUTPUT_DIRECTORY.resolve("fixtures").resolve(baselineCase.caseId() + ".pdf"),
                baselineCase.pdfContent());
    }

    private MockMultipartFile file(PdfBaselineCase baselineCase) {
        return new MockMultipartFile(
                "file", baselineCase.caseId() + ".pdf", "application/pdf", baselineCase.pdfContent());
    }

    private List<String> observedOrder(String content, List<String> expectedOrder) {
        List<String> observed = new ArrayList<>(expectedOrder);
        observed.sort((left, right) -> Integer.compare(content.indexOf(left), content.indexOf(right)));
        return List.copyOf(observed);
    }

    private double averageKeyPhraseHitRate(List<PdfExtractionCaseResult> cases) {
        return cases.stream()
                .filter(result -> result.keyPhraseHitRate() != null)
                .mapToDouble(PdfExtractionCaseResult::keyPhraseHitRate)
                .average()
                .orElse(0.0);
    }

    private record PdfBaselineCase(
            String caseId,
            byte[] pdfContent,
            int expectedPageCount,
            List<String> keyPhrases,
            List<String> expectedOrder) {
    }

    private record PdfExtractionCaseResult(
            String caseId,
            int expectedPageCount,
            Integer actualPageCount,
            int keyPhraseCount,
            int keyPhraseHitCount,
            Double keyPhraseHitRate,
            Boolean readingOrderPassed,
            List<String> expectedOrder,
            List<String> observedOrder,
            String expectedFailureType,
            String actualFailureType,
            boolean passed) {
    }

    private record PdfExtractionBaselineReport(
            String parser,
            String evaluator,
            int caseCount,
            int passedCaseCount,
            double keyPhraseHitRate,
            boolean readingOrderPassed,
            boolean textlessRejected,
            List<PdfExtractionCaseResult> cases) {
    }
}
