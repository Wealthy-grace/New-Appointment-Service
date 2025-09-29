package com.example.appointmentservice.business.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import jakarta.annotation.PreDestroy;

@TestConfiguration
public class MongoTestContainerConfig {

    private static MongoDBContainer mongoDBContainer;

    static {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0.12"));
        mongoDBContainer.start();
    }

    @Bean(name = "testMongoClient")
    @Primary
    public MongoClient mongoClient() {
        String connectionString = mongoDBContainer.getReplicaSetUrl();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        return MongoClients.create(settings);
    }

    @Bean(name = "testMongoDatabaseFactory")
    @Primary
    @DependsOn("testMongoClient")
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory(mongoClient(), "test");
    }

    @Bean(name = "mongoTemplate")
    @Primary
    @DependsOn("testMongoDatabaseFactory")
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoDatabaseFactory());
    }

    @PreDestroy
    public void stopContainer() {
        if (mongoDBContainer != null && mongoDBContainer.isRunning()) {
            mongoDBContainer.stop();
        }
    }
}