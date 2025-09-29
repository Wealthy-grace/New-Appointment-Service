package com.example.appointmentservice.exception;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(String message, String s) {
        super(message);
    }
}