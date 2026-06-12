package com.epsi.tp;

import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        LOGGER.info("Démarrage de l'application...");

        UserService userService = new UserService();
        userService.login("admin", "password123");
        userService.getUserDetails("john_doe");
    }
}
