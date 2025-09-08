package org.makarimal.projet_gestionautoplanningsecure.service;


import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /** Enregistre le fichier et retourne une URL (ou chemin) accessible côté front */
    String store(MultipartFile file);
}

