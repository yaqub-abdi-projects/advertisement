package com.example.advertisement.metric;

import com.example.advertisement.AdvertisementApplication;
import com.example.advertisement.config.MongoContainerConfiguration;
import com.example.advertisement.model.Click;
import com.example.advertisement.model.Impression;
import com.example.advertisement.model.Metrics;
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

@SpringBootTest(classes = {AdvertisementApplication.class, MongoContainerConfiguration.class})
@ContextConfiguration
@Testcontainers
@CommonsLog
class MetricsCalculatorServiceTest {

    @Autowired
    private MetricsCalculatorService metricsCalculatorService;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        // Clean the collections before each test
        mongoTemplate.dropCollection(Impression.class).block();
        mongoTemplate.dropCollection(Click.class).block();
    }

    @Test
    void testMetricsCalculationWithMultipleScenarios() {
        // Insert sample impressions
        Flux<Impression> impressions = mongoTemplate.insertAll(Flux.just(
                new Impression("1", "app1", "US", "adv1"),
                new Impression("2", "app1", "US", "adv2"),
                new Impression("3", "app2", "CA", "adv1"),
                new Impression("4", "app1", "US", "adv3"),
                new Impression("5", "app2", "CA", "adv1"),
                new Impression("6", "app3", "UK", "adv4"),
                new Impression("7", "app3", "UK", "adv5")
        ).collectList());

        // Insert sample clicks
        Flux<Click> clicks = mongoTemplate.insertAll(Flux.just(
                new Click("1", "1", 50.0),
                new Click("2", "1", 75.0),
                new Click("3", "3", 100.0),
                new Click("4", "4", 25.0),
                new Click("5", "4", 30.0),
                new Click("6", "5", 40.0),
                new Click("7", "6", 60.0)
        ).collectList());

        // Wait for both insertions to complete
        StepVerifier.create(impressions.thenMany(mongoTemplate.findAll(Impression.class)))
                .expectNextCount(7)
                .verifyComplete();

        StepVerifier.create(clicks.thenMany(mongoTemplate.findAll(Click.class)))
                .expectNextCount(7)
                .verifyComplete();

        // Call the metrics method and verify results
        Flux<Metrics> result = metricsCalculatorService.metrics();

        StepVerifier.create(result)
                .expectNextMatches(this::assertMetrics)
                .expectNextMatches(this::assertMetrics)
                .expectNextMatches(this::assertMetrics)
                .expectComplete()
                .verify();
    }

    private boolean assertMetrics(Metrics metrics) {
        return (metrics.getAppId().equals("app1")
                && metrics.getCountryCode().equals("US")
                && metrics.getImpressions() == 3
                && metrics.getClicks() == 4
                && metrics.getRevenue() == 180.0)
                ||
                (metrics.getAppId().equals("app2")
                        && metrics.getCountryCode().equals("CA")
                        && metrics.getImpressions() == 2
                        && metrics.getClicks() == 2
                        && metrics.getRevenue() == 140.0)
                ||
                (metrics.getAppId().equals("app3")
                        && metrics.getCountryCode().equals("UK")
                        && metrics.getImpressions() == 2
                        && metrics.getClicks() == 1
                        && metrics.getRevenue() == 60.0);
    }

    @Test
    void testMetricsWithNoClicks() {
        // Insert impressions without any clicks
        Flux<Impression> impressions = mongoTemplate.insertAll(Flux.just(
                new Impression("1", "app1", "US", "adv1"),
                new Impression("2", "app2", "CA", "adv2")
        ).collectList());

        // Wait for insertions to complete
        StepVerifier.create(impressions.thenMany(mongoTemplate.findAll(Impression.class)))
                .expectNextCount(2)
                .verifyComplete();

        // Call the metrics method and verify results
        Flux<Metrics> result = metricsCalculatorService.metrics();

        StepVerifier.create(result)
                .expectNextMatches(this::assertMetricsWithNoClicks)
                .expectNextMatches(this::assertMetricsWithNoClicks)
                .expectComplete()
                .verify();
    }

    private boolean assertMetricsWithNoClicks(Metrics metrics) {
        return (metrics.getAppId().equals("app1")
                && metrics.getCountryCode().equals("US")
                && metrics.getImpressions() == 1
                && metrics.getClicks() == 0
                && metrics.getRevenue() == 0.0)
                ||
                (metrics.getAppId().equals("app2")
                        && metrics.getCountryCode().equals("CA")
                        && metrics.getImpressions() == 1
                        && metrics.getClicks() == 0
                        && metrics.getRevenue() == 0.0);
    }
}
