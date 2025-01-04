
package com.tradepro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://0.0.0.0:3000", 
                "https://*.repl.co", 
                "https://*.replit.dev",
                "https://tradalystfrontend-chantabbai07ai.replit.app",
                "https://tradalyst.com/",
                "https://c71f7103-fbd0-4f80-b1ad-4983fe533a72-00-33fxwqqrqumnb.spock.replit.dev/auth/login"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .exposedHeaders("Authorization")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
