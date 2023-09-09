package oleg.sopilnyak.test.service.facade.organization.entity;

import oleg.sopilnyak.test.school.common.model.Faculty;

/**
 * Service-Facade: To process command for school's faculties
 * @see Faculty
 */
public interface FacultyCommandFacade {
    String FIND_ALL = "organization.faculty.findAll";
    String FIND_BY_ID = "organization.faculty.findById";
    String CREATE_OR_UPDATE = "organization.faculty.createOrUpdate";
    String DELETE = "organization.faculty.delete";
}
