package oleg.sopilnyak.test.endpoint.rest.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.profile.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.STUDENT_PROFILES)
@ResponseStatus(HttpStatus.OK)
public class StudentProfileRestController {
    private static final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);
    public static final String FACADE_NAME = "StudentProfileFacade";
    public static final String PROFILE_ID_VAR_NAME = "personProfileId";
    public static final String WRONG_STUDENT_PROFILE_ID = "Wrong student profile-id: '";
    private StudentProfileFacade facade;

    @GetMapping("/{" + PROFILE_ID_VAR_NAME + "}")
    public StudentProfileDto findById(@PathVariable(PROFILE_ID_VAR_NAME) String personId) {
        ActionContext.setup(FACADE_NAME, "findById");
        log.debug("Trying to get student-profile by Id: '{}'", personId);
        try {
            final Long id = Long.parseLong(personId);
            log.debug("Getting student-profile for id: {}", id);

            return toDto(id, facade.findStudentProfileById(id));
        } catch (NumberFormatException e) {
            throw new ProfileNotFoundException(WRONG_STUDENT_PROFILE_ID + personId + "'");
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot get student-profile for id: " + personId, e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentProfileDto create(@RequestBody StudentProfileDto profileDto) {
        ActionContext.setup(FACADE_NAME, "createNew");
        log.debug("Trying to create student-profile {}", profileDto);
        try {
            profileDto.setId(null);
            return toDto(null, facade.createOrUpdateProfile(profileDto));
        } catch (Exception e) {
            log.error("Cannot create new student-profile {}", profileDto.toString(), e);
            throw new CannotProcessActionException("Cannot create new student-profile " + profileDto, e);
        }
    }

    @PutMapping
    public StudentProfileDto update(@RequestBody StudentProfileDto profileDto) {
        ActionContext.setup(FACADE_NAME, "updateExists");
        log.debug("Trying to update student-profile {}", profileDto);
        try {
            final Long id = profileDto.getId();
            if (isInvalid(id)) {
                throw new ProfileNotFoundException(WRONG_STUDENT_PROFILE_ID + id + "'");
            }
            return toDto(id, facade.createOrUpdateProfile(profileDto));
        } catch (Exception e) {
            log.error("Cannot update student-profile {}", profileDto.toString(), e);
            throw new CannotProcessActionException("Cannot update student-profile " + profileDto, e);
        }
    }

    @DeleteMapping
    public void delete(@RequestBody StudentProfileDto profileDto) {
        ActionContext.setup(FACADE_NAME, "delete");
        log.debug("Trying to delete student-profile : {}", profileDto);
        try {
            log.debug("Deleting student-profile : {}", profileDto);

            facade.delete(profileDto);
        } catch (Exception e) {
            log.error("Cannot delete student-profile {}", profileDto, e);
            throw new CannotProcessActionException("Cannot delete student-profile " + profileDto, e);
        }
    }

    @DeleteMapping("/{" + PROFILE_ID_VAR_NAME + "}")
    public void deleteById(@PathVariable(PROFILE_ID_VAR_NAME) String profileId) {
        ActionContext.setup(FACADE_NAME, "deleteById");
        log.debug("Trying to delete student-profile for Id: '{}'", profileId);
        try {
            final Long id = Long.parseLong(profileId);
            log.debug("Deleting student-profile for id: {}", id);
            if (isInvalid(id)) {
                throw new ProfileNotFoundException(WRONG_STUDENT_PROFILE_ID + id + "'");
            }

            facade.deleteById(id);
        } catch (NumberFormatException e) {
            throw new ProfileNotFoundException(WRONG_STUDENT_PROFILE_ID + profileId + "'");
        } catch (Exception e) {
            log.error("Cannot delete student-profile for id = {}", profileId, e);
            throw new CannotProcessActionException("Cannot delete student-profile for id = " + profileId, e);
        }
    }


    private static StudentProfileDto toDto(Long profileId, Optional<StudentProfile> profile) {
        log.debug("Converting {} to DTO for student profile-id '{}'", profile, profileId);
        return mapper.toDto(
                profile.orElseThrow(() -> new ProfileNotFoundException("Profile with id: " + profileId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0L;
    }

}
