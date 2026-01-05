Spring AI Agent
================

A small Spring Boot demo application to demonstrate how an agent can be created
which integrates with `local tools` , `mcp servers` and `RAG`

Quick overview
--------------
- Language & Framework: Java, Spring Boot (Gradle wrapper included)
- Key capabilities:
  - Agent (LLM - Mistral AI) chat via a configured ChatClient
  - Local tool registration (e.g., GetCurrentTime)
  - GitHub Remote MCP tool integration
  - Document ingestion (PDF) into a VectorStore (Redis) for RAG
  - Runtime tool discovery via an endpoint
  - Advisors used
    -  `SimpleLoggerAdvisor` - to log interactions
    - `QuestionAnswerAdvisor` - to answer questions based on ingested documents 
    (RAG integration)
      
This project uses `Spring Docker Compose` to spin off Redis container.


Main components
---------------
- `SpringAiAgentApplication` 
  - Spring Boot application entry point.
  - Implements `Command Line Runner` to Ingest faq document (available under 
  `resources/docs/online_shopping_faq.pdf`) on startup using `IngestionService`.
  
- `AgentController` 
  - builds the ChatClient, registers advisors/tools, exposes:
    - `GET  /api/agent` 
      - returns information about Agents (tools, advisors, model, vector store)
    - `POST /api/agent` 
      - forwards a `UserQuery` to the ChatClient and returns the response based on LLM, RAG , Local Tool and MCP server.

- `RAGIngestionController` 
  - exposes ingestion and verification endpoints:
    - `GET  /api/ingest/{fileName}` — ingest a PDF from `classpath:/docs/`
    - `POST /api/verify-ingest` — verify ingested data using a `UserQuery` payload

- `IngestionService` 
  - reads PDFs, chunks them into Documents, and adds them to the VectorStore.

- `GetCurrentTime` — a local `@Service` containing a `@Tool` method that returns the current time for a provided IANA timezone (registered as a tool on the ChatClient).

- `MCPClientCustomizaton` — configuration class that customizes MCP HTTP requests (adds Authorization header for a configured MCP endpoint).

- `UserQuery` — simple record DTO for controller requests.


### Deployment 

```docker

docker image pull saurabhaga/spring-ai-agent:latest

docker container run -p 8080:8080  -e REDIS_HOST="REDIS-PORT" 
-e GITHUB-API-KEY="xxxx" -e MISTRAL-API-KEY="yyyy" saurabhaga/spring-ai-agent:latest
``
`
### Sample Requests

- Ingest a PDF document:
  ```bash
  curl --location 'http://localhost:8080/api/upload' \
--form 'file=@"Bill.pdf"'
  ```
- Verify ingested data:
  ```bash
  curl -X POST http://localhost:8080/api/verify-ingest \
       -H "Content-Type: application/json" \
       -d '{"query": "What is the return policy for online orders?"}'
  ```
- Interact with the agent:
  ```bash
  curl -X POST http://localhost:8080/api/agent \
       -H "Content-Type: application/json" \
       -d '{"query": "What is the current time in New York?"}'
  ```
  
- Get agent info:
  ```bash
  curl -X GET http://localhost:8080/api/agent
  ```
  

