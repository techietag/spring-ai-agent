package com.techietag.springaiagent;

import com.techietag.springaiagent.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring AI Agent application.
 *
 * <p>This class boots the Spring application context. Run the `main` method to
 * start the embedded server and initialize all configured beans.
 *
 * Typical usage:
 * <pre>
 *   SpringAiAgentApplication.main(new String[]{});
 * </pre>
 */
@SpringBootApplication
public class SpringAiAgentApplication implements CommandLineRunner {

    Logger logger = LoggerFactory.getLogger(SpringAiAgentApplication.class);
    @Autowired
    IngestionService ingestionService;

    /**
     * Application main entry point which delegates to Spring Boot's
     * {@link SpringApplication#run(Class, String[])} to start the application.
     *
     * @param args command-line arguments forwarded to SpringApplication
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringAiAgentApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        // Application started
        logger.info("Spring AI Agent Application started successfully. Ingesting documents...");
        try {
            ingestionService.ingestPDF("");
            logger.info("Document ingestion completed.");
        }catch (Exception e){
            logger.error("Error during document ingestion: ", e);
        }

    }
}
