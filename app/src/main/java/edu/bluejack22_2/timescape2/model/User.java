package edu.bluejack22_2.timescape2.model;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private String phoneNumber;
    // Add this no-argument constructor
    public User() {
    }

    public User(String uid, String displayName, String email, String phoneNumber) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
