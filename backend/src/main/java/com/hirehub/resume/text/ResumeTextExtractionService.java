package com.hirehub.resume.text;

import java.io.InputStream;

/**
 * Abstraction for extracting text from resume PDFs.
 * Allows swapping extraction libraries without changing callers.
 */
public interface ResumeTextExtractionService {

    /**
     * Extract text from a PDF input stream.
     *
     * @param pdfInputStream the PDF file as a stream
     * @return extracted and normalized text
     * @throws TextExtractionException if extraction fails
     */
    String extractText(InputStream pdfInputStream) throws TextExtractionException;
}
