package com.example.appointmentservice;

import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;



@SpringBootTest
//@EnableFeignClients
@EnableMongoAuditing
class AppointmentServiceApplicationTests {

//    @JavaDispatcher.Container
//    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.0");

//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
//    }

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
    }

}
