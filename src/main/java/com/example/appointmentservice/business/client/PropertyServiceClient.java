package com.example.appointmentservice.business.client;

import com.example.appointmentservice.domain.response.PropertyServiceResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


@HttpExchange
public interface PropertyServiceClient {

    @GetExchange("/api/v1/properties/{id}")
    @CircuitBreaker(name = "propertyService", fallbackMethod = "getPropertyFallback")
    @Retry(name = "propertyService")
    PropertyServiceResponse getPropertyById(@PathVariable("id") Long id);
}