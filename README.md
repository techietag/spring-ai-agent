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

docker image pull xxxx/spring-ai-agent:latest

docker container run -p 8080:8080  -e REDIS_HOST="REDIS-PORT" -e GITHUB-API-KEY="xxxx" -e MISTRAL-API-KEY="yyyy" xxxx/spring-ai-agent:latest
```

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


### ECS Fargate Deployment (Task Definition)

```json
{
  "compatibilities": [
    "EC2",
    "FARGATE",
    "MANAGED_INSTANCES"
  ],
  "containerDefinitions": [
    {
      "cpu": 0,
      "environment": [
        {
          "name": "GITHUB-API-KEY",
          "value": "value of PAT"
        },
        {
          "name": "MISTRAL-API-KEY",
          "value": "api key of mistral ai"
        },
        {
          "name": "REDIS_HOST",
          "value": "localhost"
        }
      ],
      "environmentFiles": [],
      "essential": true,
      "image": "1234567890.dkr.ecr.us-east-1.amazonaws.com/spring-ai-agent@sha256:3b83c737a6bbd9f4867f0da73f15598ed63da86067b59288f7892c79e60e7347",
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/agent",
          "awslogs-create-group": "true",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        },
        "secretOptions": []
      },
      "mountPoints": [],
      "name": "spring-ai-agent",
      "portMappings": [
        {
          "appProtocol": "http",
          "containerPort": 80,
          "hostPort": 80,
          "name": "spring-ai-agent-80-tcp",
          "protocol": "tcp"
        }
      ],
      "systemControls": [],
      "ulimits": [],
      "volumesFrom": []
    },
    {
      "cpu": 0,
      "environment": [],
      "environmentFiles": [],
      "essential": false,
      "image": "redis:latest",
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/agent",
          "awslogs-create-group": "true",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        },
        "secretOptions": []
      },
      "memoryReservation": 1024,
      "mountPoints": [],
      "name": "redis",
      "portMappings": [
        {
          "containerPort": 6379,
          "hostPort": 6379,
          "name": "redis-6379-tcp",
          "protocol": "tcp"
        }
      ],
      "systemControls": [],
      "volumesFrom": []
    }
  ],
  "cpu": "1024",
  "enableFaultInjection": false,
  "executionRoleArn": "arn:aws:iam::1234567890:role/ecsTaskExecutionRole",
  "family": "agent",
  "memory": "2048",
  "networkMode": "awsvpc",
  "placementConstraints": [],
  "registeredAt": "2026-01-05T09:58:02.604Z",
  "registeredBy": "arn:aws:iam::1234567890:user/xxxx",
  "requiresAttributes": [
    {
      "name": "com.amazonaws.ecs.capability.logging-driver.awslogs"
    },
    {
      "name": "ecs.capability.execution-role-awslogs"
    },
    {
      "name": "com.amazonaws.ecs.capability.ecr-auth"
    },
    {
      "name": "com.amazonaws.ecs.capability.docker-remote-api.1.19"
    },
    {
      "name": "com.amazonaws.ecs.capability.docker-remote-api.1.21"
    },
    {
      "name": "ecs.capability.execution-role-ecr-pull"
    },
    {
      "name": "com.amazonaws.ecs.capability.docker-remote-api.1.18"
    },
    {
      "name": "ecs.capability.task-eni"
    },
    {
      "name": "com.amazonaws.ecs.capability.docker-remote-api.1.29"
    }
  ],
  "requiresCompatibilities": [
    "FARGATE"
  ],
  "revision": 5,
  "runtimePlatform": {
    "cpuArchitecture": "X86_64",
    "operatingSystemFamily": "LINUX"
  },
  "status": "ACTIVE",
  "taskDefinitionArn": "arn:aws:ecs:us-east-1:1234567890:task-definition/agent:5",
  "volumes": [],
  "tags": []
}

```
