package com.example.advertisement.recommendation;

import com.example.advertisement.model.Impression;
import com.example.advertisement.model.Recommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final ReactiveMongoTemplate mongoTemplate;

    /**
     * Generates a list of recommended advertisers based on the click data.
     *
     * @param numberAdvertisers The number of top recommended advertisers to return.
     * @return A Flux of Recommendation objects containing recommended advertiser IDs.
     */
    public Flux<Recommendation> recommendations(int numberAdvertisers) {
        // Define the aggregation pipeline
        Aggregation aggregation = newAggregation(
                // Stage 1: Perform a lookup to join the "click" collection based on the impression ID
                lookup("click", "_id", "impressionId", "clickData"),

                // Stage 2: Project fields and compute the sum of revenue from the clickData
                project()
                        .and("_id").as("_id")
                        .and("appId").as("appId")
                        .and("countryCode").as("countryCode")
                        .and("advertiserId").as("advertiserId")
                        .and(AccumulatorOperators.Sum.sumOf("clickData.revenue")).as("revenue"), // Calculate total revenue

                // Stage 3: Sort the documents by revenue in descending order
                sort(Sort.by(Sort.Order.desc("revenue"))),

                // Stage 4: Group by appId and countryCode, and collect unique advertiser IDs into a set
                group("appId", "countryCode")
                        .addToSet("advertiserId").as("recommendedAdvertiserIds"),

                // Stage 5: Project the output and limit the number of recommended advertiser IDs to the specified number
                project()
                        .and("_id.appId").as("appId")
                        .and("_id.countryCode").as("countryCode")
                        .and("recommendedAdvertiserIds").slice(numberAdvertisers).as("recommendedAdvertiserIds") // Limit the number of IDs
        );

        // Execute the aggregation pipeline and return the resulting Flux of Recommendation objects
        return mongoTemplate.aggregate(aggregation, Impression.class, Recommendation.class);
    }
}
