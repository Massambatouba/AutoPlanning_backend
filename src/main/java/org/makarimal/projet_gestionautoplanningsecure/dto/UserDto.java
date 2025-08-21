/*
package org.makarimal.projet_gestionautoplanningsecure.dto;

import org.makarimal.projet_gestionautoplanningsecure.model.Role;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.model.User;

import java.util.Collections;
import java.util.List;

public record UserDto(
        Long   id,
        String firstName,
        String lastName,
        String email,
        boolean active,
        List<Role> roles,
        boolean manageAllEmployees,
        List<Long>   managedSiteIds
) {
    public static UserDto of(User u){
        boolean all = u.isManageAllSites();


        List<Long> siteIds = all
                ? Collections.emptyList()
                : u.getManagedSites()
                .stream()
                .map(Site::getId)
                .toList();

        return new UserDto(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.isActive(),
                u.getRoles(),
                all,
                siteIds
        );
    }
}
 */

package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Builder;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.model.User;

import java.util.List;

@Builder
public record UserDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        List<String> roles,
        boolean isActive,
        boolean manageAllSites,
        List<Long> siteIds
) {
    public static UserDto of(User u) {
        return UserDto.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .roles(u.getRoles().stream().map(Role::name).toList())
                .isActive(u.isActive())
                .manageAllSites(u.isManageAllSites())
                .siteIds(u.getManagedSites().stream().map(Site::getId).toList())
                .build();
    }
}