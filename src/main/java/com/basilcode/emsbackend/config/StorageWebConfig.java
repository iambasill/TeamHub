package com.basilcode.emsbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Registers a Spring MVC resource handler so that files stored on the local filesystem
 * under {@code app.storage.local.upload-dir} are served at {@code /uploads/**}.
 *
 * <p>This handler is harmless when {@code app.storage.type=cloud} — the path simply
 * returns 404 since no files are written there.
 */
@Configuration
public class StorageWebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toString();
        // Ensure the path ends with a separator so Spring resolves sub-paths correctly
        String resourceLocation = "file:" + absolutePath + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
