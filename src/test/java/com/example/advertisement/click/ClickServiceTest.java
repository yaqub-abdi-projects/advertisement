package com.example.advertisement.click;

import com.example.advertisement.AdvertisementApplication;
import com.example.advertisement.config.MongoContainerConfiguration;
import com.example.advertisement.model.Click;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(classes = {AdvertisementApplication.class, MongoContainerConfiguration.class})
@ContextConfiguration
@Testcontainers
@CommonsLog
class ClickServiceTest {

    @Autowired
    private ClickService clickService;

    @Autowired
    private ClickRepository repository;

    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("testDir");
        // Clean the repository before each test
        repository.deleteAll().block();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up the temporary directory after each test
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // reverse order, so files are deleted before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("Failed to delete temp file: " + path, e);
                    }
                });
    }

    @Test
    void testLoad_Success() throws Exception {
        // Prepare a sample JSON file with Click objects
        String jsonContent = "[{\"id\":\"1\", \"impression_id\":\"imp1\", \"revenue\":100.0}, {\"id\":\"2\", \"impression_id\":\"imp2\", \"revenue\":200.0}]";
        Path jsonFile = tempDir.resolve("clicks.json");
        Files.write(jsonFile, jsonContent.getBytes());

        // Call the load method
        Flux<Click> result = clickService.load(tempDir.resolve("*.json").toString());

        // Verify the results
        StepVerifier.create(result)
                .expectNextMatches(click -> click.getId().equals("1") && click.getRevenue() == 100.0)
                .expectNextMatches(click -> click.getId().equals("2") && click.getRevenue() == 200.0)
                .expectComplete()
                .verify();

        // Validate that the clicks are stored in the repository
        StepVerifier.create(repository.findAll())
                .expectNextMatches(click -> click.getId().equals("1") && click.getImpressionId().equals("imp1"))
                .expectNextMatches(click -> click.getId().equals("2") && click.getImpressionId().equals("imp2"))
                .verifyComplete();
    }

    @Test
    void testLoad_EmptyFile() throws Exception {
        // Create an empty JSON file
        Path jsonFile = tempDir.resolve("empty_clicks.json");
        Files.write(jsonFile, "".getBytes());

        // Call the load method
        Flux<Click> result = clickService.load(tempDir.resolve("*.json").toString());

        // Verify no clicks are loaded
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        // Validate that no clicks are stored in the repository
        StepVerifier.create(repository.findAll())
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testLoad_InvalidJson() throws Exception {
        // Create a file with invalid JSON content
        String invalidJsonContent = "{invalid json}";
        Path jsonFile = tempDir.resolve("invalid_clicks.json");
        Files.write(jsonFile, invalidJsonContent.getBytes());

        // Call the load method and expect an error
        Flux<Click> result = clickService.load(tempDir.resolve("*.json").toString());

        // Verify that an error is thrown
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IOException)
                .verify();
    }

    @Test
    void testLoad_NoFilesFound() throws Exception {
        // Call the load method with a path that doesn't match any files
        Flux<Click> result = clickService.load(tempDir.resolve("*.json").toString());

        // Verify that no clicks are loaded
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        // Validate that no clicks are stored in the repository
        StepVerifier.create(repository.findAll())
                .expectNextCount(0)
                .verifyComplete();
    }
}
