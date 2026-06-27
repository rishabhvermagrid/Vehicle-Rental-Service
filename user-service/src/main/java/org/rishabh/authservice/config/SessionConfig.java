package org.rishabh.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    /**
     * Overrides the default Spring Session cookie ("SESSION") with a custom
     * HttpOnly "SESSION_ID" cookie. Spring Session auto-detects this bean.
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION_ID");
        // HttpOnly is always true in DefaultCookieSerializer — no setter needed
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        // Set to true in production (requires HTTPS)
        serializer.setUseSecureCookie(false);
        // Match cookie lifetime to server-side session TTL (application.yml: session.timeout)
        serializer.setCookieMaxAge(3600);
        return serializer;
    }
}
