package com.example.xytattoo;

import java.time.LocalTime;

public class TimeSlot {
    private LocalTime startTime;
    private LocalTime endTime;

    // No-arg constructor for serialization/deserialization
    public TimeSlot() {}

    public TimeSlot(LocalTime start, LocalTime end) {
        this.startTime = start;
        this.endTime = end;
    }

    // Getters
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    // Setters (if needed for deserialization)
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}