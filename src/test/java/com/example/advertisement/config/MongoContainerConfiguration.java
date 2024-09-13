package com.example.advertisement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * This class provides the configuration for setting up a MongoDB container for testing purposes.
 * It is annotated with @TestConfiguration to indicate that it is a source of bean definitions.
 * The proxyBeanMethods attribute is set to false to optimize runtime bean creation.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoContainerConfiguration {

    @Value("${test-container.mongo.image}")
    private String mongoImage;

    @Value("${test-container.mongo.reuse}")
    private boolean reuse;

    /**
     * This method creates a MongoDBContainer bean for testing purposes.
     * It uses the DynamicPropertyRegistry to dynamically register properties for the MongoDB container.
     * The property includes the URI for the MongoDB container.
     *
     * @param registry The DynamicPropertyRegistry used to dynamically register properties for the MongoDB container.
     * @return The created MongoDBContainer bean.
     */
    @Bean
    @ServiceConnection
    public MongoDBContainer mongoDBContainer(DynamicPropertyRegistry registry) {
        MongoDBContainer container = new MongoDBContainer(DockerImageName.parse(mongoImage));
        container.withReuse(reuse);
        registry.add("spring.data.mongodb.uri", container::getReplicaSetUrl);
        return container;
    }
}