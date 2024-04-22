package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.StudentsGroupDto;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.CannotDoRestCallException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentsGroupException;
import oleg.sopilnyak.test.school.common.business.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.STUDENT_GROUPS)
public class StudentsGroupsRestController {
    public static final String VAR_NAME = "groupId";
    // delegate for requests processing
    private final StudentsGroupFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping
    public ResponseEntity<List<StudentsGroupDto>> findAll() {
        log.debug("Trying to get all school's students groups");
        try {
            return ResponseEntity.ok(resultToDto(facade.findAllStudentsGroups()));
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot get all school's students groups", e);
        }
    }

    @GetMapping("/{" + VAR_NAME + "}")
    public ResponseEntity<StudentsGroupDto> findById(@PathVariable(VAR_NAME) String groupId) {
        log.debug("Trying to get students group by Id: '{}'", groupId);
        try {
            Long id = Long.parseLong(groupId);
            log.debug("Getting students group for id: {}", id);

            return ResponseEntity.ok(resultToDto(groupId, facade.getStudentsGroupById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong students-group-id: '{}'", groupId);
            throw new ResourceNotFoundException("Wrong students-group-id: '" + groupId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot get students group for id: " + groupId, e);
        }
    }

    @PostMapping
    public ResponseEntity<StudentsGroupDto> create(@RequestBody StudentsGroupDto studentsGroupDto) {
        log.debug("Trying to create the students group {}", studentsGroupDto);
        try {
            studentsGroupDto.setId(null);
            return ResponseEntity.ok(resultToDto(facade.createOrUpdateStudentsGroup(studentsGroupDto)));
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot create new students group " + studentsGroupDto.toString(), e);
        }
    }

    @PutMapping
    public ResponseEntity<StudentsGroupDto> update(@RequestBody StudentsGroupDto studentsGroupDto) {
        log.debug("Trying to update students group {}", studentsGroupDto);
        try {
            Long id = studentsGroupDto.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException("Wrong students-group-id: '" + id + "'");
            }
            return ResponseEntity.ok(resultToDto(facade.createOrUpdateStudentsGroup(studentsGroupDto)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot update students group " + studentsGroupDto.toString());
        }
    }

    @DeleteMapping("/{" + VAR_NAME + "}")
    public ResponseEntity<Void> delete(@PathVariable(VAR_NAME) String groupId) {
        log.debug("Trying to delete students group for Id: '{}'", groupId);
        try {
            Long id = Long.parseLong(groupId);
            log.debug("Deleting students group for id: {}", id);
            if (isInvalid(id)) {
                throw new NumberFormatException();
            }

            facade.deleteStudentsGroupById(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | NotExistStudentsGroupException e) {
            log.error("Wrong students-group-id: '{}'", groupId);
            throw new ResourceNotFoundException("Wrong students-group-id: '" + groupId + "'");
        } catch (Exception e) {
            log.error("Cannot delete students group for id = {}", groupId, e);
            throw new CannotDeleteResourceException("Cannot delete students group for id = " + groupId, e);
        }
    }

    // private methods
    private StudentsGroupDto resultToDto(Optional<StudentsGroup> course) {
        log.debug("Converting {} to DTO", course);
        return mapper.toDto(
                course.orElseThrow(() -> new ResourceNotFoundException("group is not updated"))
        );
    }

    private StudentsGroupDto resultToDto(String groupId, Optional<StudentsGroup> faculty) {
        log.debug("Converting {} to DTO for students-group-id '{}'", faculty, groupId);
        return mapper.toDto(
                faculty.orElseThrow(() -> new ResourceNotFoundException("StudentsGroup with id: " + groupId + " is not found"))
        );
    }

    private List<StudentsGroupDto> resultToDto(Collection<StudentsGroup> persons) {
        return persons.stream()
                .map(mapper::toDto)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StudentsGroupDto::getName))
                .toList();
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }

}
