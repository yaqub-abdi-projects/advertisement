package com.example.advertisement.click;

import com.example.advertisement.model.Click;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickRepository extends ReactiveMongoRepository<Click, String> {

}
