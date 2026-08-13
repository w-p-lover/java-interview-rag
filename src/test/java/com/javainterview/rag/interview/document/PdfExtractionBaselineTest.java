package com.javainterview.rag.interview.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PdfExtractionBaselineTest {

    private static final String TEXT_LAYER_MISSING = "TEXT_LAYER_MISSING";
    private static final Path OUTPUT_DIRECTORY = Path.of("target", "pdf-extraction-baseline");
    private static final Path CHINESE_FONT = Path.of("C:", "Windows", "Fonts", "simhei.ttf");
    private static final Path EXPECTED_REPORT = Path.of(
            "evaluation", "results", "2026-08-13-pdf-complex-resume-baseline.json");

    private final PdfInterviewDocumentTextExtractor extractor = new PdfInterviewDocumentTextExtractor();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void recordsDeterministicPdfExtractionBaseline() throws IOException {
        assumeTrue(Files.isRegularFile(CHINESE_FONT), "当前环境缺少中文 PDF 基线字体：" + CHINESE_FONT);
        Files.createDirectories(OUTPUT_DIRECTORY.resolve("fixtures"));

        List<PdfExtractionCaseResult> cases = List.of(
                evaluateTextCase(singleColumnCase()),
                evaluateTextCase(twoColumnCase()),
                evaluateTextCase(chineseResumeCase()),
                evaluateTextCase(dateRailCase()),
                evaluateTextCase(skillsTableCase()),
                evaluateTextCase(complexSidebarCase()),
                evaluateTextCase(mixedCase()),
                evaluateTextlessCase(textlessCase()));
        PdfExtractionBaselineReport report = new PdfExtractionBaselineReport(
                "pdfbox-layout-aware-v2",
                "repository-deterministic-v2",
                cases.size(),
                (int) cases.stream().filter(PdfExtractionCaseResult::passed).count(),
                averageKeyPhraseHitRate(cases),
                cases.stream()
                        .filter(result -> result.readingOrderPassed() != null)
                        .allMatch(PdfExtractionCaseResult::readingOrderPassed),
                cases.stream()
                        .filter(result -> "textless".equals(result.caseId()))
                        .findFirst()
                        .orElseThrow()
                        .passed(),
                cases.stream()
                        .filter(result -> "mixed".equals(result.caseId()))
                        .findFirst()
                        .orElseThrow()
                        .passed(),
                cases);

        Path actualReport = OUTPUT_DIRECTORY.resolve("report.json");
        objectMapper.writeValue(actualReport.toFile(), report);

        assertEquals(8, report.caseCount());
        assertEquals(8, report.passedCaseCount());
        assertEquals(1.0, report.keyPhraseHitRate());
        assertTrue(report.readingOrderPassed());
        assertTrue(report.textlessRejected());
        assertTrue(report.mixedDocumentIdentified());
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
                && !Boolean.FALSE.equals(orderPassed)
                && extraction.documentType() == baselineCase.expectedDocumentType()
                && extraction.reorderedPageCount() == baselineCase.expectedReorderedPageCount()
                && extraction.pagesNeedingOcr().equals(baselineCase.expectedPagesNeedingOcr());
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
                baselineCase.expectedDocumentType(),
                extraction.documentType(),
                baselineCase.expectedReorderedPageCount(),
                extraction.reorderedPageCount(),
                baselineCase.expectedPagesNeedingOcr(),
                extraction.pagesNeedingOcr(),
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
                PdfDocumentType.IMAGE_BASED,
                null,
                0,
                null,
                List.of(1),
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
                            "RESULT retrieval baseline"),
                    PdfDocumentType.TEXT_BASED,
                    0,
                    List.of());
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
                            "RIGHT_EXPERIENCE", "RIGHT_PROJECT"),
                    PdfDocumentType.TEXT_BASED,
                    1,
                    List.of());
        }
    }

    private PdfBaselineCase chineseResumeCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDFont font = PDType0Font.load(document, CHINESE_FONT.toFile());
            addSingleColumnPage(document, font, List.of(
                    "候选人：示例用户",
                    "求职方向：Java 后端开发工程师",
                    "技术能力：Spring AI 与 RAG",
                    "项目成果：固定评估集首位命中率百分之百"));
            return new PdfBaselineCase(
                    "chinese-resume",
                    save(document),
                    1,
                    List.of("示例用户", "Java 后端开发工程师", "Spring AI 与 RAG", "固定评估集"),
                    List.of("候选人", "求职方向", "技术能力", "项目成果"),
                    PdfDocumentType.TEXT_BASED,
                    0,
                    List.of());
        }
    }

    private PdfBaselineCase dateRailCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 760, 14, "WORK EXPERIENCE");
                writeLine(stream, 72, 710, 12, "BACKEND ENGINEER Example Company");
                writeLine(stream, 475, 710, 10, "2022-2024");
                writeLine(stream, 72, 670, 12, "JAVA DEVELOPER Sample Company");
                writeLine(stream, 475, 670, 10, "2020-2022");
            }
            return new PdfBaselineCase(
                    "date-rail",
                    save(document),
                    1,
                    List.of("WORK EXPERIENCE", "BACKEND ENGINEER", "2022-2024", "JAVA DEVELOPER", "2020-2022"),
                    List.of("WORK EXPERIENCE", "BACKEND ENGINEER", "2022-2024", "JAVA DEVELOPER", "2020-2022"),
                    PdfDocumentType.TEXT_BASED,
                    0,
                    List.of());
        }
    }

    private PdfBaselineCase skillsTableCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 760, 14, "TECHNICAL SKILLS");
                writeLine(stream, 72, 710, 12, "LANGUAGE");
                writeLine(stream, 250, 710, 12, "Java SQL");
                writeLine(stream, 72, 675, 12, "FRAMEWORK");
                writeLine(stream, 250, 675, 12, "Spring Boot Spring AI");
                writeLine(stream, 72, 625, 12, "PROJECT EVIDENCE FROM RAG EVALUATION");
                writeLine(stream, 72, 585, 12, "DATABASE");
                writeLine(stream, 250, 585, 12, "MySQL Qdrant");
            }
            return new PdfBaselineCase(
                    "skills-table",
                    save(document),
                    1,
                    List.of("TECHNICAL SKILLS", "LANGUAGE", "Java SQL", "FRAMEWORK", "Spring Boot Spring AI",
                            "PROJECT EVIDENCE", "DATABASE", "MySQL Qdrant"),
                    List.of("TECHNICAL SKILLS", "LANGUAGE", "Java SQL", "FRAMEWORK", "Spring Boot Spring AI",
                            "PROJECT EVIDENCE", "DATABASE", "MySQL Qdrant"),
                    PdfDocumentType.TEXT_BASED,
                    0,
                    List.of());
        }
    }

    private PdfBaselineCase complexSidebarCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 770, 16, "JAVA BACKEND RESUME");
                writeLine(stream, 72, 710, 12, "CONTACT example@test.local");
                writeLine(stream, 72, 675, 12, "SKILLS Java Spring MySQL");
                writeLine(stream, 330, 710, 12, "EXPERIENCE Backend Platform");
                writeLine(stream, 330, 675, 12, "PROJECT Interview RAG System");
                writeLine(stream, 330, 640, 12, "RESULT Evidence Based Scoring");
            }
            return new PdfBaselineCase(
                    "complex-sidebar",
                    save(document),
                    1,
                    List.of("JAVA BACKEND RESUME", "CONTACT", "SKILLS", "EXPERIENCE", "PROJECT", "RESULT"),
                    List.of("JAVA BACKEND RESUME", "CONTACT", "SKILLS", "EXPERIENCE", "PROJECT", "RESULT"),
                    PdfDocumentType.TEXT_BASED,
                    1,
                    List.of());
        }
    }

    private PdfBaselineCase mixedCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            addSingleColumnPage(document, List.of("MIXED_DOCUMENT_TEXT_PAGE", "Java backend profile"));
            addImageOnlyPage(document, "SCANNED SECOND PAGE");
            return new PdfBaselineCase(
                    "mixed",
                    save(document),
                    2,
                    List.of("MIXED_DOCUMENT_TEXT_PAGE", "Java backend profile"),
                    List.of("MIXED_DOCUMENT_TEXT_PAGE", "Java backend profile"),
                    PdfDocumentType.MIXED,
                    0,
                    List.of(2));
        }
    }

    private PdfBaselineCase textlessCase() throws IOException {
        try (PDDocument document = new PDDocument()) {
            addImageOnlyPage(document, "SCANNED RESUME IMAGE");
            return new PdfBaselineCase("textless", save(document), 1, List.of(), List.of(),
                    PdfDocumentType.IMAGE_BASED, 0, List.of(1));
        }
    }

    private void addImageOnlyPage(PDDocument document, String title) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        BufferedImage image = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        graphics.drawString(title, 90, 160);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 30));
        graphics.drawString("No PDF text operators on this page", 90, 250);
        graphics.dispose();
        PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.drawImage(pdfImage, 72, 72, 451, 601);
        }
    }

    private void addSingleColumnPage(PDDocument document, List<String> lines) throws IOException {
        addSingleColumnPage(document, new PDType1Font(Standard14Fonts.FontName.HELVETICA), lines);
    }

    private void addSingleColumnPage(PDDocument document, PDFont font, List<String> lines) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            float y = 760;
            for (String line : lines) {
                writeLine(stream, font, 72, y, 12, line);
                y -= 30;
            }
        }
    }

    private void writeLine(PDPageContentStream stream, float x, float y, float fontSize, String text)
            throws IOException {
        writeLine(stream, new PDType1Font(Standard14Fonts.FontName.HELVETICA), x, y, fontSize, text);
    }

    private void writeLine(PDPageContentStream stream, PDFont font, float x, float y, float fontSize, String text)
            throws IOException {
        stream.setFont(font, fontSize);
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
            List<String> expectedOrder,
            PdfDocumentType expectedDocumentType,
            int expectedReorderedPageCount,
            List<Integer> expectedPagesNeedingOcr) {
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
            PdfDocumentType expectedDocumentType,
            PdfDocumentType actualDocumentType,
            int expectedReorderedPageCount,
            Integer actualReorderedPageCount,
            List<Integer> expectedPagesNeedingOcr,
            List<Integer> actualPagesNeedingOcr,
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
            boolean mixedDocumentIdentified,
            List<PdfExtractionCaseResult> cases) {
    }
}
