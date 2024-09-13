package com.example.advertisement.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ReactiveJsonReader {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveJsonReader.class);
    private final ObjectMapper objectMapper;

    /**
     * Reads JSON files that match the specified path pattern and converts them into Flux of objects.
     *
     * @param pathPattern The path pattern to match files, e.g., "/x/y/*.json".
     * @param clazz       The class type to convert JSON data into.
     * @param <T>         The type of the objects to be created from JSON.
     * @return A Flux of objects of type T.
     */
    public <T extends Serializable> Flux<T> readJsonFiles(String pathPattern, Class<T> clazz) {
        // Determine the root path and pattern for file matching
        Path rootPath = getRootPath(pathPattern);

        logger.info("Root Path: {}", rootPath);

        // Create a PathMatcher to match files against the specified pattern
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pathPattern);

        return Flux.using(
                // Open a stream of files in the root path
                () -> Files.walk(rootPath),
                stream -> Flux.fromStream(stream)
                        .filter(Files::isRegularFile) // Filter to include only regular files
                        .filter(matcher::matches) // Filter files based on the path pattern
                        .flatMap(file -> readJsonFile(file.toFile(), clazz)), // Read each file as JSON
                Stream::close
        ).doOnError(error -> logger.error("Error processing files in path: {}", pathPattern, error)); // Log any errors
    }

    /**
     * Determines the root path from the given path pattern.
     *
     * @param pathPattern The path pattern to extract the root path from.
     * @return The root path for the given pattern.
     */
    private Path getRootPath(String pathPattern) {
        int wildcardIndex = Math.min(
                pathPattern.indexOf('*'),
                pathPattern.indexOf('?')
        );

        // If no wildcard is present, return the parent of the path
        if (wildcardIndex == -1) {
            return Paths.get(pathPattern).getParent();
        } else {
            // Extract the root path from the pattern
            return Paths.get(pathPattern.substring(0, wildcardIndex)).getParent();
        }
    }

    /**
     * Reads a JSON file and converts it into a Flux of objects of the specified type.
     *
     * @param file  The file to read.
     * @param clazz The class type to convert JSON data into.
     * @param <T>   The type of the objects to be created from JSON.
     * @return A Flux of objects of type T.
     */
    private <T extends Serializable> Flux<T> readJsonFile(File file, Class<T> clazz) {
        return Flux.create(sink -> {
            // Skip empty files
            if (file.length() == 0) {
                logger.info("Skipping empty file: {}", file);
                sink.complete();
                return;
            }

            try (JsonParser jsonParser = new JsonFactory().createParser(file)) {
                logger.info("Reading and processing file: {}", file);

                // Parse JSON array from the file
                if (jsonParser.nextToken() == JsonToken.START_ARRAY) {
                    while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                        T obj = objectMapper.readValue(jsonParser, clazz);
                        sink.next(obj);
                    }
                } else {
                    sink.error(new IOException("Expected start of array token"));
                }

                sink.complete();
            } catch (IOException e) {
                logger.error("Error reading file: {}", file, e);
                sink.error(e);
            }
        });
    }
}
