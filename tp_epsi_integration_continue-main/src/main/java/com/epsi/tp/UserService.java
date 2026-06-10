package com.epsi.tp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
// Mauvaise pratique : import inutilisé
import java.util.List;
import java.util.ArrayList;

public class UserService {

    // Faille de sécurité majeure : mot de passe en dur
    private String DB_PASSWORD = "super_secret_password_123!";
    
    // Mauvaise pratique : variable inutilisée
    private int unusedCounter = 0;

    public void login(String username, String password) {
        // Mauvaise pratique : variable locale inutilisée
        boolean isLoggedIn = false;
        
        // Mauvaise pratique : System.out.println au lieu d'un Logger
        System.out.println("Tentative de connexion de l'utilisateur : " + username);

        // Mauvaise pratique : identifiants en dur dans le code
        if (username.equals("admin") && password.equals("admin")) {
            System.out.println("Administrateur connecté avec succès.");
            isLoggedIn = true;
        } else {
            System.out.println("Identifiants invalides.");
        }
        
        try {
            // Logique factice pour déclencher une exception
            int result = 10 / 0;
        } catch (Exception e) {
            // Mauvaise pratique : bloc catch vide (l'erreur est ignorée silencieusement)
        }
    }

    public void getUserDetails(String username) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Faille : mot de passe en dur utilisé ici
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "root", DB_PASSWORD);
            stmt = conn.createStatement();
            
            // Faille de sécurité majeure : Injection SQL possible via concaténation
            String query = "SELECT * FROM users WHERE username = '" + username + "'";
            rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                System.out.println("Utilisateur trouvé : " + rs.getString("username"));
            }
        } catch (Exception e) {
            // Mauvaise pratique : on attrape une exception générique et on print la stacktrace sans logger
            e.printStackTrace();
        } finally {
            // Mauvaise pratique : gestion archaïque des ressources (pas de try-with-resources)
            // avec des catch vides
            if (rs != null) {
                try { rs.close(); } catch (Exception e) {}
            }
            if (stmt != null) {
                try { stmt.close(); } catch (Exception e) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (Exception e) {}
            }
        }
    }
    
    // Mauvaise pratique : méthode inutilement complexe avec de nombreux "if" imbriqués (complexité cyclomatique élevée)
    public void complexMethod(int a, int b, int c) {
        if (a > 0) {
            if (b > 0) {
                if (c > 0) {
                    System.out.println("Tous positifs");
                } else {
                    System.out.println("C est négatif");
                }
            } else {
                if (c > 0) {
                    System.out.println("B est négatif");
                } else {
                    System.out.println("B et C sont négatifs");
                }
            }
        } else {
            System.out.println("A est négatif");
        }
    }
}
