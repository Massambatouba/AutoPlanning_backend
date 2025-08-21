package org.makarimal.projet_gestionautoplanningsecure.dto;

import org.makarimal.projet_gestionautoplanningsecure.model.Notification;

import java.time.Duration;
import java.time.LocalDateTime;

public record NotificationDTO(
        Long   id,
        String message,
        String icon,
        String timeAgo         // « il y a 2 h »
) {
    public static NotificationDTO of(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getMessage(),
                n.getIcon(),
                humanize(n.getCreatedAt())
        );
    }

    private static String humanize(LocalDateTime ts) {
        Duration d = Duration.between(ts, LocalDateTime.now());
        if (d.toDays()   >= 1) return "il y a " + d.toDays()   + " j";
        if (d.toHours()  >= 1) return "il y a " + d.toHours()  + " h";
        if (d.toMinutes()>= 1) return "il y a " + d.toMinutes()+ " min";
        return "à l’instant";
    }
}

