package com.tradepro.service;

import com.tradepro.exception.CustomException;
import com.tradepro.model.User;
import com.tradepro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.regex.Pattern;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{8,}$");
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final JavaMailSender mailSender;
    private static final long PASSWORD_RESET_TOKEN_EXPIRY = 3600000; // 1 hour in milliseconds
    private final Map<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public User registerUser(String email, String password) {
        if (userRepository.findByEmail(email) != null) {
            throw new CustomException("Email already exists");
        }

        if (!isValidPassword(password)) {
            throw new CustomException("Password must be at least 8 characters long and contain at least one digit, one lowercase letter, one uppercase letter, and one special character");
        }

        User newUser = new User(email, passwordEncoder.encode(password));
        return userRepository.save(newUser);
    }

    public String loginUser(String email, String password) {
        User user = userRepository.findByEmail(email);
        
        if (user == null) {
            throw new CustomException("No account found with this email address");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException("Incorrect password");
        }
        
        return generateToken(user);
    }

    private boolean isValidPassword(String password) {
        boolean isValid = PASSWORD_PATTERN.matcher(password).matches();
        if (!isValid) {
            logger.debug("Password validation failed. Password does not meet requirements");
        }
        return isValid;
    }

    private String generateToken(User user) {
        long expirationTime = 1000 * 60 * 60 * 24; // 24 hours
        return Jwts.builder()
                .setSubject(user.getId()) // Use ID instead of email
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    public String getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new CustomException("User not found");
        }
        return user.getId();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public void changePassword(String currentPassword, String newPassword) {
        logger.info("Processing password change request");
        
        // Get current user's email
        String userEmail;
        try {
            userEmail = getCurrentUserEmail();
            logger.debug("Retrieved user email for password change");
        } catch (CustomException e) {
            logger.error("Failed to get current user email: {}", e.getMessage());
            throw e;
        }

        // Find user
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            logger.error("User not found for email: {}", userEmail);
            throw new CustomException("User not found");
        }
        logger.debug("Found user account for password change");

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            logger.warn("Invalid current password provided for user: {}", userEmail);
            throw new CustomException("The current password you entered is incorrect. Please try again.");
        }
        logger.debug("Current password verified successfully");

        // Validate new password
        if (!isValidPassword(newPassword)) {
            logger.warn("New password validation failed for user: {}", userEmail);
            throw new CustomException("Your new password must be at least 8 characters long and contain at least one digit, one lowercase letter, one uppercase letter, and one special character.");
        }
        logger.debug("New password meets all requirements");

        try {
            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            logger.info("Password successfully updated for user: {}", userEmail);
        } catch (Exception e) {
            logger.error("Failed to save new password for user: {}", userEmail, e);
            throw new CustomException("Please ensure your current password is correct and that your new password meets all requirements.");
        }
    }

    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.debug("Current principal: {}", principal);
        
        if (principal instanceof String) {
            return (String) principal;
        }
        logger.error("User not authenticated properly. Principal type: {}", 
            principal != null ? principal.getClass().getName() : "null");
        throw new CustomException("User not authenticated");
    }

    public String getEmailById(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException("User not found"));
        return user.getEmail();
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email) != null;
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String userId = claims.getSubject();
            logger.debug("Extracted user ID from token: {}", userId);
            
            // Get email from userId
            return getEmailById(userId);
        } catch (Exception e) {
            logger.error("Failed to parse JWT token: {}", e.getMessage());
            return null;
        }
    }

    public void sendPasswordResetEmail(String email) {
        logger.info("Preparing to send password reset email to: {}", email);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            logger.warn("Attempted to send password reset email, but user not found: {}", email);
            throw new CustomException("User not found");
        }

        String resetToken = UUID.randomUUID().toString();
        resetTokens.put(resetToken, new PasswordResetToken(email));
        logger.debug("Generated reset token for user: {}", email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("support@tradalyst.com");
        message.setTo(email);
        message.setSubject("Password Reset Request");
        message.setText("Hello,\n\n" +
                "You have requested to reset your password. Click the link below to reset it:\n\n" +
                "http://localhost:3000/auth/reset-password?token=" + resetToken + "\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Tradalyst Team");

        try {
            mailSender.send(message);
            logger.info("Password reset email successfully sent to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", email, e.getMessage());
            throw new CustomException("Failed to send password reset email");
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void resetPassword(String token, String newPassword) {
        logger.info("Processing password reset request");
        
        PasswordResetToken resetToken = resetTokens.get(token);
        if (resetToken == null) {
            logger.warn("Invalid reset token provided");
            throw new CustomException("Invalid or expired reset token");
        }

        if (resetToken.isExpired()) {
            resetTokens.remove(token);
            logger.warn("Expired reset token provided");
            throw new CustomException("Reset token has expired");
        }

        User user = userRepository.findByEmail(resetToken.email);
        if (user == null) {
            logger.error("User not found for reset token");
            throw new CustomException("User not found");
        }

        // Validate new password
        if (!isValidPassword(newPassword)) {
            logger.warn("Invalid new password format");
            throw new CustomException("Password must be at least 8 characters long and contain at least one digit, one lowercase letter, one uppercase letter, and one special character");
        }

        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            resetTokens.remove(token); // Remove used token
            logger.info("Password successfully reset for user: {}", resetToken.email);
        } catch (Exception e) {
            logger.error("Failed to save new password: {}", e.getMessage());
            throw new CustomException("Failed to reset password");
        }
    }

    public void sendContactFormEmail(String fromEmail, String message) {
        logger.info("Sending contact form email from: {}", fromEmail);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom("support@tradalyst.com");
        mailMessage.setTo("support@tradalyst.com");
        mailMessage.setReplyTo(fromEmail);
        mailMessage.setSubject("New Contact Form Submission");
        mailMessage.setText(String.format("""
            New contact form submission:
            
            From: %s
            Message:
            %s
            """, fromEmail, message));

        try {
            mailSender.send(mailMessage);
            logger.info("Contact form email sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send contact form email: {}", e.getMessage());
            throw new CustomException("Failed to send message. Please try again later.");
        }
    }

    private static class PasswordResetToken {
        private final String email;
        private final long expiryTime;

        public PasswordResetToken(String email) {
            this.email = email;
            this.expiryTime = System.currentTimeMillis() + PASSWORD_RESET_TOKEN_EXPIRY;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
