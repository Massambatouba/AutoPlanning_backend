package org.makarimal.projet_gestionautoplanningsecure.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AgentType {
    ADS_Filtrage,
    ADS,
    ADS_Agent_Securite,
    SSIAP1,
    SSIAP2,
    SSIAP3,
    CHEF_DE_POSTE,
    CHEF_DE_EQUIPE,
    RONDIER,
    ASTREINTE,
    FORMATION;

    @JsonCreator
    public static AgentType fromString(String value) {
        for (AgentType type : AgentType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid AgentType: " + value);
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}