package com.example.appointmentservice.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyDto {
    private Long id;
    private String title;
    private String address;
    private String propertyType;
    private String locationType;
    private BigDecimal rentAmount;
    private String description;

    private String surfaceArea;
    private Integer bedrooms;
    private Integer quantity;
    private String interior;
    private String condition;
    private String availableDate;
    private String postalCode;
    private String rentalcondition;
    private BigDecimal securityDeposit;
    private String image;
    private String image2;
    private String image3;
    private String image4;
}