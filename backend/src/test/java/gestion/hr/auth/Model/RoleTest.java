package gestion.hr.auth.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void seulsAdminEtDirectionVoientToutesLesDemandes() {
        Set<Role> autorises = EnumSet.noneOf(Role.class);
        for (Role r : Role.values()) {
            if (r.voitToutesLesDemandes()) autorises.add(r);
        }
        assertEquals(EnumSet.of(Role.ADMIN, Role.DIRECTION), autorises);
    }
}
