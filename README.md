Spring AI Agent
================

A small Spring Boot demo application that integrates Spring AI tooling (ChatClient, tools, and vector stores) to showcase:

- building a ChatClient with advisors and tools,
- exposing simple REST endpoints for interacting with the agent,
- ingesting PDF documents into a VectorStore and running similarity searches,
- registering and exposing local tool methods as runtime-discoverable tools.

This repository is intended as a proof-of-concept / reference for integrating Spring AI tooling
with a Spring Boot application and a vector store.

Quick overview
--------------
- Language & Framework: Java, Spring Boot (Gradle wrapper included)
- Key capabilities:
  - Agent (LLM) chat via a configured ChatClient
  - Local tool registration (e.g., GetCurrentTime)
  - Document ingestion (PDF) into a VectorStore for RAG
  - Runtime tool discovery via an endpoint

Main components
---------------
- `SpringAiAgentApplication` — Spring Boot application entry point.
- `AgentController` — builds the ChatClient, registers advisors/tools, exposes:
  - `GET  /api/agent` — returns runtime metadata (tools, advisors, model, vector store)
  - `POST /api/agent` — forwards a `UserQuery` to the ChatClient and returns the response
- `RAGIngestionController` — exposes ingestion and verification endpoints:
  - `GET  /api/ingest/{fileName}` — ingest a PDF from `classpath:/docs/`
  - `POST /api/verify-ingest` — verify ingested data using a `UserQuery` payload
- `IngestionService` — reads PDFs, chunks them into Documents, and adds them to the VectorStore.
- `GetCurrentTime` — a local `@Service` containing a `@Tool` method that returns the current time for a provided IANA timezone (registered as a tool on the ChatClient).
- `MCPClientCustomizaton` — configuration class that customizes MCP HTTP requests (adds Authorization header for a configured MCP endpoint).
- `UserQuery` — simple record DTO for controller requests.

Where to find docs
------------------
- `docs/CLASS_DOCUMENTATION.md` — high-level per-class summaries (auto-generated earlier).

How to run (development)
------------------------
1. Start the application with the Gradle wrapper:

```powershell
./gradlew bootRun
```

or on Windows PowerShell (Git Bash / Windows friendly):

```powershell
.\gradlew.bat bootRun
```

2. Default server port: 8080 (unless changed in `src/main/resources/application.yml`).

Example requests
----------------
- Inspect agent capabilities:

```http
GET http://localhost:8080/api/agent
```

- Call the agent chat endpoint (JSON body):

```http
POST http://localhost:8080/api/agent
Content-Type: application/json

{
  "query": "Hello agent, what's the time in Europe/London?"
}
```

- Ingest a PDF from classpath `docs/` (example):

```http
GET http://localhost:8080/api/ingest/Flexora_FAQ.pdf
```

- Verify ingestion via similarity search:

```http
POST http://localhost:8080/api/verify-ingest
Content-Type: application/json

{
  "query": "What is Flexora?"
}
```

Configuration
-------------
- Application properties are under `src/main/resources/application.yml`.
- MCP client customization uses properties:
  - `spring.ai.mcp.client.streamable-http.connections.github-mcp.url`
  - `spring.ai.mcp.client.streamable-http.connections.github-mcp.api-key`

- Vector store, chat client model, and tool callback configurations are provided via Spring configuration (auto-configured or defined in `build.gradle` / environment). Check `AgentController` and `IngestionService` for how these beans are wired.

Testing & build
---------------
- Build the project:

```powershell
./gradlew build
```

- Run tests:

```powershell
./gradlew test
```

Notes & next steps
------------------
- This repository is a demo/POC. Consider the following improvements for production:
  - Add authentication/authorization for endpoints that trigger ingestion or call the agent.
  - Make the system prompt and tool registrations configurable rather than hard-coded.
  - Add proper error handling, async ingestion, and progress reporting for large documents.
  - Add integration tests for controller endpoints and the vector store.
  - Generate Javadoc and/or OpenAPI specs for the REST endpoints.

If you'd like, I can:
- Generate a per-class Markdown documentation set under `docs/classes/`.
- Produce an OpenAPI (Swagger) spec for the controllers.
- Run a full build and the test suite and report results.


License & attribution
---------------------
This project is a small demo; adapt the code for your needs and ensure appropriate licensing if you reuse third-party code or models.


