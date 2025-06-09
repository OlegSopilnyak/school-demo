package oleg.sopilnyak.test.endpoint.rest.organization;

import static java.util.Objects.isNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.FacultyDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@Getter
@RestController
@RequestMapping(RequestMappingRoot.FACULTIES)
@ResponseStatus(HttpStatus.OK)
public class FacultiesRestController {
    public static final String VAR_NAME = "facultyId";
    public static final String WRONG_FACULTY_ID = "Wrong faculty-id: '";
    // delegate for requests processing
    private final FacultyFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping
    public List<FacultyDto> findAll() {
        log.debug("Trying to get all school's faculties");
        try {
            return resultToDto(facade.findAllFaculties());
        } catch (Exception e) {
            log.error("Cannot get all school's faculties", e);
            throw new CannotProcessActionException("Cannot get all school's faculties", e);
        }
    }

    @GetMapping("/{" + VAR_NAME + "}")
    public FacultyDto findById(@PathVariable(VAR_NAME) String facultyId) {
        log.debug("Trying to get faculty by Id: '{}'", facultyId);
        try {
            final Long id = Long.parseLong(facultyId);
            log.debug("Getting faculty for id: {}", id);

            return resultToDto(facultyId, facade.findFacultyById(id));
        } catch (NumberFormatException e) {
            throw new FacultyNotFoundException(WRONG_FACULTY_ID + facultyId + "'");
        } catch (Exception e) {
            log.error("Cannot get faculty for id: {}", facultyId, e);
            throw new CannotProcessActionException("Cannot get faculty for id: " + facultyId, e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyDto create(@RequestBody FacultyDto facultyDto) {
        log.debug("Trying to create the faculty {}", facultyDto);
        try {
            facultyDto.setId(null);
            return resultToDto(facade.createOrUpdateFaculty(facultyDto));
        } catch (Exception e) {
            log.error("Cannot create new faculty {}", facultyDto, e);
            throw new CannotProcessActionException("Cannot create new faculty " + facultyDto, e);
        }
    }

    @PutMapping
    public FacultyDto update(@RequestBody FacultyDto facultyDto) {
        log.debug("Trying to update faculty {}", facultyDto);
        try {
            final Long id = facultyDto.getId();
            if (isInvalid(id)) {
                throw new FacultyNotFoundException(WRONG_FACULTY_ID + id + "'");
            }
            return resultToDto(facade.createOrUpdateFaculty(facultyDto));
        } catch (Exception e) {
            log.error("Cannot update new faculty {}", facultyDto, e);
            throw new CannotProcessActionException("Cannot update faculty " + facultyDto, e);
        }
    }

    @DeleteMapping
    public void delete(@RequestBody FacultyDto facultyDto) {
        log.debug("Trying to delete faculty {}", facultyDto);
        try {
            log.debug("Deleting faculty {}", facultyDto);

            facade.deleteFaculty(facultyDto);
        } catch (Exception e) {
            log.error("Cannot delete faculty {}", facultyDto, e);
            throw new CannotProcessActionException("Cannot delete faculty " + facultyDto, e);
        }
    }

    @DeleteMapping("/{" + VAR_NAME + "}")
    public void delete(@PathVariable(VAR_NAME) String facultyId) {
        log.debug("Trying to delete faculty for Id: '{}'", facultyId);
        try {
            final Long id = Long.parseLong(facultyId);
            log.debug("Deleting faculty for id: {}", id);
            if (isInvalid(id)) {
                throw new FacultyNotFoundException(WRONG_FACULTY_ID + id + "'");
            }

            facade.deleteFacultyById(id);
        } catch (NumberFormatException e) {
            throw new FacultyNotFoundException(WRONG_FACULTY_ID + facultyId + "'", e);
        } catch (Exception e) {
            log.error("Cannot delete faculty for id = {}", facultyId, e);
            throw new CannotProcessActionException("Cannot delete faculty for id = " + facultyId, e);
        }
    }

    // private methods
    private FacultyDto resultToDto(Optional<Faculty> course) {
        log.debug("Converting {} to DTO", course);
        return mapper.toDto(
                course.orElseThrow(() -> new CourseNotFoundException("faculty is not updated"))
        );
    }

    private FacultyDto resultToDto(String facultyId, Optional<Faculty> faculty) {
        log.debug("Converting {} to DTO for faculty-id '{}'", faculty, facultyId);
        return mapper.toDto(
                faculty.orElseThrow(() -> new FacultyNotFoundException("Faculty with id: " + facultyId + " is not found"))
        );
    }

    private List<FacultyDto> resultToDto(Collection<Faculty> persons) {
        return persons.stream().map(mapper::toDto).filter(Objects::nonNull)
                .sorted(Comparator.comparing(FacultyDto::getName))
                .toList();
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }

}
