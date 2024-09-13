package com.example.advertisement.impression;

import com.example.advertisement.model.Impression;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImpressionRepository extends ReactiveMongoRepository<Impression, String> {

}
