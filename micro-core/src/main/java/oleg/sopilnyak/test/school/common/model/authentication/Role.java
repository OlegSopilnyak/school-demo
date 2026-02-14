package oleg.sopilnyak.test.school.common.model.authentication;

/**
 * Enumeration: possible authenticated user roles for principal persons
 */
public enum Role {
    PRINCIPAL("School Principal"),
    DEPUTY_PRINCIPAL("School Deputy Principal"),
    HEAD_TEACHER("School Head Teacher"),
    TEACHER("School Teacher"),
    SUPPORT_STAFF   ("School Support Staff")
    ;
    private final String description;

    Role(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Role:" + description;
    }
}
