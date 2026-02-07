package oleg.sopilnyak.test.school.common.business.facade.education;

import oleg.sopilnyak.test.school.common.business.facade.education.base.EducationFacade;

import java.util.List;

/**
 * Service-Facade: Service for manage students in the school
 */
public interface StudentsFacade extends EducationFacade {
    String SUBSPACE = "::students";
    String FIND_BY_ID = NAMESPACE + SUBSPACE + ":find.By.Id";
    String FIND_ENROLLED = NAMESPACE + SUBSPACE + ":find.Enrolled.To.The.Course";
    String FIND_NOT_ENROLLED = NAMESPACE + SUBSPACE + ":find.Not.Enrolled.To.Any.Course";
    String CREATE_OR_UPDATE = NAMESPACE + SUBSPACE + ":create.Or.Update";
    String CREATE_MACRO = NAMESPACE + SUBSPACE + ":create.Macro";
    String DELETE = NAMESPACE + SUBSPACE + ":delete";
    String DELETE_MACRO = NAMESPACE + SUBSPACE + ":delete.Macro";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(
            FIND_BY_ID, FIND_ENROLLED, FIND_NOT_ENROLLED, CREATE_OR_UPDATE, CREATE_MACRO, DELETE, DELETE_MACRO
    );

    /**
     * To get the list of valid action-ids
     *
     * @return valid action-ids for concrete descendant-facade
     */
    @Override
    default List<String> validActions() {
        return ACTION_IDS;
    }

    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    @Override
    default String getName() {
        return "StudentsFacade";
    }
}
