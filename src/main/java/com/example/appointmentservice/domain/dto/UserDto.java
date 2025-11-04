package com.example.appointmentservice.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private Long id;
    private String username;

    // These will be null when User Service sends fullName instead
    private String firstName;
    private String lastName;

    private String email;

    // Accept both "phoneNumber" and "telephone"
    @JsonAlias({"telephone"})
    private String phoneNumber;

    private String role;

    // Accept both "profileImage" and "image"
    @JsonAlias({"image"})
    private String profileImage;

    private String address;

    // This will capture fullName from User Service
    @JsonProperty("fullName")
    private String fullName;

    private String password;
    private String token;

    // Use @JsonIgnore to prevent Jackson from treating this as a property
    @JsonIgnore
    public String getFirstName() {
        if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName;
        }
        // Extract from fullName if firstName is null
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.split(" ", 2);
            return parts[0];
        }
        return username;
    }

    // Use @JsonIgnore to prevent Jackson from treating this as a property
    @JsonIgnore
    public String getLastName() {
        if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName;
        }
        // Extract from fullName if lastName is null
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.split(" ", 2);
            return parts.length > 1 ? parts[1] : "";
        }
        return "";
    }

    // Regular getter for fullName - Jackson will use this
    public String getFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        // Construct from firstName and lastName if fullName is not available
        if (firstName != null && lastName != null && !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        } else if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName;
        } else if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName;
        } else {
            return username != null ? username : "Unknown User";
        }
    }
}