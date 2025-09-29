package com.example.appointmentservice.business.client;


// PropertyServiceClient.javae;
import com.example.appointmentservice.domain.response.PropertyServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "property-service", url = "${app.services.property-service.url:http://localhost:8082}")
public interface PropertyServiceClient {

    @GetMapping("/api/v1/properties/{id}")
    PropertyServiceResponse getPropertyById(@PathVariable("id") Long id);
}

// Updated DTOs to match your actual services
// UserDto.java