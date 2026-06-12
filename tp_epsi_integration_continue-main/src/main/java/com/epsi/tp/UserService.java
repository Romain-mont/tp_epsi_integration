package com.epsi.tp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserService {

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    private String dbPassword = System.getenv("DB_PASSWORD");

    public void login(String username, String password) {
        LOGGER.log(Level.INFO, "Tentative de connexion de l''utilisateur : {0}", username);

        String adminUser = System.getenv("ADMIN_USERNAME");
        String adminPass = System.getenv("ADMIN_PASSWORD");

        if (username.equals(adminUser) && password.equals(adminPass)) {
            LOGGER.info("Administrateur connecté avec succès.");
        } else {
            LOGGER.warning("Identifiants invalides.");
        }
    }

    public void getUserDetails(String username) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?")) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LOGGER.log(Level.INFO, "Utilisateur trouvé : {0}", rs.getString("username"));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération de l''utilisateur : {0}", e.getMessage());
        }
    }

    public void complexMethod(int a, int b, int c) {
        if (a <= 0) {
            LOGGER.info("A est négatif");
            return;
        }
        if (b <= 0) {
            LOGGER.info(c > 0 ? "B est négatif" : "B et C sont négatifs");
            return;
        }
        LOGGER.info(c > 0 ? "Tous positifs" : "C est négatif");
    }
}
