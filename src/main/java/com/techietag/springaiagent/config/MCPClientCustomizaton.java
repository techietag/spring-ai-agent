package com.techietag.springaiagent.config;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for customizing outgoing MCP (Model Context Protocol) HTTP requests.
 *
 * <p>This class registers a {@link McpSyncHttpClientRequestCustomizer} bean that can
 * modify outgoing HTTP requests produced by the MCP client. Currently it adds an
 * Authorization header for a configured GitHub MCP endpoint. The customization is
 * intentionally scoped to a single endpoint to avoid mutating unrelated requests.
 *
 * Notes:
 * - The configuration reads the MCP endpoint URL and API key from application
 *   properties (see the @Value annotations below).
 * - This class does not manage credentials rotation — if you need dynamic token
 *   refresh, extend the customizer to consult a credential provider at call time.
 */
@Configuration
public class MCPClientCustomizaton {

    private static final Logger log = LoggerFactory.getLogger(MCPClientCustomizaton.class);

    /**
     * The configured GitHub MCP endpoint URL (injected from application properties).
     * When an outgoing request's endpoint equals this value, an Authorization header
     * will be added.
     */
    @Value("${spring.ai.mcp.client.streamable-http.connections.github-mcp.url}")
    private String gitHubMcpUr;

    /**
     * API key (or token) associated with the GitHub MCP connection. Injected from
     * application properties and added to the Authorization header when appropriate.
     */
    @Value("${spring.ai.mcp.client.streamable-http.connections.github-mcp.api-key}")
    private String githubApiKey;

    /**
     * Bean that customizes MCP HTTP requests.
     *
     * <p>The returned customizer inspects the request endpoint and, if it exactly
     * matches the configured `gitHubMcpUr`, adds an Authorization header containing
     * the configured `githubApiKey`.
     *
     * @return a McpSyncHttpClientRequestCustomizer that can modify outgoing requests
     */
    @Bean
    public McpSyncHttpClientRequestCustomizer requestCustomizer() {
        return (builder, method, endpoint, body, context) -> {
            // Apply the header only for the configured GitHub MCP endpoint
            if (endpoint.toString().equals(gitHubMcpUr)) {
                log.info("Adding Authorization header for endpoint: {}", endpoint);
                builder.header("Authorization", " Bearer " + githubApiKey);
            }
        };
    }


}
