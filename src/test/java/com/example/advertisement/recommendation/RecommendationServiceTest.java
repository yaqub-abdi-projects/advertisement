package com.example.advertisement.recommendation;

import com.example.advertisement.AdvertisementApplication;
import com.example.advertisement.config.MongoContainerConfiguration;
import com.example.advertisement.model.Click;
import com.example.advertisement.model.Impression;
import com.example.advertisement.model.Recommendation;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

@SpringBootTest(classes = {AdvertisementApplication.class, MongoContainerConfiguration.class})
@ContextConfiguration
@Testcontainers
@CommonsLog
class RecommendationServiceTest {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Autowired
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        // Clean the collections before each test
        mongoTemplate.dropCollection(Impression.class).block();
        mongoTemplate.dropCollection(Click.class).block();
    }

    @Test
    void recommendations_shouldReturnTopAdvertisers() {
        // Insert sample impressions with identifiable information
        Flux<Impression> impressions = mongoTemplate.insertAll(Flux.just(
                new Impression("1", "app1", "US", "adv1"),  // ID: 1
                new Impression("2", "app1", "US", "adv2"),  // ID: 2
                new Impression("3", "app1", "US", "adv3"),  // ID: 3
                new Impression("4", "app1", "US", "adv11"), // ID: 4
                new Impression("5", "app1", "US", "adv16"), // ID: 5
                new Impression("6", "app2", "CA", "adv1"),  // ID: 6
                new Impression("7", "app2", "CA", "adv2"),  // ID: 7
                new Impression("8", "app3", "UK", "adv4"),  // ID: 8
                new Impression("9", "app3", "UK", "adv5"),  // ID: 9
                new Impression("10", "app4", "US", "adv1")  // ID: 10
        ).collectList());

        // Insert sample clicks with identifiable information
        Flux<Click> clicks = mongoTemplate.insertAll(Flux.just(
                new Click("1", "1", 10.0),  // Impression ID: 1
                new Click("2", "1", 15.0),  // Impression ID: 1
                new Click("3", "2", 20.0),  // Impression ID: 2
                new Click("4", "3", 25.0),  // Impression ID: 3
                new Click("5", "3", 30.0),  // Impression ID: 3
                new Click("6", "4", 35.0),  // Impression ID: 4
                new Click("7", "5", 40.0),  // Impression ID: 5
                new Click("8", "6", 45.0),  // Impression ID: 6
                new Click("9", "7", 50.0),  // Impression ID: 7
                new Click("10", "8", 55.0), // Impression ID: 8
                new Click("11", "9", 60.0), // Impression ID: 9
                new Click("12", "10", 65.0),// Impression ID: 10
                new Click("13", "11", 70.0),// Impression ID: 11
                new Click("14", "12", 75.0) // Impression ID: 12
        ).collectList());

        // Verify that all impressions and clicks are inserted correctly
        StepVerifier.create(impressions.thenMany(mongoTemplate.findAll(Impression.class)))
                .expectNextCount(10)
                .verifyComplete();

        StepVerifier.create(clicks.thenMany(mongoTemplate.findAll(Click.class)))
                .expectNextCount(14)
                .verifyComplete();

        // Execute the recommendation logic
        Flux<Recommendation> recommendationsFlux = recommendationService.recommendations(5);

        // Verify the recommendations by app and country code
        StepVerifier.create(recommendationsFlux)
                .expectNextMatches(this::assertRecommendation)
                .expectNextMatches(this::assertRecommendation)
                .expectNextMatches(this::assertRecommendation)
                .expectNextMatches(this::assertRecommendation)
                .verifyComplete();
    }

    private boolean assertRecommendation(Recommendation recommendation) {
        // Check for app1 in the US
        if (recommendation.getAppId().equals("app1") && recommendation.getCountryCode().equals("US")) {
            List<String> expectedAdvertiserIds = List.of("adv3", "adv2", "adv16", "adv11", "adv1");
            return recommendation.getRecommendedAdvertiserIds().containsAll(expectedAdvertiserIds);
        }
        // Check for app2 in CA
        else if (recommendation.getAppId().equals("app2") && recommendation.getCountryCode().equals("CA")) {
            List<String> expectedAdvertiserIds = List.of("adv1", "adv2");
            return recommendation.getRecommendedAdvertiserIds().containsAll(expectedAdvertiserIds);
        }
        // Check for app3 in the UK
        else if (recommendation.getAppId().equals("app3") && recommendation.getCountryCode().equals("UK")) {
            List<String> expectedAdvertiserIds = List.of("adv4", "adv5");
            return recommendation.getRecommendedAdvertiserIds().containsAll(expectedAdvertiserIds);
        }
        // Check for app3 in the UK
        else if (recommendation.getAppId().equals("app4") && recommendation.getCountryCode().equals("US")) {
            List<String> expectedAdvertiserIds = List.of("adv1");
            return recommendation.getRecommendedAdvertiserIds().containsAll(expectedAdvertiserIds);
        }
        return false;
    }
}
