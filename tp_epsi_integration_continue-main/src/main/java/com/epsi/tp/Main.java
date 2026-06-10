package com.epsi.tp;

public class Main {
    public static void main(String[] args) {
        // Mauvaise pratique : System.out.println au lieu d'un vrai Logger
        System.out.println("Démarrage de l'application...");
        
        UserService userService = new UserService();
        userService.login("admin", "password123");
        userService.getUserDetails("john_doe");
    }
}
