Project class documentation
==========================

Generated: 2026-01-04

This document summarizes the main Java classes in the project under `com.techietag.springaiagent`.
Each section includes the file path, brief purpose, notable public methods/fields, and quick examples or notes.

---

1) `SpringAiAgentApplication`
-----------------------------
- Path: `src/main/java/com/techietag/springaiagent/SpringAiAgentApplication.java`
- Purpose: Spring Boot application entry point. Boots the Spring application context.
- Key content:
  - public static void `main(String[] args)` — starts Spring via `SpringApplication.run(...)`.
- Notes: No additional configuration or behavior.

---

2) `MCPClientCustomizaton`
--------------------------
- Path: `src/main/java/com/techietag/springaiagent/config/MCPClientCustomizaton.java`
- Purpose: Spring `@Configuration` that provides an `McpSyncHttpClientRequestCustomizer` bean. Used to customize outgoing MCP (Model Context Protocol) client HTTP requests, e.g., add Authorization header for a configured GitHub MCP endpoint.
- Key fields (injected via `@Value`):
  - `gitHubMcpUr` — configured MCP URL for GitHub connection.
  - `githubApiKey` — API key to use for that MCP connection.
- Key bean:
  - `public McpSyncHttpClientRequestCustomizer requestCustomizer()` — returns a lambda that inspects outgoing request endpoint and adds an Authorization header when the endpoint equals the configured `gitHubMcpUr`.
- Notes:
  - This customization is scoped to the single endpoint value. If additional endpoints or headers are needed, extend the logic accordingly.

---

3) `UserQuery` (record)
------------------------
- Path: `src/main/java/com/techietag/springaiagent/dto/UserQuery.java`
- Purpose: Simple DTO (Java record) used to receive a user query payload in controller endpoints.
- Structure:
  - `record UserQuery(String query)` — single field `query`.
- Usage: Typical input for POST endpoints where the agent expects a natural language query.

---

4) `GetCurrentTime` (tool)
--------------------------
- Path: `src/main/java/com/techietag/springaiagent/tools/GetCurrentTime.java`
- Purpose: Exposes a small utility as a Spring `@Service` and a `@Tool` for Spring AI tool integration. It returns the current time in the provided IANA time zone.
- Public API:
  - `@Tool(name = "GetCurrentTimeTool", description = "Get the current time for a specified country")`
    `public String getCurrentTime(String country)` — returns current zoned date-time for the supplied IANA zone id (e.g., `Europe/London`). If invalid, returns a user-friendly error message instead of throwing.
- Behavior/Notes:
  - Internally uses `java.time.ZoneId.of(country)` and `ZonedDateTime.now(zoneId)`.
  - The tool method prints a diagnostic line to stdout for visibility.
  - This bean is registered as a default tool for the `ChatClient` builder in `AgentController`.

---

5) `RAGIngestionController` (REST controller)
---------------------------------------------
- Path: `src/main/java/com/techietag/springaiagent/controller/RAGIngestionController.java`
- Purpose: Exposes endpoints to trigger ingestion of documents into the project's vector store and to verify ingested data via similarity search.
- Endpoints:
  - `GET api/ingest/{fileName}` — triggers ingestion of the specified file (file name passed as path variable). If `fileName` is omitted the service's default is used. Calls `IngestionService.ingestPDF(String)`.
    - Returns: plain String status message like "Ingestion completed".
  - `POST api/verify-ingest` — accepts `UserQuery` in the request body and runs similarity search using `IngestionService.verifyIngestedData(query)`.
    - Returns: the verification result (object returned by `IngestionService`).
- Notes:
  - `IngestionService` does the heavy lifting (PDF reading, chunking, storing).
  - Endpoints are intentionally simple and suitable for manual/demo usage. Consider adding authentication and richer DTOs for production.

---

6) `AgentController` (REST controller)
--------------------------------------
- Path: `src/main/java/com/techietag/springaiagent/controller/AgentController.java`
- Purpose: Builds and exposes endpoints to interact with the configured AI agent (chat client and tools). The controller configures `ChatClient` with advisors, registers the `GetCurrentTime` tool, and wires tool callbacks.
- Key injected/constructed components:
  - `ChatClient chatClient` — built from `ChatClient.Builder` with default advisors and tools.
  - `GetCurrentTime getCurrentTimeTool` — the local tool bean exposed as a tool to the chat client.
  - `ToolCallbackProvider toolCallbackProvider` — provides the tool callbacks (registered tool handlers).
  - `VectorStore vectorStore` — injected vector store used by the QuestionAnswerAdvisor.
  - `Resource systemText` (value `classpath:/prompt-templates/system-message.st`) — path to the system prompt template resource.
