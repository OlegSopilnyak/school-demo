package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of person-profile command-ids
 */
public interface ProfileCommands {
    String FIND_BY_ID = "profile.person.findById";
    String FIND_STUDENT_BY_ID = "profile.student.findById";
    String FIND_PRINCIPAL_BY_ID = "profile.principal.findById";
    String CREATE_OR_UPDATE_STUDENT = "profile.student.createOrUpdate";
    String CREATE_OR_UPDATE_PRINCIPAL = "profile.principal.createOrUpdate";
}
