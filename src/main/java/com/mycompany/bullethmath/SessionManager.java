
package com.mycompany.bullethmath;

/**
 * SessionManager.java
 * Menyimpan user yang sedang login secara global (static).
 * Memudahkan akses user dari form mana pun tanpa perlu passing objek terus.
 */
public class SessionManager {

    private static UserModel      currentUser = null;
    private static DatabaseManager db         = null;

    public static void setDb(DatabaseManager database) {
        db = database;
    }

    public static DatabaseManager getDb() {
        return db;
    }

    public static void login(UserModel user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static UserModel getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static int getUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    public static String getUsername() {
        return currentUser != null ? currentUser.getUsername() : "Guest";
    }
}