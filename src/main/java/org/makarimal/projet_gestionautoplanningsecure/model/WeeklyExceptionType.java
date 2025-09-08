package org.makarimal.projet_gestionautoplanningsecure.model;

public enum WeeklyExceptionType {
    ADD_SHIFT,     // ajoute un shift
    CLOSE_DAY,     // ferme la journée
    REPLACE_DAY,   // remplace entièrement la journée (ignore le weekly)
    MASK_SHIFT     // supprime les shifts qui matchent (ex : plage de nuit)
}

