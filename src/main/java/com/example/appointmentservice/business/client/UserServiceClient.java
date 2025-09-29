package com.example.appointmentservice.business.interfaces;


import com.example.appointmentservice.domain.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "user-service", url = "${app.services.user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    @GetMapping("/api/auth/user/{username}")
    UserDto getUserByUsername(@PathVariable("username") String username);

    // Since the user controller uses username, we'll need to get user by username
    // You may need to add a getUserById endpoint in your User Service if you want to fetch by ID
}