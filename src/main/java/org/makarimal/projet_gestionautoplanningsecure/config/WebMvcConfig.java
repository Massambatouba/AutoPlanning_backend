package org.makarimal.projet_gestionautoplanningsecure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.upload-public-prefix:/files}")
    private String publicPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + uploadPath.toString() + "/";
        String pattern  = (publicPrefix.endsWith("/")) ? publicPrefix + "**" : publicPrefix + "/**";

        registry.addResourceHandler(pattern)
                .addResourceLocations(location)
                .setCachePeriod(3600); // 1h de cache
    }
}
