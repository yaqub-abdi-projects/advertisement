package com.example.advertisement.impression;

import com.example.advertisement.AdvertisementApplication;
import com.example.advertisement.config.MongoContainerConfiguration;
import com.example.advertisement.model.Impression;
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
class ImpressionServiceTest {

    @Autowired
    private ImpressionService impressionService;

    @Autowired
    private ImpressionRepository repository;

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
        // Prepare a sample JSON file with Impression objects
        String jsonContent = "[{\"id\":\"1\", \"app_id\":\"app1\", \"country_code\":\"US\", \"advertiser_id\":\"adv1\"}, " +
                "{\"id\":\"2\", \"app_id\":\"app2\", \"country_code\":\"CA\", \"advertiser_id\":\"adv2\"}]";
        Path jsonFile = tempDir.resolve("impressions.json");
        Files.write(jsonFile, jsonContent.getBytes());

        // Call the load method
        Flux<Impression> result = impressionService.load(tempDir.resolve("*.json").toString());

        // Verify the results
        StepVerifier.create(result)
                .expectNextMatches(impression -> impression.getId().equals("1") && impression.getAppId().equals("app1"))
                .expectNextMatches(impression -> impression.getId().equals("2") && impression.getAppId().equals("app2"))
                .expectComplete()
                .verify();

        // Validate that the impressions are stored in the repository
        StepVerifier.create(repository.findAll())
                .expectNextMatches(impression -> impression.getId().equals("1") && impression.getCountryCode().equals("US"))
                .expectNextMatches(impression -> impression.getId().equals("2") && impression.getCountryCode().equals("CA"))
                .verifyComplete();
    }

    @Test
    void testLoad_EmptyFile() throws Exception {
        // Create an empty JSON file
        Path jsonFile = tempDir.resolve("empty_impressions.json");
        Files.write(jsonFile, "".getBytes());

        // Call the load method
        Flux<Impression> result = impressionService.load(tempDir.resolve("*.json").toString());

        // Verify no impressions are loaded
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        // Validate that no impressions are stored in the repository
        StepVerifier.create(repository.findAll())
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testLoad_InvalidJson() throws Exception {
        // Create a file with invalid JSON content
        String invalidJsonContent = "{invalid json}";
        Path jsonFile = tempDir.resolve("invalid_impressions.json");
        Files.write(jsonFile, invalidJsonContent.getBytes());

        // Call the load method and expect an error
        Flux<Impression> result = impressionService.load(tempDir.resolve("*.json").toString());

        // Verify that an error is thrown
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IOException)
                .verify();
    }

    @Test
    void testLoad_NoFilesFound() {
        // Call the load method with a path that doesn't match any files
        Flux<Impression> result = impressionService.load(tempDir.resolve("*.json").toString());

        // Verify that no impressions are loaded
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        // Validate that no impressions are stored in the repository
        StepVerifier.create(repository.findAll())
                .expectNextCount(0)
                .verifyComplete();
    }
}
