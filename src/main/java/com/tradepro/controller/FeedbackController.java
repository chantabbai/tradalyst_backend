package com.tradepro.controller;

import com.tradepro.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class FeedbackController {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);
    private final UserService userService;

    @Autowired
    public FeedbackController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/contact")
    public ResponseEntity<String> submitContactForm(@Valid @RequestBody ContactFormRequest request) {
        try {
            logger.info("Processing contact form submission for email: {}", request.getEmail());
            userService.sendContactFormEmail(request.getEmail(), request.getMessage());
            return ResponseEntity.ok("Message sent successfully");
        } catch (Exception e) {
            logger.error("Failed to process contact form: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 