package com.example.book2quiz.service;

import com.example.book2quiz.config.ChapterDetectionProperties;
import com.example.book2quiz.model.Book;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ChapterExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ChapterExtractionService.class);

    private final FileStorageService fileStorageService;
    private final ChapterDetectionProperties properties;

    public ChapterExtractionService(FileStorageService fileStorageService,
                                    ChapterDetectionProperties properties) {
        this.fileStorageService = fileStorageService;
        this.properties = properties;
    }

    /** A detected chapter whose PDF has already been uploaded to MinIO. */
    public record ExtractedChapter(String title, int startPage, int endPage, String pdfObjectKey) {
    }

    /**
     * @param chapters        the detected chapters (never empty)
     * @param detectionFailed true when no chapters could be detected and the whole
     *                        book was emitted as a single chapter for review
     */
    public record ExtractionResult(List<ExtractedChapter> chapters, boolean detectionFailed) {
    }

    /** Detected boundary before splitting: title + 1-based start page. */
    private record Boundary(String title, int startPage) {
    }

    public ExtractionResult extractAndSplit(Book book) {
        byte[] pdfBytes = fileStorageService.downloadFile(book.getFileKey());
        log.info("Book {}: downloaded PDF ({} bytes) for chapter extraction", book.getId(), pdfBytes.length);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();

            List<Boundary> boundaries = detectFromOutline(document);
            boolean detectionFailed = false;

            if (boundaries.isEmpty()) {
                log.info("Book {}: no usable PDF outline, falling back to heuristic detection", book.getId());
                boundaries = detectFromHeuristic(document);
            }

            if (boundaries.isEmpty()) {
                log.warn("Book {}: chapter detection failed, treating whole book as a single chapter", book.getId());
                boundaries = List.of(new Boundary(book.getTitle(), 1));
                detectionFailed = true;
            }

            List<ExtractedChapter> chapters = split(book, document, boundaries, totalPages);
            log.info("Book {}: split into {} chapter PDF(s) (detectionFailed={})",
                    book.getId(), chapters.size(), detectionFailed);
            return new ExtractionResult(chapters, detectionFailed);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process PDF for book " + book.getId(), e);
        }
    }

    private List<Boundary> detectFromOutline(PDDocument document) throws IOException {
        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        if (outline == null) {
            return List.of();
        }

        List<Boundary> boundaries = new ArrayList<>();
        for (PDOutlineItem item : outline.children()) {
            String title = item.getTitle();
            PDPage page = item.findDestinationPage(document);
            if (title == null || title.isBlank() || page == null) {
                continue;
            }
            int index = document.getPages().indexOf(page);
            if (index < 0) {
                continue;
            }
            boundaries.add(new Boundary(title.trim(), index + 1));
        }
        return normalize(boundaries);
    }

    private List<Boundary> detectFromHeuristic(PDDocument document) throws IOException {
        HeadingCollector collector = new HeadingCollector();
        collector.setStartPage(1);
        collector.setEndPage(document.getNumberOfPages());
        collector.getText(document);

        List<HeadingCollector.Run> runs = collector.getRuns();
        if (runs.isEmpty()) {
            return List.of();
        }

        float bodyFontSize = estimateBodyFontSize(runs);
        double threshold = bodyFontSize * properties.getFontSizeRatio();

        List<Pattern> patterns = properties.getPatterns().stream()
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
                .toList();

        List<Boundary> boundaries = new ArrayList<>();
        for (HeadingCollector.Run run : runs) {
            if (run.fontSize() < threshold) {
                continue;
            }
            boolean matches = patterns.stream().anyMatch(p -> p.matcher(run.text()).matches());
            if (matches) {
                boundaries.add(new Boundary(run.text(), run.page()));
            }
        }
        return normalize(boundaries);
    }

    /** Sorts by start page and drops entries that share a start page with an earlier one. */
    private List<Boundary> normalize(List<Boundary> boundaries) {
        List<Boundary> sorted = new ArrayList<>(boundaries);
        sorted.sort((a, b) -> Integer.compare(a.startPage(), b.startPage()));

        List<Boundary> result = new ArrayList<>();
        int lastStart = -1;
        for (Boundary b : sorted) {
            if (b.startPage() != lastStart) {
                result.add(b);
                lastStart = b.startPage();
            }
        }
        return result;
    }

    private List<ExtractedChapter> split(Book book, PDDocument document,
                                         List<Boundary> boundaries, int totalPages) throws IOException {
        List<ExtractedChapter> chapters = new ArrayList<>();
        for (int i = 0; i < boundaries.size(); i++) {
            Boundary current = boundaries.get(i);
            int startPage = Math.max(1, current.startPage());
            int endPage = (i + 1 < boundaries.size())
                    ? boundaries.get(i + 1).startPage() - 1
                    : totalPages;
            endPage = Math.min(Math.max(endPage, startPage), totalPages);

            int ordinal = i + 1;
            byte[] chapterPdf = extractPages(document, startPage, endPage);
            String objectKey = "books/" + book.getId() + "/chapters/" + ordinal + ".pdf";
            fileStorageService.uploadBytes(chapterPdf, objectKey, "application/pdf");

            log.info("Book {}: chapter {} '{}' pages {}-{} uploaded to {}",
                    book.getId(), ordinal, current.title(), startPage, endPage, objectKey);
            chapters.add(new ExtractedChapter(current.title(), startPage, endPage, objectKey));
        }
        return chapters;
    }

    private byte[] extractPages(PDDocument source, int startPage, int endPage) throws IOException {
        try (PDDocument chapterDoc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int p = startPage; p <= endPage; p++) {
                chapterDoc.importPage(source.getPage(p - 1));
            }
            chapterDoc.save(out);
            return out.toByteArray();
        }
    }

    /** Most common font size, weighted by the length of each text run. */
    private float estimateBodyFontSize(List<HeadingCollector.Run> runs) {
        Map<Integer, Integer> weights = new HashMap<>();
        for (HeadingCollector.Run run : runs) {
            int bucket = Math.round(run.fontSize());
            weights.merge(bucket, run.text().length(), Integer::sum);
        }
        return weights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(12)
                .floatValue();
    }

    /**
     * Collects text runs with their page number and font size so headings can be
     * identified by relative font size.
     */
    private static final class HeadingCollector extends PDFTextStripper {

        record Run(int page, String text, float fontSize) {
        }

        private final List<Run> runs = new ArrayList<>();

        HeadingCollector() throws IOException {
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            String trimmed = text.trim();
            if (!trimmed.isEmpty() && !textPositions.isEmpty()) {
                float maxSize = 0f;
                for (TextPosition tp : textPositions) {
                    maxSize = Math.max(maxSize, tp.getFontSizeInPt());
                }
                runs.add(new Run(getCurrentPageNo(), trimmed, maxSize));
            }
            super.writeString(text, textPositions);
        }

        List<Run> getRuns() {
            return runs;
        }
    }
}
