package com.techietag.springaiagent.dto;

/**
 * Data transfer object representing a user-provided query.
 *
 * <p>Used as the request payload for controller endpoints that accept a natural
 * language query from the client.
 *
 * Example JSON: { "query": "What is current time in India?" }
 */
public record UserQuery(
        String query
) {
}
