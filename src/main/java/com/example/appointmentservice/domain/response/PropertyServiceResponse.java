package com.example.appointmentservice.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyServiceResponse {
    private Long propertyId;
    private String message;
    private boolean success;
    private String title;
    private String description;
    private String address;
    private String propertyType;
    private BigDecimal rentAmount;
    private String image;
    private String image2;
    private String image3;
    private String image4;
}