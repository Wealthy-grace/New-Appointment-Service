package com.example.appointmentservice.domain.response;

import com.example.appointmentservice.domain.dto.PropertyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyResponse {
    private boolean success;
    private String message;
    private PropertyDto property;
    private String errorCode;
    private String timestamp;

    public static PropertyResponse success(String message, PropertyDto property) {
        return PropertyResponse.builder()
                .success(true)
                .message(message)
                .property(property)
                .build();
    }

    public static PropertyResponse error(String message, String errorCode) {
        return PropertyResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}