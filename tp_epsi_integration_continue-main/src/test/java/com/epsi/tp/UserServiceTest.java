package com.epsi.tp;

import org.junit.jupiter.api.Test;
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
}
