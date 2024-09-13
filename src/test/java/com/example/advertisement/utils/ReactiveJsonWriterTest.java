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
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AdvertisementApplication.class, MongoContainerConfiguration.class})
@ContextConfiguration
@Testcontainers
@CommonsLog
class ReactiveJsonWriterTest {

    @Autowired
    private ReactiveJsonWriter reactiveJsonWriter;


    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary file for testing
        tempFile = Files.createTempFile("test", ".json");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Delete the temporary file after each test
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testWriteJsonFile_Success() throws IOException {
        // Prepare a sample Flux of data
        Flux<TestModel> sampleDataFlux = Flux.just(new TestModel("1", 100), new TestModel("2", 200));

        // Write the data to the temporary file and verify completion
        StepVerifier.create(reactiveJsonWriter.writeJsonFile(sampleDataFlux, tempFile))
                .verifyComplete();

        // Read the content of the file and verify
        String fileContent = Files.readString(tempFile);
        assertThat(fileContent).isEqualToIgnoringWhitespace("""
                [
                  {"name":"1","value":100},
                  {"name":"2","value":200}
                ]
                """);
    }

}
