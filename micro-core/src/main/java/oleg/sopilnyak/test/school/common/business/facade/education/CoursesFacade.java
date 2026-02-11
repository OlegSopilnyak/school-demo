package oleg.sopilnyak.test.school.common.business.facade.education;

import oleg.sopilnyak.test.school.common.business.facade.education.base.EducationFacade;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;

import java.util.List;

/**
 * Service-Facade: Service for manage courses in the school
 *
 * @see Student
 * @see Course
 */
public interface CoursesFacade extends EducationFacade {
    String SUBSPACE = "::courses";
    String FIND_BY_ID = NAMESPACE + SUBSPACE + ":find.By.Id";
    String FIND_REGISTERED = NAMESPACE + SUBSPACE + ":find.Registered.To.The.Student";
    String FIND_NOT_REGISTERED = NAMESPACE + SUBSPACE + ":find.Without.Any.Student";
    String CREATE_OR_UPDATE = NAMESPACE + SUBSPACE + ":create.Or.Update";
    String DELETE = NAMESPACE + SUBSPACE + ":delete";
    String REGISTER = NAMESPACE + SUBSPACE + ":register";
    String UN_REGISTER = NAMESPACE + SUBSPACE + ":unregister";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(
            FIND_BY_ID, FIND_REGISTERED, FIND_NOT_REGISTERED, CREATE_OR_UPDATE, DELETE, REGISTER, UN_REGISTER
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
        return "CoursesFacade";
    }
}

