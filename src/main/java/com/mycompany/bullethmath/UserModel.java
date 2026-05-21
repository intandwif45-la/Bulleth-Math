package com.mycompany.bullethmath;
 
/**
 * UserModel.java
 * Merepresentasikan user yang sedang login (session object).
 */
public class UserModel {
    private int    id;
    private String username;
    private int    progressLevel;
 
    public UserModel(int id, String username) {
        this.id            = id;
        this.username      = username;
        this.progressLevel = 1;
    }

    public UserModel(int id, String username, int progressLevel) {
        this.id            = id;
        this.username      = username;
        this.progressLevel = progressLevel;
    }
 
    public int    getId()            { return id; }
    public String getUsername()      { return username; }
    public int    getProgressLevel() { return progressLevel; }
    public void   setProgressLevel(int level) { this.progressLevel = level; }
 
    @Override
    public String toString() {
        return "UserModel{id=" + id + ", username='" + username + "', progressLevel=" + progressLevel + "}";
    }
}