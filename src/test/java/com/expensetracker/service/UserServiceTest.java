package com.expensetracker.service;

import com.expensetracker.dao.UserDAO;
import com.expensetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 *
 * <p>The {@link UserDAO} is mocked with Mockito so that these tests have no
 * database dependency and run quickly on every build.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests (Mocked DAO)")
class UserServiceTest {

    @Mock
    private UserDAO mockUserDAO;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(mockUserDAO);
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register succeeds with valid input")
    void testRegister_success() throws SQLException {
        when(mockUserDAO.existsByUsername("alice")).thenReturn(false);
        when(mockUserDAO.existsByEmail("alice@example.com")).thenReturn(false);
        when(mockUserDAO.save(any(User.class))).thenReturn(1);

        User user = userService.register("alice", "alice@example.com", "Password1");

        assertNotNull(user);
        assertEquals("alice", user.getUsername());
        assertEquals(1, user.getId());
        // Password should be stored as a BCrypt hash, not plain text
        assertNotEquals("Password1", user.getPassword());
    }

    @Test
    @DisplayName("register throws when username is taken")
    void testRegister_duplicateUsername() throws SQLException {
        when(mockUserDAO.existsByUsername("alice")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice", "alice@example.com", "Password1"));

        assertTrue(ex.getMessage().contains("already taken"));
    }

    @Test
    @DisplayName("register throws when email is taken")
    void testRegister_duplicateEmail() throws SQLException {
        when(mockUserDAO.existsByUsername("alice")).thenReturn(false);
        when(mockUserDAO.existsByEmail("alice@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice", "alice@example.com", "Password1"));

        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("register throws for invalid password")
    void testRegister_invalidPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice", "alice@example.com", "short"));
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login succeeds with correct credentials")
    void testLogin_success() throws SQLException {
        String hash = com.expensetracker.util.PasswordUtil.hashPassword("Password1");
        User storedUser = new User(1, "alice", "alice@example.com", hash, null);
        when(mockUserDAO.findByUsername("alice")).thenReturn(Optional.of(storedUser));

        User loggedIn = userService.login("alice", "Password1");

        assertNotNull(loggedIn);
        assertEquals("alice", loggedIn.getUsername());
    }

    @Test
    @DisplayName("login fails with wrong password")
    void testLogin_wrongPassword() throws SQLException {
        String hash = com.expensetracker.util.PasswordUtil.hashPassword("RightPass1");
        User storedUser = new User(1, "alice", "alice@example.com", hash, null);
        when(mockUserDAO.findByUsername("alice")).thenReturn(Optional.of(storedUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.login("alice", "WrongPass1"));

        assertTrue(ex.getMessage().contains("Invalid username or password"));
    }

    @Test
    @DisplayName("login fails when user not found")
    void testLogin_userNotFound() throws SQLException {
        when(mockUserDAO.findByUsername("unknown")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.login("unknown", "AnyPassword1"));

        assertTrue(ex.getMessage().contains("Invalid username or password"));
    }

    @Test
    @DisplayName("login throws for blank username")
    void testLogin_blankUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.login("", "Password1"));
    }
}
