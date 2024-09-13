package com.example.advertisement.utils;

import com.example.advertisement.AdvertisementApplication;
import com.example.advertisement.commons.TestModel;
import com.example.advertisement.config.MongoContainerConfiguration;
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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@SpringBootTest(classes = {AdvertisementApplication.class, MongoContainerConfiguration.class})
@ContextConfiguration
@Testcontainers
@CommonsLog
class ReactiveJsonReaderTest {

    @Autowired
    private ReactiveJsonReader reactiveJsonReader;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("testDir");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up temporary directory after each test
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directory
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void testReadJsonFiles_Success() throws IOException {
        // Prepare a sample JSON file with an array of objects
        String jsonContent = "[{\"name\":\"Test1\", \"value\":123}, {\"name\":\"Test2\", \"value\":456}]";
        Path jsonFile = tempDir.resolve("test.json");
        Files.write(jsonFile, jsonContent.getBytes());

        // Call the method
        Flux<TestModel> result = reactiveJsonReader.readJsonFiles(tempDir.resolve("*.json").toString(), TestModel.class);

        // Verify the results
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void testReadJsonFiles_FileNotFound() {
        // Call the method with a non-existing pattern
        Flux<TestModel> result = reactiveJsonReader.readJsonFiles(tempDir.resolve("nonexistent/*.json").toString(), TestModel.class);

        // Verify that an error is returned
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof NoSuchFileException)
                .verify();
    }

    @Test
    void testReadJsonFile_ErrorHandling() throws IOException {
        // Prepare a sample JSON file with invalid content
        Path jsonFile = tempDir.resolve("error.json");
        Files.write(jsonFile, "{\"invalidJson".getBytes()); // Invalid JSON

        // Call the method and expect an error
        Flux<TestModel> result = reactiveJsonReader.readJsonFiles(tempDir.resolve("error.json").toString(), TestModel.class);

        StepVerifier.create(result)
                .expectError(IOException.class) // Expect an IOException due to invalid JSON
                .verify();
    }


}
