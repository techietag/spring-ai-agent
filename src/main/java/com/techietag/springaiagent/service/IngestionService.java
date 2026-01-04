package com.techietag.springaiagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service responsible for ingesting documents (PDFs) into a configured VectorStore
 * and performing simple verification/search operations against the ingested data.
 *
 * <p>Responsibilities:
 * - Load a PDF resource from the classpath (or other ResourceLoader-backed location).
 * - Chunk the PDF into document pages using {@link PagePdfDocumentReader}.
 * - Add the resulting {@link Document} objects to the provided {@link VectorStore}.
 * - Provide a small verification method that runs a similarity search and returns
 *   high-confidence matches as a single concatenated string.
 *
 * Notes:
 * - The concrete VectorStore implementation (for example a Redis-backed vector store)
 *   is provided by Spring (auto-configured elsewhere) and injected into this service.
 * - This class is intentionally simple for demo/POC usage and can be extended to
 *   return richer status objects, async ingestion, progress reporting, and error handling.
 */
@Service
public class IngestionService {

    /**
     * The vector store where embeddings/documents are stored. Provided by Spring's
     * dependency injection. In this project the implementation is configured in Gradle
     * and uses a Redis-backed VectorStore with an embedding model.
     */
    private final VectorStore vectorStore;

    /**
     * ResourceLoader used to resolve classpath resources (PDF files) by name.
     * This lets callers specify the filename and the service locate it on the classpath.
     */
    private final ResourceLoader resourceLoader;

    // Simple logger for informational messages and debugging
    Logger logger = LoggerFactory.getLogger(IngestionService.class);

     /**
     * Constructor used by Spring to inject the required dependencies.
     *
     * @param vectorStore the VectorStore implementation to add documents to
     * @param resourceLoader a ResourceLoader for locating resources (e.g., classpath files)
     */
    public IngestionService(VectorStore vectorStore, ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.vectorStore = vectorStore;
    }

    /**
     * Ingests the specified PDF (by filename) into the configured vector store.
     * The method will:
     * 1. Resolve the requested file from classpath:/docs/ by provided filename.
     * 2. If the file exists, chunk it into page-level {@link Document} instances
     *    using {@link PagePdfDocumentReader} and add them to the VectorStore.
     * 3. Return a short human-friendly status string.
     *
     * Behavior/edge-cases:
     * - If fileName is null/empty, a default filename is used.
     * - If the resource does not exist the method logs and returns "File Not Found".
     * - This method performs ingestion synchronously and blocks until vectorStore.add(...) completes.
     *
     * @param fileName the name of the file in classpath:/docs/ to ingest (for example "Flexora_FAQ.pdf")
     * @return a short status message indicating success or failure
     */
    public String ingestPDF(String fileName) {

        logger.info("File to be ingested : {}", fileName);
        if (fileName == null || fileName.isBlank()) {
            // Provide a sensible default when the caller doesn't pass a filename
            fileName = "online_shopping_faq.pdf";
        }

        // Resolve the resource from the classpath:/docs/ location
        Resource file = resourceLoader.getResource("classpath:/docs/" + fileName);

        if (file.exists()) {
            // Use PagePdfDocumentReader to split the PDF into page-level Document objects.
            // Note: the existing code uses the injected faqPdf resource; this keeps the
            // behaviour unchanged (it reads the configured FAQ PDF).
            PagePdfDocumentReader pdfDocumentReader = new PagePdfDocumentReader(file);
            List<Document> chunkedDocuments = pdfDocumentReader.get();

            // Add the chunked documents to the vector store so they are available for
            // similarity search and retrieval later.
            vectorStore.add(chunkedDocuments);
            return "FAQ data ingested successfully";
        } else {
            logger.info("File not found : {}", fileName);
            return "File Not Found";
        }

    }

    /**
     * Performs a similarity search against the vector store using the provided query
     * and returns a concatenated text of highly confident matches.
     *
     * Implementation details:
     * - Calls {@link VectorStore#similaritySearch(String)} to get candidate documents.
     * - Filters out nulls and documents with a score is less than 0.8 (confidence threshold).
     * - Returns the concatenated text of matching documents separated by newlines.
     *
     * @param query the natural language query used for similarity search
     * @return concatenated high-confidence match texts (or empty string when none match)
     */
    public String verifyIngestedData(String query) {

        // Perform the similarity search. Depending on the VectorStore implementation
        // this may perform network IO or CPU work to compute/retrieve nearest neighbors.
        List<Document> results = vectorStore.similaritySearch(query);
        return results.stream()
                .filter(Objects::nonNull)
                .filter(result -> result.getScore() != null && result.getScore() > 0.8)
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

    }

}
