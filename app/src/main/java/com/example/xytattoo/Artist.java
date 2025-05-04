package com.example.xytattoo;

public class Artist {
    private String id;
    private String name;
    private String specialty;
    private String photoUrl;

    // No-arg constructor required for Firestore
    public Artist() {}

    public Artist(String id, String name, String specialty, String photoUrl) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.photoUrl = photoUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}