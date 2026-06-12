package com.epsi.tp;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @Test
    void testLoginAdmin() {
        UserService userService = new UserService();
        assertDoesNotThrow(() -> userService.login("admin", "admin"));
    }

    @Test
    void testComplexMethod() {
        UserService userService = new UserService();
        assertDoesNotThrow(() -> {
            userService.complexMethod(1, 1, 1);
            userService.complexMethod(1, 1, -1);
            userService.complexMethod(1, -1, 1);
            userService.complexMethod(1, -1, -1);
            userService.complexMethod(-1, 0, 0);
        });
    }

    @Test
    void testGetUserDetailsWithInvalidUrl() {
        // DB_URL env var is null → DriverManager throws → couvre le bloc catch
        UserService userService = new UserService();
        assertDoesNotThrow(() -> userService.getUserDetails("john_doe"));
    }

    @Test
    void testGetUserDetailsSuccess() throws Exception {
        String h2Url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS users (username VARCHAR(50))"
            );
            conn.createStatement().execute(
                "INSERT INTO users VALUES ('john_doe')"
            );
        }
        UserService service = new UserService(h2Url, "sa", "");
        assertDoesNotThrow(() -> service.getUserDetails("john_doe"));
    }

    @Test
    void testMain() {
        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }
}
