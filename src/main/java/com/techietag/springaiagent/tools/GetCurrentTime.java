package com.techietag.springaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * Utility tool that returns the current date/time for a given IANA time zone identifier.
 *
 * <p>This class is exposed as a Spring-managed service and declares a tool method used by
 * Spring AI tooling. The method expects a valid IANA time zone ID (for example
 * "Europe/London", "America/New_York", or "Asia/Kolkata") and returns the current
 * zoned date-time as a string. If an invalid or unknown time zone identifier is
 * provided, the method returns a user-friendly error message instead of throwing an exception.
 *
 * <p>Usage example:
 * <pre>
 * GetCurrentTime tool = applicationContext.getBean(GetCurrentTime.class);
 * String nowInLondon = tool.getCurrentTime("Europe/London");
 * </pre>
 * <p>
 * See: java.time.ZoneId for valid IANA zone identifiers and java.time.ZonedDateTime for
 * formatting options if you need a different output format.
 */
@Service
public class GetCurrentTime {

    /**
     * Returns the current date and time for the specified IANA time zone identifier.
     *
     * @param country the IANA time zone identifier (for example "Europe/London").
     *                This parameter is forwarded to java.time.ZoneId.of(...).
     * @return a String representation of the current zoned date-time in the given zone
     * (produced by ZonedDateTime.toString()), or a user-friendly error message
     * if the provided identifier is invalid.
     * <p>
     * Error behavior:
     * - If the provided zone id is invalid or not recognized, the method returns
     * the message: "Invalid country/region provided. Please provide a valid  time zone identifier.".
     * <p>
     * Notes:
     * - The method intentionally returns a String for simplicity when integrating
     * with tool frameworks; callers that need to manipulate the date-time should
     * parse the result into java.time.ZonedDateTime or modify this method to
     * return a ZonedDateTime object instead.
     */
    @Tool(
            description = "Get the current time for a specified country",
            name = "GetCurrentTimeTool")
    public String getCurrentTime(String country) {

        java.time.ZoneId zoneId;
        try {
            zoneId = java.time.ZoneId.of(country);
        } catch (Exception e) {
            return "Invalid country/region provided. Please provide a valid  time zone identifier.";
        }
        java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.now(zoneId);
        return zonedDateTime.toString();

    }
}
