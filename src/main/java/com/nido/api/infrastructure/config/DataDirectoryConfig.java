package com.nido.api.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class DataDirectoryConfig {

    @Bean
    DataDirectory dataDirectory(@Value("${NIDO_DATA_DIR:./data}") String root) {
        return new DataDirectory(Path.of(root));
    }
}
