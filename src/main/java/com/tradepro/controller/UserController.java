package com.tradepro.controller;

import com.tradepro.exception.CustomException;
import com.tradepro.model.User;
import com.tradepro.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String email = userService.getEmailFromToken(token.replace("Bearer ", ""));
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("User not found"));
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error fetching current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Error fetching user details"));
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest) {
        // Validate email format
        if (!isValidEmail(registrationRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid email format."));
        }

        // Check if email already exists
        if (userService.emailExists(registrationRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is already registered. Please use a different email."));
        }

        // Validate password requirements
        if (!isValidPassword(registrationRequest.getPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Password must be at least 6 characters long."));
        }

        User user = userService.registerUser(registrationRequest.getEmail(), registrationRequest.getPassword());
        return ResponseEntity.ok(new RegistrationResponse(user.getId(), "User registered successfully"));
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return email.matches(emailRegex);
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6; // Example: Password must be at least 6 characters long
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());
            String userId = userService.getUserIdByEmail(loginRequest.getEmail());
            return ResponseEntity.ok(new LoginResponse(token, userId, null));
        } catch (CustomException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(null, null, e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        logger.info("Password change request received");
        try {
            // Validate request
            if (request.getCurrentPassword() == null || request.getNewPassword() == null) {
                logger.error("Invalid request: missing required fields");
                return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Both current and new passwords are required."));
            }

            logger.debug("Validating password change request...");
            
            userService.changePassword(request.getCurrentPassword(), request.getNewPassword());
            
            logger.info("Password changed successfully");
            return ResponseEntity.ok(new MessageResponse("Your password has been updated successfully."));
        } catch (CustomException e) {
            logger.error("Password change failed: {}", e.getMessage());
            String userFriendlyMessage = getUserFriendlyErrorMessage(e.getMessage());
            logger.debug("Sending user-friendly error message: {}", userFriendlyMessage);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(userFriendlyMessage));
        } catch (Exception e) {
            logger.error("Unexpected error during password change", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("An unexpected error occurred while changing your password. Please try again later."));
        }
    }

    private String getUserFriendlyErrorMessage(String errorMessage) {
        switch (errorMessage) {
            case "Current password is incorrect":
                return "The current password you entered is incorrect. Please try again.";
            case "User not found":
                return "Unable to update password. Please log in again.";
            case "User not authenticated":
                return "Your session has expired. Please log in again to change your password.";
            default:
                if (errorMessage.contains("Password must be")) {
                    return "Your new password doesn't meet the requirements. Please make sure it includes at least 8 characters, one uppercase letter, one number, and one special character.";
                }
                return "An error occurred while changing your password. Please ensure your current password is correct and that your new password meets all requirements.";
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody String emailJson) {
        // Remove quotes and any JSON formatting
        String email = emailJson.replace("\"", "").trim();
        logger.info("Received password reset request for email: {}", email);
        try {
            // Check if the user exists
            User user = userService.findByEmail(email);
            if (user == null) {
                logger.warn("User not found for email: {}", email);
                return ResponseEntity.badRequest().body("User not found");
            }

            // Proceed with sending the password reset email
            userService.sendPasswordResetEmail(email);
            logger.info("Password reset email sent to: {}", email);
            return ResponseEntity.ok("Password reset email sent");
        } catch (CustomException e) {
            logger.error("Error sending password reset email: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> testEmail() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("support@tradalyst.com");
            message.setTo("support@tradalyst.com");
            message.setSubject("Test Email");
            message.setText("This is a test email from your application.");
            mailSender.send(message);
            return ResponseEntity.ok("Test email sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send test email: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to send test email: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        logger.info("Received password reset request with token");
        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            logger.info("Password reset successful");
            return ResponseEntity.ok("Password has been reset successfully");
        } catch (CustomException e) {
            logger.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

class RegistrationRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class LoginRequest {
    private String email;
    private String password;

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class RegistrationResponse {
    private String userId;
    private String message;

    public RegistrationResponse(String userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
}

class LoginResponse {
    private String token;
    private String userId;
    private String error;

    public LoginResponse(String token, String userId, String error) {
        this.token = token;
        this.userId = userId;
        this.error = error;
    }

    // Getters
    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public String getError() { return error; }
}

class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;

    // Getters and setters
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}

class MessageResponse {
    private String message;

    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}

class ResetPasswordRequest {
    private String token;
    private String newPassword;

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
