package com.example.appointmentservice.exception;

public class AppointmentConflictException extends Throwable {

    public AppointmentConflictException(String message) {
        super(message);
    }
}
