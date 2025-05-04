package com.example.xytattoo;

import java.io.Serializable;

public class Appointment implements Serializable {
    private String id;
    private String userId;
    private String artistId;
    private String date;
    private String startTime;
    private String endTime;

    // Required empty constructor for Firestore
    public Appointment() {}

    public Appointment(String id, String userId, String artistId, String date,
                       String startTime, String endTime) {
        this.id = id;
        this.userId = userId;
        this.artistId = artistId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getArtistId() { return artistId; }
    public void setArtistId(String artistId) { this.artistId = artistId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}