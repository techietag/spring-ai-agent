package com.techietag.springaiagent.controller;

import com.techietag.springaiagent.dto.UserQuery;
import com.techietag.springaiagent.service.IngestionService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for triggering and verifying the RAG (Retrieval-Augmented
 * Generation) ingestion process. This controller exposes simple endpoints used to
 * ingest documents (e.g., PDFs) into the vector store and to perform a quick
 * verification/search against the ingested content.
 * <p>
 * Note: The heavy lifting is delegated to {@link IngestionService} which handles
 * reading documents, creating embeddings, and storing vectors in the configured
 * vector store.
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
     * Trigger ingestion of PDFs (or other supported documents) into the vector store.
     * This is a simple HTTP GET endpoint used for manual or demo ingestion flows.
     * <p>
     * Real production code would likely protect this endpoint (authentication/authorization)
     * and provide more detailed status reporting.
     * @param fileName optional name of the file to ingest; if not provided, a default FAQ PDF is used
     * @return a short status message indicating the ingestion was invoked
     */
    @GetMapping("api/ingest/{fileName}")
    public String ingestData(
            @PathVariable(name = "fileName", required = false) String fileName) {

        // Call the service to perform the actual ingestion work (IO, embedding, storing)
        ingestionService.ingestPDF(fileName);

        // Return a human-readable confirmation. For richer clients use a proper DTO.
        return "Ingestion completed";
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
