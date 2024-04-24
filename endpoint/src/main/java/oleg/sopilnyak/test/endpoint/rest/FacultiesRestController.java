package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.CannotDoRestCallException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.business.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.exception.NotExistFacultyException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.FACULTIES)
public class FacultiesRestController {
    public static final String VAR_NAME = "facultyId";
    public static final String WRONG_FACULTY_ID = "Wrong faculty-id: '";
    // delegate for requests processing
    private final FacultyFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping
    public ResponseEntity<List<FacultyDto>> findAll() {
        log.debug("Trying to get all school's faculties");
        try {
            return ResponseEntity.ok(resultToDto(facade.findAllFaculties()));
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot get all school's faculties", e);
        }
    }

    @GetMapping("/{" + VAR_NAME + "}")
    public ResponseEntity<FacultyDto> findById(@PathVariable(VAR_NAME) String facultyId) {
        log.debug("Trying to get faculty by Id: '{}'", facultyId);
        try {
            Long id = Long.parseLong(facultyId);
            log.debug("Getting faculty for id: {}", id);

            return ResponseEntity.ok(resultToDto(facultyId, facade.getFacultyById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong faculty-id: '{}'", facultyId);
            throw new ResourceNotFoundException(WRONG_FACULTY_ID + facultyId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot get faculty for id: " + facultyId, e);
        }
    }

    @PostMapping
    public ResponseEntity<FacultyDto> create(@RequestBody FacultyDto facultyDto) {
        log.debug("Trying to create the faculty {}", facultyDto);
        try {
            facultyDto.setId(null);
            return ResponseEntity.ok(resultToDto(facade.createOrUpdateFaculty(facultyDto)));
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot create new faculty " + facultyDto.toString(), e);
        }
    }

    @PutMapping
    public ResponseEntity<FacultyDto> update(@RequestBody FacultyDto facultyDto) {
        log.debug("Trying to update faculty {}", facultyDto);
        try {
            Long id = facultyDto.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException(WRONG_FACULTY_ID + id + "'");
            }
            return ResponseEntity.ok(resultToDto(facade.createOrUpdateFaculty(facultyDto)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot update faculty " + facultyDto.toString());
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody FacultyDto facultyDto) {
        log.debug("Trying to delete faculty {}", facultyDto);
        try {
            log.debug("Deleting faculty {}", facultyDto);

            facade.deleteFaculty(facultyDto);

            return ResponseEntity.ok().build();
        } catch (NotExistFacultyException e) {
            log.error("Wrong faculty {}", facultyDto);
            throw new ResourceNotFoundException("Wrong faculty " + facultyDto);
        } catch (Exception e) {
            log.error("Cannot delete faculty {}", facultyDto, e);
            throw new CannotDeleteResourceException("Cannot delete faculty " + facultyDto, e);
        }
    }

    @DeleteMapping("/{" + VAR_NAME + "}")
    public ResponseEntity<Void> delete(@PathVariable(VAR_NAME) String facultyId) {
        log.debug("Trying to delete faculty for Id: '{}'", facultyId);
        try {
            Long id = Long.parseLong(facultyId);
            log.debug("Deleting faculty for id: {}", id);
            if (isInvalid(id)) {
                throw new NumberFormatException();
            }

            facade.deleteFacultyById(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | NotExistFacultyException e) {
            log.error("Wrong faculty-id: '{}'", facultyId);
            throw new ResourceNotFoundException(WRONG_FACULTY_ID + facultyId + "'");
        } catch (Exception e) {
            log.error("Cannot delete faculty for id = {}", facultyId, e);
            throw new CannotDeleteResourceException("Cannot delete faculty for id = " + facultyId, e);
        }
    }

    // private methods
    private FacultyDto resultToDto(Optional<Faculty> course) {
        log.debug("Converting {} to DTO", course);
        return mapper.toDto(
                course.orElseThrow(() -> new ResourceNotFoundException("faculty is not updated"))
        );
    }

    private FacultyDto resultToDto(String facultyId, Optional<Faculty> faculty) {
        log.debug("Converting {} to DTO for faculty-id '{}'", faculty, facultyId);
        return mapper.toDto(
                faculty.orElseThrow(() -> new ResourceNotFoundException("Faculty with id: " + facultyId + " is not found"))
        );
    }

    private List<FacultyDto> resultToDto(Collection<Faculty> persons) {
        return persons.stream()
                .map(mapper::toDto)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(FacultyDto::getName))
                .toList();
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }

}
