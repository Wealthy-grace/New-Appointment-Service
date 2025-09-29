package com.example.appointmentservice.business.client;


import com.example.appointmentservice.domain.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "user-service", url = "${app.services.user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    // Use internal unprotected endpoints
    @GetMapping("/api/internal/users/username/{username}")
    UserDto getUserByUsername(@PathVariable("username") String username);

    @GetMapping("/api/internal/users/id/{id}")
    UserDto getUserById(@PathVariable("id") Long id);



    // Additional helpful methods for internal calls
    @GetMapping("/api/internal/users/username/{username}/exists")
    Boolean userExistsByUsername(@PathVariable("username") String username);

    @GetMapping("/api/internal/users/username/{username}/role")
    String getUserRoleByUsername(@PathVariable("username") String username);
    // Since the user controller uses username, we'll need to get user by username
    // You may need to add a getUserById endpoint in your User Service if you want to fetch by ID
}