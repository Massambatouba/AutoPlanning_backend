package org.makarimal.projet_gestionautoplanningsecure.dto;

import java.util.List;

public record SendReport(List<SendResult> results) {

    public long successCount() {
        return results.stream().filter(SendResult::success).count();
    }
    public long failureCount() {
        return results.size() - successCount();
    }
}
