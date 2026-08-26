package com.hirehub.resume.text;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * PDFBox-based implementation of PDF text extraction.
 * Normalizes whitespace, rejects empty documents, and limits output length.
 */
@Slf4j
@Service
public class PdfBoxResumeTextExtractionService implements ResumeTextExtractionService {

    private static final int MAX_EXTRACTED_LENGTH = 10000;
    private static final int MIN_TEXT_LENGTH = 10;

    @Override
    public String extractText(InputStream pdfInputStream) throws TextExtractionException {
        if (pdfInputStream == null) {
            throw new TextExtractionException("PDF input stream is null");
        }

        try (PDDocument document = Loader.loadPDF(pdfInputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);

            if (rawText == null || rawText.isBlank()) {
                throw new TextExtractionException(
                        "Could not extract meaningful text from this PDF. " +
                        "It may be a scanned image. Please upload a PDF with selectable text.");
            }

            // Normalize whitespace
            String normalized = rawText
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();

            if (normalized.length() < MIN_TEXT_LENGTH) {
                throw new TextExtractionException(
                        "Extracted text is too short to analyze. " +
                        "Please upload a PDF with more selectable text content.");
            }

            // Truncate if excessively long
            if (normalized.length() > MAX_EXTRACTED_LENGTH) {
                normalized = normalized.substring(0, MAX_EXTRACTED_LENGTH);
            }

            return normalized;

        } catch (TextExtractionException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to read PDF document", e);
            throw new TextExtractionException("Failed to read the PDF file. It may be corrupted or invalid.");
        } catch (Exception e) {
            log.error("PDF text extraction failed", e);
            throw new TextExtractionException("PDF processing failed: " + e.getMessage());
        }
    }
}
