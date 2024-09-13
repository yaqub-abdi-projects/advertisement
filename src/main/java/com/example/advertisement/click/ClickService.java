package com.example.advertisement.click;

import com.example.advertisement.model.Click;
import com.example.advertisement.utils.ReactiveJsonReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ClickService {
    private final ReactiveJsonReader reader;
    private final ClickRepository repository;

    public Flux<Click> load(String path) {
        return reader.readJsonFiles(path, Click.class)
                .flatMap(repository::save);
    }

}
