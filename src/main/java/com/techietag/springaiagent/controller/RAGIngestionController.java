package com.techietag.springaiagent.controller;

import com.techietag.springaiagent.dto.UserQuery;
import com.techietag.springaiagent.service.IngestionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller responsible for triggering and verifying the RAG (Retrieval-Augmented
 * Generation) ingestion process. This controller exposes endpoints used to
 * ingest documents (for example PDFs) into the vector store and to perform a quick
 * verification/search against the ingested content.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Accept uploaded files (multipart) and forward them to {@link IngestionService} for processing.</li>
 *   <li>Provide a verification/search endpoint to exercise the ingested vectors.</li>
 * </ul>
 *
 * Note: The heavy lifting is delegated to {@link IngestionService} which handles
 * reading documents, creating embeddings, and storing vectors in the configured
 * vector store. Production usage should add authentication/authorization and
 * more robust error reporting.</p>
 */
@RestController
public class RAGIngestionController {

    /**
     * Service that encapsulates ingestion and verification logic.
     * Injected via constructor to make the dependency explicit and easier to test.
     */
    private final IngestionService ingestionService;

    /**
     * Constructor injection of the {@link IngestionService}.
     * Using constructor injection makes the controller easier to unit test and
     * ensures the dependency is provided at creation time.
     *
     * @param ingestionService service that performs ingestion and search operations
     */
    public RAGIngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }


    // ---------------------------------------------------------------------
    // Endpoints
    // ---------------------------------------------------------------------

    /**
     * Trigger ingestion of a multipart file (PDF or other supported document) into the vector store.
     * This is a simple HTTP POST endpoint intended for manual or demo ingestion flows. It accepts
     * a multipart/form-data request with a "file" part and forwards the file bytes and original
     * filename to {@link IngestionService#ingest(byte[], String)} for processing.
     *
     * <p>Important notes:
     * <ul>
     *   <li>The endpoint consumes multipart/form-data and returns a short status message.</li>
     *   <li>In production, protect this endpoint and return richer status information (ingestion id, progress, errors).</li>
     * </ul>
     *
     * @param file the uploaded multipart file to ingest; must not be null and should contain the file bytes
     * @return a {@link ResponseEntity} containing a short status message and HTTP status code
     */
    @PostMapping(value = "api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            // Process the file content
            byte[] fileContent = file.getBytes();

            // Ingest the content into your vector database
            ingestionService.ingest(fileContent, file.getOriginalFilename());
            return ResponseEntity.ok("File uploaded and processed successfully.");
        } catch (Exception e){
            return ResponseEntity.status(500).body("Failed to process the file.");
        }
    }

    /**
     * Simple verification endpoint that performs a similarity search on the
     * ingested data. This is useful to validate that ingestion worked and that
     * the vector store returns relevant documents.
     *
     * @param userQuery the user query containing the search text
     * @return the search results from the vector store
     **/
    @PostMapping("api/verify-ingest")
    public Object verifyIngestedData(@RequestBody UserQuery userQuery) {
        // Perform a test search against the ingested vectors
        return ingestionService.verifyIngestedData(userQuery.query());
    }
}
