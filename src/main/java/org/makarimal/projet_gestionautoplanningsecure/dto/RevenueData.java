package org.makarimal.projet_gestionautoplanningsecure.dto;

import java.math.BigDecimal;

public record RevenueData(String month, BigDecimal revenue, int newCompanies) {

    public RevenueData(String month, BigDecimal revenue) {
        this(month, revenue, 0);
    }
}
