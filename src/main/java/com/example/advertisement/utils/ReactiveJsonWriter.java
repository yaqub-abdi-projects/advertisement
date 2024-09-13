package com.example.advertisement.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ReactiveJsonWriter {
    private final ObjectMapper objectMapper;

    /**
     * Writes a Flux of objects to a JSON file.
     *
     * @param flux       The Flux of objects to write to the file.
     * @param pathString The path of the file as a string.
     * @param <T>        The type of the objects to write.
     * @return A Flux that completes when the file write operation is finished.
     */
    public <T> Flux<Void> writeJsonFile(Flux<T> flux, String pathString) {
        Path path = Paths.get(pathString); // Convert the string path to a Path object
        return writeJsonFile(flux, path);
    }

    /**
     * Writes a Flux of objects to a JSON file.
     *
     * @param flux The Flux of objects to write to the file.
     * @param path The Path of the file.
     * @param <T>  The type of the objects to write.
     * @return A Flux that completes when the file write operation is finished.
     */
    public <T> Flux<Void> writeJsonFile(Flux<T> flux, Path path) {
        return Flux.create(sink -> {
            BufferedWriter writer;
            try {
                // Create a BufferedWriter to write to the file
                writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                writer.write("[\n"); // Start the JSON array

                AtomicBoolean firstItem = new AtomicBoolean(true);

                flux
                        .publishOn(Schedulers.boundedElastic()) // Use a bounded elastic scheduler for IO operations
                        .doOnNext(item -> {
                            try {
                                // Convert the object to JSON and write it to the file
                                String json = objectMapper.writeValueAsString(item);
                                if (firstItem.get()) {
                                    firstItem.set(false);
                                } else {
                                    writer.write(",\n"); // Add a comma before subsequent items
                                }
                                writer.write(json);
                            } catch (IOException e) {
                                // Handle errors during writing
                                sink.error(new RuntimeException("Failed to write JSON to file", e));
                            }
                        })
                        .doFinally(signalType -> {
                            try {
                                writer.write("\n]"); // Close the JSON array
                                writer.flush();
                                writer.close(); // Close the BufferedWriter
                                sink.complete(); // Complete the Flux
                            } catch (IOException e) {
                                // Handle errors during flushing or closing
                                sink.error(new RuntimeException("Failed to flush or close writer", e));
                            }
                        })
                        .subscribe(); // Subscribe to start processing the Flux
            } catch (IOException e) {
                // Handle errors during BufferedWriter creation
                sink.error(new RuntimeException("Failed to create BufferedWriter", e));
            }
        });
    }
}
