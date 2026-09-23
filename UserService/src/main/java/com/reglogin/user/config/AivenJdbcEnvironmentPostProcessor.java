package com.reglogin.user.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the JDBC URL from the per-field Aiven connection variables:
 * DB_HOST, DB_PORT, DB_NAME, DB_SSL (plus DB_USERNAME/DB_PASSWORD pass-through).
 * If a full DB_URL is provided it is used as-is and this processor does nothing.
 */
public class AivenJdbcEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("DB_URL");
        if (hasText(url)) {
            return;
        }

        String host = environment.getProperty("DB_HOST");
        String database = environment.getProperty("DB_NAME");
        if (!hasText(host) || !hasText(database)) {
            return;
        }

        String port = environment.getProperty("DB_PORT", "3306");
        boolean ssl = !"false".equalsIgnoreCase(environment.getProperty("DB_SSL", "true"));

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?ssl-mode=" + (ssl ? "REQUIRED" : "PREFERRED")
                + "&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", jdbcUrl);
        environment.getPropertySources().addFirst(new MapPropertySource("aiven-jdbc-props", props));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}