//package com.example.appointmentservice.business.client;
//
//
//import com.example.appointmentservice.domain.dto.UserDto;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.stereotype.Component;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//
//@Component
//@FeignClient(name = "user-service", url = "${app.services.user-service.url:http://localhost:8081}")
//public interface UserServiceClient {
//
//
//    @GetMapping("/api/internal/users/username/{username}")
//    UserDto getUserByUsername(@PathVariable("username") String username);
//
//    @GetMapping("/api/internal/users/id/{id}")
//    UserDto getUserById(@PathVariable("id") Long id);
//
//
//
//    @GetMapping("/api/internal/users/username/{username}/exists")
//    Boolean userExistsByUsername(@PathVariable("username") String username);
//
//    @GetMapping("/api/internal/users/username/{username}/role")
//    String getUserRoleByUsername(@PathVariable("username") String username);
//
//}

// TODO: Implement UserServiceClient

package com.example.appointmentservice.business.client;

import com.example.appointmentservice.domain.dto.UserDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


@HttpExchange
public interface UserServiceClient {
    @GetExchange("/api/internal/users/username/{username}")
    UserDto getUserByUsername(@PathVariable("username") String username);

    @GetExchange("/api/internal/users/id/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    @GetExchange("/api/internal/users/username/{username}/exists")
    Boolean userExistsByUsername(@PathVariable("username") String username);

    @GetExchange("/api/internal/users/username/{username}/role")
    String getUserRoleByUsername(@PathVariable("username") String username);
}
