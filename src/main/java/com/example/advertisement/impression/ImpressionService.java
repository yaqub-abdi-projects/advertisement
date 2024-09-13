package com.example.advertisement.impression;

import com.example.advertisement.model.Impression;
import com.example.advertisement.utils.ReactiveJsonReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ImpressionService {

    private final ReactiveJsonReader reader;
    private final ImpressionRepository repository;

    public Flux<Impression> load(String path) {
        return reader.readJsonFiles(path, Impression.class)
                .flatMap(repository::save);
    }
}
