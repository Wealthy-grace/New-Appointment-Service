package com.example.appointmentservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyResponse {
    private boolean success;
    private String message;
    private PropertyDto property;
    private List<PropertyDto> properties;
    private String errorCode;
    private LocalDateTime timestamp;
    private Integer totalCount;
    private Integer pageNumber;
    private Integer pageSize;
}
