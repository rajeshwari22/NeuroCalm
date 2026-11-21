package com.neurocalm.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Tells Spring where the JPA entities and repositories live. */
@Configuration
@EnableTransactionManagement
@EntityScan("com.neurocalm.model")
@EnableJpaRepositories("com.neurocalm.repository")
public class DatabaseConfig {
}
