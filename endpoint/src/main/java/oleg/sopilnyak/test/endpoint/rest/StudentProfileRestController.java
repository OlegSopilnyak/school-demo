package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.CannotDoRestCallException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.business.profile.StudentProfileFacade;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.STUDENT_PROFILES)
public class StudentProfileRestController {
    public static final String PROFILE_ID_VAR_NAME = "personProfileId";
    private static final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);
    public static final String WRONG_STUDENT_PROFILE_ID = "Wrong student profile-id: '";
    private StudentProfileFacade facade;

    @GetMapping("/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<StudentProfileDto> findById(@PathVariable(PROFILE_ID_VAR_NAME) String personId) {
        log.debug("Trying to get student-profile by Id: '{}'", personId);
        try {
            final Long id = Long.parseLong(personId);
            log.debug("Getting student-profile for id: {}", id);

            return ResponseEntity.ok(toDto(id, facade.findStudentProfileById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong student profile-id: '{}'", personId);
            throw new ResourceNotFoundException(WRONG_STUDENT_PROFILE_ID + personId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot get student-profile for id: " + personId, e);
        }
    }

    @PostMapping
    public ResponseEntity<StudentProfileDto> create(@RequestBody StudentProfileDto profileDto) {
        log.debug("Trying to create student-profile {}", profileDto);
        try {
            profileDto.setId(null);
            return ResponseEntity.ok(toDto(null, facade.createOrUpdateProfile(profileDto)));
        } catch (Exception e) {
            log.error("Cannot create new student-profile {}", profileDto.toString(), e);
            throw new CannotDoRestCallException("Cannot create new student-profile " + profileDto, e);
        }
    }

    @PutMapping
    public ResponseEntity<StudentProfileDto> update(@RequestBody StudentProfileDto profileDto) {
        log.debug("Trying to update student-profile {}", profileDto);
        try {
            final Long id = profileDto.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException(WRONG_STUDENT_PROFILE_ID + id + "'");
            }
            return ResponseEntity.ok(toDto(id, facade.createOrUpdateProfile(profileDto)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cannot update student-profile {}", profileDto.toString(), e);
            throw new CannotDoRestCallException("Cannot update student-profile " + profileDto, e);
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody StudentProfileDto profileDto) {
        log.debug("Trying to delete student-profile : {}", profileDto);
        try {
            log.debug("Deleting student-profile : {}", profileDto);

            facade.delete(profileDto);

            return ResponseEntity.ok().build();
        } catch (ProfileIsNotFoundException e) {
            log.error("Wrong student-profile to delete {}", profileDto);
            throw new ResourceNotFoundException("Wrong student-profile to delete " + profileDto);
        } catch (Exception e) {
            log.error("Cannot delete student-profile {}", profileDto, e);
            throw new CannotDeleteResourceException("Cannot delete student-profile " + profileDto, e);
        }
    }

    @DeleteMapping("/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<Void> deleteById(@PathVariable(PROFILE_ID_VAR_NAME) String profileId) {
        log.debug("Trying to delete student-profile for Id: '{}'", profileId);
        try {
            final Long id = Long.parseLong(profileId);
            log.debug("Deleting student-profile for id: {}", id);
            if (isInvalid(id)) {
                throw new NumberFormatException();
            }

            facade.deleteById(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | ProfileIsNotFoundException e) {
            log.error("Wrong student profile-id: '{}'", profileId);
            throw new ResourceNotFoundException(WRONG_STUDENT_PROFILE_ID + profileId + "'");
        } catch (Exception e) {
            log.error("Cannot delete student-profile for id = {}", profileId, e);
            throw new CannotDeleteResourceException("Cannot delete student-profile for id = " + profileId, e);
        }
    }


    private static StudentProfileDto toDto(Long profileId, Optional<StudentProfile> profile) {
        log.debug("Converting {} to DTO for student profile-id '{}'", profile, profileId);
        return mapper.toDto(
                profile.orElseThrow(() -> new ResourceNotFoundException("Profile with id: " + profileId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }

}
