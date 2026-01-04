package com.techietag.springaiagent.controller;

import com.techietag.springaiagent.dto.UserQuery;
import com.techietag.springaiagent.tools.GetCurrentTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * REST controller responsible for constructing and exposing the AI agent endpoints.
 *
 * <p>Responsibilities:
 * - Build the {@link ChatClient} with default advisors, register local tools, and wire tool callbacks.
 * - Provide a discovery endpoint to report runtime agent metadata (tools, advisors, model, vector store).
 * - Provide an endpoint to forward user queries to the agent and return responses.
 *
 * Implementation notes:
 * - The controller registers the local {@link GetCurrentTime} bean as a tool on the chat client.
 * - Tool callback metadata is available via {@link ToolCallbackProvider}.
 */
@RestController
public class AgentController {

    // Logger for informational messages
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    // The ChatClient used to prompt the LLM and run tools/advisors
    private final ChatClient chatClient;

    // Local tool bean - provides a method annotated with @Tool used by the agent
    private final GetCurrentTime getCurrentTimeTool;

    // Provider for tool callbacks which exposes runtime metadata about registered tools
    private final ToolCallbackProvider toolCallbackProvider;

    // Vector store used by the QuestionAnswerAdvisor for retrieval-augmented responses
    private final VectorStore vectorStore;

    /**
     * Construct the controller and build the ChatClient.
     *
     * @param chatClientBuilder builder used to construct a ChatClient instance
     * @param aGetCurrentTimeTool local tool bean (GetCurrentTime) to register as a tool
     * @param aToolCallbackProvider provider that exposes runtime ToolCallback instances
     * @param vectorStore vector store used for retrieval/advisor capabilities
     */
    public AgentController(ChatClient.Builder chatClientBuilder,
                           GetCurrentTime aGetCurrentTimeTool,
                           ToolCallbackProvider aToolCallbackProvider,
                           VectorStore vectorStore) {

        // Save injected beans for later use and reporting
        this.getCurrentTimeTool = aGetCurrentTimeTool;
        this.toolCallbackProvider = aToolCallbackProvider;
        this.vectorStore = vectorStore;

        // Build ChatClient with default advisors (logger + QA advisor) and register
        // the local GetCurrentTime tool plus any tool callbacks provided by the application.
        this.chatClient = chatClientBuilder
                .defaultAdvisors(List.of(
                        new SimpleLoggerAdvisor(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()))
                .defaultTools(getCurrentTimeTool)
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
                .build();

    }

    /**
     * GET /api/agent — discovery endpoint that returns runtime metadata about the agent.
     *
     * The returned map is JSON-serializable by Spring and contains:
     * - LLM model identifier (if available),
     * - chat client implementation class name,
     * - vector store implementation class name,
     * - number of tool callbacks registered,
     * - a list of tool metadata (name and description),
     * - configured advisors.
     *
     * @return a map with agent configuration and tool metadata
     */
    @GetMapping("/api/agent")
    public Map<String, Object> agentCapabilities() {
        log.info("Agent capabilities endpoint called");

        // Obtain a request-spec from the chat client to inspect configured callbacks/advisors.
        DefaultChatClient.DefaultChatClientRequestSpec chatClientRequestSpec =
                (DefaultChatClient.DefaultChatClientRequestSpec) chatClient.prompt();

        // Extract registered tool callbacks and present their definitions
        var toolCallbacks = chatClientRequestSpec.getToolCallbacks();
        List<Map<String, String>> tools = new ArrayList<>();
        for (var cb : toolCallbacks) {
            var def = cb.getToolDefinition();
            Map<String, String> toolInfo = new HashMap<>();
            toolInfo.put("name", def.name());
            toolInfo.put("description", def.description());
            tools.add(toolInfo);
        }

        // Collect advisor names for visibility
        List<String> advisors = new ArrayList<>();
        chatClientRequestSpec.getAdvisors().forEach(advisor -> advisors.add(advisor.getName()));

        // Compose a JSON-serializable map with the discovered information
        Map<String, Object> info = new HashMap<>();
        info.put("LLM Model", chatClientRequestSpec.getChatOptions().getModel());
        info.put("chatClientClass", chatClient != null ? chatClient.getClass().getName() : null);
        info.put("vectorStoreClass", vectorStore != null ? vectorStore.getClass().getName() : null);
        info.put("toolCallbackCount", toolCallbacks.size());
        info.put("tools", tools);
        info.put("advisors", advisors);
        return info;
    }

    /**
     * POST /api/agent — forwards a user query to the configured ChatClient instance.
     *
     * The method builds a `SystemMessage` persona for the assistant, packages the
     * user's text as a `UserMessage`, constructs a `Prompt`, and calls the ChatClient.
     * The textual content of the ChatClient response is returned directly.
     *
     * @param userQuery request body containing the user's query text
     * @return the assistant's response content as plain text
     */
    @PostMapping("/api/agent")
    public String agentEndpoint(@RequestBody UserQuery userQuery) {

        log.info("Agent endpoint called with {}", userQuery.query());

        // System message defines the assistant's persona and tasks
        SystemMessage systemMessage = new SystemMessage("""
                You are an intelligent assistant named Jennie.
                Your tasks are as follows:
                1. Retrieve the user's GitHub username and the current time using the available tools.
                2. Greet the user appropriately with "Good Morning," "Good Afternoon," or "Good Night," based on their current time.
                3. Provide a complete response to the user's query in presentable format (no markdown format).
                4. In your response, explicitly mention the user's GitHub username and the current time.
                5. Conclude by thanking the user for using your services.
                """);

        // Wrap user's query into a UserMessage and create a Prompt
        UserMessage userMessage = new UserMessage(userQuery.query());

        Prompt promptMessage = new Prompt(
                systemMessage,
                userMessage
        );

        // Log configured tool callbacks for visibility (useful for debugging)
        Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .forEach(tool -> {
                    System.out.println("Tool Name: " + tool.getToolDefinition().name());
                    System.out.println("Tool Description: " + tool.getToolDefinition().description());

                });

        // Execute the chat client call and return the content portion of the response
        var requestSpec = chatClient.prompt(promptMessage);
        var responseSpec = requestSpec.call();

        return responseSpec.content();
    }

}
