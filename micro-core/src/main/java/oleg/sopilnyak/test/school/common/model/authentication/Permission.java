package oleg.sopilnyak.test.school.common.model.authentication;


/**
 * Enumeration: possible authenticated user permissions for principal persons
 */
public enum Permission {
    // education part permissions
    EDU_LIST("Can look for Students or Courses"),
    EDU_GET("Can get the instance of Student or Course"),
    EDU_UPDATE("Can update the instance of Student or Course"),
    EDU_DELETE("Can remove the instance of Student or Course"),
    // organization part permissions
    ORG_LIST("Can look for AuthorityPersons, Faculties or StudentsGroups"),
    ORG_GET("Can get the instance of AuthorityPerson, Faculty or StudentsGroup"),
    ORG_UPDATE("Can update the instance of AuthorityPerson, Faculty or StudentsGroup"),
    ORG_DELETE("Can remove the instance of AuthorityPerson, Faculty or StudentsGroup")
    ;
    private final String description;

    Permission(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Permission:" + description;
    }
}
