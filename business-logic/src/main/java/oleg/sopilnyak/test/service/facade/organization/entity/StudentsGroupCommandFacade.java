package oleg.sopilnyak.test.service.facade.organization.entity;

import oleg.sopilnyak.test.school.common.model.StudentsGroup;

/**
 * Service-Facade: To process command for school's faculties
 * @see StudentsGroup
 */
public interface StudentsGroupCommandFacade {
    String FIND_ALL = "organization.students.group.findAll";
    String FIND_BY_ID = "organization.students.group.findById";
    String CREATE_OR_UPDATE = "organization.students.group.createOrUpdate";
    String DELETE = "organization.students.group.delete";
}
