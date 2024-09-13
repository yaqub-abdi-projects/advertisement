package com.example.advertisement.metric;

import com.example.advertisement.model.Impression;
import com.example.advertisement.model.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class MetricsCalculatorService {
    private final ReactiveMongoTemplate mongoTemplate;

    /**
     * Calculates metrics based on impressions and clicks.
     *
     * @return A Flux of Metrics objects containing aggregated data.
     */
    public Flux<Metrics> metrics() {
        // Define the aggregation pipeline
        Aggregation aggregation = newAggregation(
                // Stage 1: Project fields to include in the aggregation
                project()
                        .and("_id").as("_id")
                        .and("appId").as("appId")
                        .and("countryCode").as("countryCode"),

                // Stage 2: Lookup operation to join "click" collection based on impression ID
                lookup("click", "_id", "impressionId", "clickData"),

                // Stage 3: Project additional fields including the size of the clickData array
                // and sum of revenue from clickData
                project()
                        .and("_id").as("_id")
                        .and("appId").as("appId")
                        .and("countryCode").as("countryCode")
                        .and(ArrayOperators.Size.lengthOfArray("clickData")).as("clicks") // Count number of clicks
                        .and(AccumulatorOperators.Sum.sumOf("clickData.revenue")).as("revenue"), // Sum of revenue

                // Stage 4: Group by appId and countryCode, and calculate totals
                group("appId", "countryCode")
                        .count().as("impressions") // Total number of impressions
                        .sum("clicks").as("clicks") // Total number of clicks
                        .sum("revenue").as("revenue"), // Total revenue

                // Stage 5: Final projection to format the output
                project()
                        .and("_id.appId").as("appId")
                        .and("_id.countryCode").as("countryCode")
                        .and("impressions").as("impressions")
                        .and("clicks").as("clicks")
                        .and("revenue").as("revenue")
        );

        // Execute the aggregation and return the results
        return mongoTemplate.aggregate(aggregation, Impression.class, Metrics.class);
    }
}
