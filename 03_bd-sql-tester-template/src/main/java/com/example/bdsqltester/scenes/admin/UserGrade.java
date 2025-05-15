package com.example.bdsqltester.scenes.admin;

public class UserGrade {
    private String username;
    private int grade;

    // Constructor
    public UserGrade(String username, int grade) {
        this.username = username;
        this.grade = grade;
    }

    // Getter untuk username
    public String getUsername() {
        return username;
    }

    // Getter untuk grade
    public int getGrade() {
        return grade;
    }

    // Setter jika diperlukan
    public void setUsername(String username) {
        this.username = username;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }
}