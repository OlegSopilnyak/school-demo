package oleg.sopilnyak.test.school.common.model.authentication;

import java.util.Arrays;
import java.util.Set;

/**
 * Enumeration: possible authenticated user roles for school staff
 */
public enum Role {
    PRINCIPAL("School Principal",
            Permission.values()
    ),
    DEPUTY_PRINCIPAL("School Deputy Principal",
            allExcept(Permission.ORG_PERSON_CREATE, Permission.ORG_DELETE)
    ),
    HEAD_TEACHER("School Head Teacher",
            allExcept(Permission.ORG_PERSON_CREATE, Permission.EDU_DELETE, Permission.ORG_DELETE)
    ),
    TEACHER("School Teacher",
            only(Permission.ORG_LIST, Permission.ORG_GET,  Permission.EDU_LIST, Permission.EDU_GET)
    ),
    SUPPORT_STAFF   ("School Support Staff",
            only(Permission.ORG_LIST,  Permission.EDU_LIST, Permission.EDU_GET)
    )
    ;
    // description of the role
    private final String description;
    // default permission allowed for the role
    private final Permission[] permission;

    Role(String description, Permission[] permission) {
        this.description = description;
        this.permission = permission;
    }

    public Permission[] getDefaultPermissions() {
        return permission;
    }

    private static Permission[] allExcept(Permission... excepted) {
        final Set<Permission> exceptionSet = Set.copyOf(Arrays.asList(excepted));
        return Arrays.stream(Permission.values())
                .filter(p -> !exceptionSet.contains(p))
                .toArray(Permission[]::new);
    }

    private static Permission[] only(Permission... allowed) {
        return Arrays.stream(allowed).toArray(Permission[]::new);
    }

    @Override
    public String toString() {
        return "The staff role: " + description;
    }
}
