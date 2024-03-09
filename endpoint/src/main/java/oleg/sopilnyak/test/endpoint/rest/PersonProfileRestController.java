package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.CannotDoRestCallException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.PROFILES)
public class PersonProfileRestController {
    public static final String PROFILE_ID_VAR_NAME = "personProfileId";
    public static final String STUDENTS_MAPPING = "/students";
    public static final String PRINCIPALS_MAPPING = "/principals";
    private PersonProfileFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping(STUDENTS_MAPPING + "/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<StudentProfileDto> findStudentProfile(@PathVariable(PROFILE_ID_VAR_NAME) String personId) {
        log.debug("Trying to get student-profile by Id: '{}'", personId);
        try {
            final Long id = Long.parseLong(personId);
            log.debug("Getting student-profile for id: {}", id);

            return ResponseEntity.ok(studentResultToDto(id, facade.findStudentProfileById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong student profile-id: '{}'", personId);
            throw new ResourceNotFoundException("Wrong student profile-id: '" + personId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot get student-profile for id: " + personId, e);
        }
    }

    @GetMapping(PRINCIPALS_MAPPING + "/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<PrincipalProfileDto> findPrincipalProfile(@PathVariable(PROFILE_ID_VAR_NAME) String personId) {
        log.debug("Trying to get principal-profile by Id: '{}'", personId);
        try {
            final Long id = Long.parseLong(personId);
            log.debug("Getting principal profile for id: {}", id);

            return ResponseEntity.ok(principalResultToDto(id, facade.findPrincipalProfileById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong principal profile-id: '{}'", personId);
            throw new ResourceNotFoundException("Wrong principal profile-id: '" + personId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot get principal-profile for id: " + personId, e);
        }
    }

    @PostMapping(STUDENTS_MAPPING)
    public ResponseEntity<StudentProfileDto> create(@RequestBody StudentProfileDto profileDto) {
        log.debug("Trying to create student-profile {}", profileDto);
        try {
            profileDto.setId(null);
            return ResponseEntity.ok(studentResultToDto(null, facade.createOrUpdateProfile(profileDto)));
        } catch (Exception e) {
            log.error("Cannot create new student-profile {}", profileDto.toString(), e);
            throw new CannotDoRestCallException("Cannot create new student-profile " + profileDto, e);
        }
    }

    @PutMapping(STUDENTS_MAPPING)
    public ResponseEntity<StudentProfileDto> update(@RequestBody StudentProfileDto profileDto) {
        log.debug("Trying to update student-profile {}", profileDto);
        try {
            final Long id = profileDto.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException("Wrong student profile-id: '" + id + "'");
            }
            return ResponseEntity.ok(studentResultToDto(id, facade.createOrUpdateProfile(profileDto)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cannot update student-profile {}", profileDto.toString(), e);
            throw new CannotDoRestCallException("Cannot update student-profile " + profileDto, e);
        }
    }

    @PostMapping(PRINCIPALS_MAPPING)
    public ResponseEntity<PrincipalProfileDto> create(@RequestBody PrincipalProfileDto profileDto) {
        log.debug("Trying to create principal-profile {}", profileDto);
        try {
            profileDto.setId(null);
            return ResponseEntity.ok(principalResultToDto(null, facade.createOrUpdateProfile(profileDto)));
        } catch (Exception e) {
            log.error("Cannot create new principal-profile {}", profileDto.toString(), e);
            throw new CannotDoRestCallException("Cannot create new principal-profile " + profileDto, e);
        }
    }

    @PutMapping(PRINCIPALS_MAPPING)
    public ResponseEntity<PrincipalProfileDto> update(@RequestBody PrincipalProfileDto profileDto) {
        log.debug("Trying to update principal-profile {}", profileDto);
        try {
            final Long id = profileDto.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException("Wrong principal profile-id: '" + id + "'");
            }
            return ResponseEntity.ok(principalResultToDto(id, facade.createOrUpdateProfile(profileDto)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cannot update principal-profile {}", profileDto.toString(), e);
            throw new CannotDoRestCallException("Cannot update principal-profile " + profileDto, e);
        }
    }

    @DeleteMapping(STUDENTS_MAPPING)
    public ResponseEntity<Void> deleteStudentProfile(@RequestBody StudentProfileDto profileDto) {
        log.debug("Trying to delete student-profile : {}", profileDto);
        try {
            log.debug("Deleting student-profile : {}", profileDto);

            facade.deleteProfile(profileDto);

            return ResponseEntity.ok().build();
        } catch (ProfileNotExistsException e) {
            log.error("Wrong student-profile to delete {}", profileDto);
            throw new ResourceNotFoundException("Wrong student-profile to delete " + profileDto);
        } catch (Exception e) {
            log.error("Cannot delete student-profile {}", profileDto, e);
            throw new CannotDeleteResourceException("Cannot delete student-profile " + profileDto, e);
        }
    }

    @DeleteMapping(STUDENTS_MAPPING + "/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<Void> deleteStudentProfile(@PathVariable(PROFILE_ID_VAR_NAME) String profileId) {
        log.debug("Trying to delete student-profile for Id: '{}'", profileId);
        try {
            final Long id = Long.parseLong(profileId);
            log.debug("Deleting student-profile for id: {}", id);
            if (isInvalid(id)) {
                throw new NumberFormatException();
            }

            facade.deleteProfileById(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | ProfileNotExistsException e) {
            log.error("Wrong student profile-id: '{}'", profileId);
            throw new ResourceNotFoundException("Wrong student profile-id: '" + profileId + "'");
        } catch (Exception e) {
            log.error("Cannot delete student-profile for id = {}", profileId, e);
            throw new CannotDeleteResourceException("Cannot delete student-profile for id = " + profileId, e);
        }
    }

    @DeleteMapping(PRINCIPALS_MAPPING)
    public ResponseEntity<Void> deletePrincipalProfile(@RequestBody PrincipalProfileDto profileDto) {
        log.debug("Trying to delete principal-profile : {}", profileDto);
        try {
            log.debug("Deleting principal-profile : {}", profileDto);

            facade.deleteProfile(profileDto);

            return ResponseEntity.ok().build();
        } catch (ProfileNotExistsException e) {
            log.error("Wrong principal-profile to delete {}", profileDto);
            throw new ResourceNotFoundException("Wrong principal-profile to delete " + profileDto);
        } catch (Exception e) {
            log.error("Cannot delete principal-profile {}", profileDto, e);
            throw new CannotDeleteResourceException("Cannot delete principal-profile " + profileDto, e);
        }
    }

    @DeleteMapping(PRINCIPALS_MAPPING + "/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<Void> deletePrincipalProfile(@PathVariable(PROFILE_ID_VAR_NAME) String profileId) {
        log.debug("Trying to delete principal-profile for Id: '{}'", profileId);
        try {
            final Long id = Long.parseLong(profileId);
            log.debug("Deleting principal-profile for id: {}", id);
            if (isInvalid(id)) {
                throw new NumberFormatException();
            }

            facade.deleteProfileById(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | ProfileNotExistsException e) {
            log.error("Wrong principal profile-id: '{}'", profileId);
            throw new ResourceNotFoundException("Wrong principal profile-id: '" + profileId + "'");
        } catch (Exception e) {
            log.error("Cannot delete principal-profile for id = {}", profileId, e);
            throw new CannotDeleteResourceException("Cannot delete principal-profile for id = " + profileId, e);
        }
    }

    private StudentProfileDto studentResultToDto(Long profileId, Optional<StudentProfile> profile) {
        log.debug("Converting {} to DTO for student profile-id '{}'", profile, profileId);
        return mapper.toDto(
                profile.orElseThrow(() -> new ResourceNotFoundException("Profile with id: " + profileId + " is not found"))
        );
    }

    private PrincipalProfileDto principalResultToDto(Long profileId, Optional<PrincipalProfile> profile) {
        log.debug("Converting {} to DTO for principal profile-id '{}'", profile, profileId);
        return mapper.toDto(
                profile.orElseThrow(() -> new ResourceNotFoundException("Profile with id: " + profileId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }

}
