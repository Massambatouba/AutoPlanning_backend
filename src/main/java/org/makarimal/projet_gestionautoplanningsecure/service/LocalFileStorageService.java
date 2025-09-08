package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.upload-public-prefix:/files}")
    private String publicPrefix;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(base);

            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);

            String name = UUID.randomUUID() + ext;
            Path target = base.resolve(name);

            // copie atomique
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // URL publique (cf. WebMvc config plus bas)
            String url = publicPrefix.endsWith("/") ? publicPrefix + name : publicPrefix + "/" + name;
            return url;
        } catch (IOException e) {
            log.error("Erreur stockage fichier", e);
            throw new RuntimeException("Impossible de stocker le fichier", e);
        }
    }
}

