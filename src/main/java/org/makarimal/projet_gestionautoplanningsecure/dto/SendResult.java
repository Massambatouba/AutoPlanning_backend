package org.makarimal.projet_gestionautoplanningsecure.dto;

/**
 * Résultat d’un envoi pour un salarié.
 *
 * @param empId   identifiant de l’employé
 * @param success true si mail/PDF OK, false sinon
 * @param error   message d’erreur quand success == false (sinon null)
 */
public record SendResult(Long empId, boolean success, String error) {

    /* usines pratiques */
    public static SendResult ok(Long empId) {
        return new SendResult(empId, true, null);
    }
    public static SendResult ko(Long empId, Throwable ex) {
        return new SendResult(empId, false, ex.getMessage());
    }
}
