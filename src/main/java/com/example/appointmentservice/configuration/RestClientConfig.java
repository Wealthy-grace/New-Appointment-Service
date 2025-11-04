package com.example.appointmentservice.configuration;

import com.example.appointmentservice.business.client.PropertyServiceClient;
import com.example.appointmentservice.business.client.UserServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;



// Rest Client Configuration for HTTP Interfaces
@Configuration
public class RestClientConfig {

    @Value("${app.services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${app.services.property-service.url:http://localhost:8082}")
    private String propertyServiceUrl;


    // Create RestClient with JWT token forwarding
    private RestClient createRestClientWithAuth(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory())
                .requestInterceptor((request, body, execution) -> {
                    // Add JWT token from SecurityContext
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                        Jwt jwt = (Jwt) authentication.getPrincipal();
                        request.getHeaders().setBearerAuth(jwt.getTokenValue());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }


    // User Service Client Bean
    @Bean
    public UserServiceClient userServiceClient() {
        RestClient restClient = createRestClientWithAuth(userServiceUrl);
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(UserServiceClient.class);
    }

    @Bean
    public PropertyServiceClient propertyServiceClient() {
        RestClient restClient = createRestClientWithAuth(propertyServiceUrl);
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(PropertyServiceClient.class);
    }
}