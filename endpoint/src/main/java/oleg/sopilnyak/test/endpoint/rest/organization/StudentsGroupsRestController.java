package oleg.sopilnyak.test.endpoint.rest.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.STUDENT_GROUPS)
@ResponseStatus(HttpStatus.OK)
public class StudentsGroupsRestController {
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);
    public static final String FACADE_NAME = "StudentsGroupFacade";
    public static final String VAR_NAME = "groupId";
    public static final String WRONG_STUDENTS_GROUP_ID = "Wrong students-group-id: '";
    // delegate for requests processing
    private final StudentsGroupFacade facade;

    @GetMapping
    public List<StudentsGroupDto> findAll() {
        ActionContext.setup(FACADE_NAME, "findAll");
        log.debug("Trying to get all school's students groups");
        try {
            return resultToDto(facade.findAllStudentsGroups());
        } catch (Exception e) {
            log.error("Cannot get all school's students groups", e);
            throw new CannotProcessActionException("Cannot get all school's students groups", e);
        }
    }

    @GetMapping("/{" + VAR_NAME + "}")
    public StudentsGroupDto findById(@PathVariable(VAR_NAME) String groupId) {
        ActionContext.setup(FACADE_NAME, "findById");
        log.debug("Trying to get students group by Id: '{}'", groupId);
        try {
            final Long id = Long.parseLong(groupId);
            log.debug("Getting students group for id: {}", id);

            return resultToDto(groupId, facade.findStudentsGroupById(id));
        } catch (NumberFormatException e) {
            throw new StudentsGroupNotFoundException(WRONG_STUDENTS_GROUP_ID + groupId + "'");
        } catch (Exception e) {
            log.error("Cannot get students group for id: {}", groupId, e);
            throw new CannotProcessActionException("Cannot get students group for id: " + groupId, e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentsGroupDto create(@RequestBody StudentsGroupDto studentsGroupDto) {
        ActionContext.setup(FACADE_NAME, "createNew");
        log.debug("Trying to create the students group {}", studentsGroupDto);
        try {
            studentsGroupDto.setId(null);
            return resultToDto(facade.createOrUpdateStudentsGroup(studentsGroupDto));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot create new students group " + studentsGroupDto, e);
        }
    }

    @PutMapping
    public StudentsGroupDto update(@RequestBody StudentsGroupDto studentsGroupDto) {
        ActionContext.setup(FACADE_NAME, "updateExists");
        log.debug("Trying to update students group {}", studentsGroupDto);
        try {
            final Long id = studentsGroupDto.getId();
            if (isInvalid(id)) {
                throw new StudentsGroupNotFoundException(WRONG_STUDENTS_GROUP_ID + id + "'");
            }
            return resultToDto(facade.createOrUpdateStudentsGroup(studentsGroupDto));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot update students group " + studentsGroupDto, e);
        }
    }

    @DeleteMapping("/{" + VAR_NAME + "}")
    public void delete(@PathVariable(VAR_NAME) String groupId) {
        ActionContext.setup(FACADE_NAME, "deleteById");
        log.debug("Trying to delete students group for Id: '{}'", groupId);
        try {
            final Long id = Long.parseLong(groupId);
            log.debug("Deleting students group for id: {}", id);
            if (isInvalid(id)) {
                throw new StudentsGroupNotFoundException(WRONG_STUDENTS_GROUP_ID + id + "'");
            }

            facade.deleteStudentsGroupById(id);
        } catch (NumberFormatException e) {
            throw new StudentsGroupNotFoundException(WRONG_STUDENTS_GROUP_ID + groupId + "'");
        } catch (Exception e) {
            log.error("Cannot delete students group for id = {}", groupId, e);
            throw new CannotProcessActionException("Cannot delete students group for id = " + groupId, e);
        }
    }

    // private methods
    private StudentsGroupDto resultToDto(Optional<StudentsGroup> course) {
        log.debug("Converting {} to DTO", course);
        return mapper.toDto(
                course.orElseThrow(() -> new StudentsGroupNotFoundException("group is not updated"))
        );
    }

    private StudentsGroupDto resultToDto(String groupId, Optional<StudentsGroup> faculty) {
        log.debug("Converting {} to DTO for students-group-id '{}'", faculty, groupId);
        return mapper.toDto(
                faculty.orElseThrow(() -> new StudentsGroupNotFoundException("StudentsGroup with id: " + groupId + " is not found"))
        );
    }

    private List<StudentsGroupDto> resultToDto(Collection<StudentsGroup> persons) {
        return persons.stream().map(mapper::toDto).filter(Objects::nonNull)
                .sorted(Comparator.comparing(StudentsGroupDto::getName))
                .toList();
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0L;
    }

}
