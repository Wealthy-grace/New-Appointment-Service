package com.example.appointmentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(excludeName = {
        "de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration"
})
//@EnableFeignClients
public class AppointmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}