- Endpoints:
  - `GET /api/agent` — returns a JSON-friendly summary of the agent configuration including:
    - `chatClientClass` — runtime class name of the ChatClient
    - `vectorStoreClass` — runtime class name of the VectorStore
    - `systemPromptFile` — filename for the system prompt resource (if available)
    - `toolCallbackCount` — number of registered tool callbacks
    - `tools` — list of tool metadata maps (name, description, callback class, whether registered, whether local, declaring class where applicable)
    - The controller merges information from `ToolCallbackProvider` and reflects the local `GetCurrentTime` bean to ensure local tools are included.
  - `POST /api/agent` — accepts `UserQuery` and runs the chat client flow:
    - Builds a `SystemMessage` (agent persona + tasks) and a `UserMessage` from `userQuery`.
    - Constructs a `Prompt` and calls `chatClient.prompt(prompt).call()` to retrieve the agent's content.
    - Returns the agent's text content as a String.
- Notes:
  - The `GET /api/agent` endpoint is useful for debugging and discovering tools and configuration at runtime.
  - The `POST /api/agent` endpoint uses a fixed system persona; consider externalizing or templating the persona and accepting additional request parameters for production.

---

7) `IngestionService` (service)
-------------------------------
- Path: `src/main/java/com/techietag/springaiagent/service/IngestionService.java`
- Purpose: Handles ingesting PDF documents into the configured `VectorStore` and running similarity searches for verification.
- Key fields:
  - `VectorStore vectorStore` — where documents are stored.
  - `ResourceLoader resourceLoader` — used to resolve files from `classpath:/docs/`.
  - `Resource faqPdf` — default FAQ PDF injected from the classpath (value `classpath:/docs/Flexora_FAQ.pdf`).
- Public methods:
  - `public String ingestPDF(String fileName)` — attempts to find `classpath:/docs/{fileName}` and, if present, uses a `PagePdfDocumentReader` to chunk the PDF into `Document` pages and calls `vectorStore.add(...)` to ingest them. Returns status string like "FAQ data ingested successfully" or "File Not Found".
  - `public String verifyIngestedData(String query)` — runs `vectorStore.similaritySearch(query)`, filters candidate documents by non-null and `score > 0.8`, and returns concatenated document text lines.
- Notes and caveats:
  - The service uses synchronous ingestion and a simple confidence threshold filter (0.8). In production, you may want asynchronous ingestion, progress reporting, and richer response DTOs.
  - `ingestPDF` uses a default filename when `fileName` is blank.

---

Quick usage examples
--------------------
- Trigger ingestion of `p` (via the controller):
  - GET `http://localhost:8080/api/ingest/Flexora_FAQ.pdf`
- Verify ingestion by running a search query:
  - POST `http://localhost:8080/api/verify-ingest` with JSON body: `{ "query": "What is Flexora?" }`
- Inspect agent configuration and tools:
  - GET `http://localhost:8080/api/agent` — returns a JSON map listing tools (including the local `GetCurrentTimeTool` when available).
- Invoke chat agent:
  - POST `http://localhost:8080/api/agent` with JSON body: `{ "query": "Hello" }` — returns agent content text.

---

Notes & next steps
------------------
- This documentation is derived from the current source files in `src/main/java/com/techietag/springaiagent`.
- Suggested improvements:
  - Add JavaDoc to remaining classes (some already have it e.g., `GetCurrentTime`, `IngestionService`).
  - Add unit tests for `IngestionService` (happy path and missing-file case) and integration tests for controllers.
  - Consider exposing tool metadata as a typed DTO rather than raw maps for `/api/agent`.

If you'd like, I can:
- Generate a more formal Markdown per-class file in `docs/classes/`.
- Produce a simple OpenAPI spec based on the controllers.
- Create unit tests for `IngestionService` and the controller endpoints.

Which of these would you like me to implement next?
