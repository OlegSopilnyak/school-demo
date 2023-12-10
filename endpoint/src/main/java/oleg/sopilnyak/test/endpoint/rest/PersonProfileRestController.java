package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.dto.StudentProfileDto;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.facade.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.PROFILES)
public class PersonProfileRestController {
    public static final String PROFILE_ID_VAR_NAME = "personProfileId";
    private PersonProfileFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping("/student/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<StudentProfileDto> findStudentProfile(@PathVariable(PROFILE_ID_VAR_NAME) String personId) {
        log.debug("Trying to get student profile by Id: '{}'", personId);
        try {
            Long id = Long.parseLong(personId);
            log.debug("Getting student profile for id: {}", id);

            return ResponseEntity.ok(studentResultToDto(personId, facade.findStudentProfileById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong student profile-id: '{}'", personId);
            throw new ResourceNotFoundException("Wrong student profile-id: '" + personId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot get student profile for id: " + personId, e);
        }
    }

    @GetMapping("/principal/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<PrincipalProfileDto> findPrincipalProfile(@PathVariable(PROFILE_ID_VAR_NAME) String personId) {
        log.debug("Trying to get principal profile by Id: '{}'", personId);
        try {
            Long id = Long.parseLong(personId);
            log.debug("Getting principal profile for id: {}", id);

            return ResponseEntity.ok(principalResultToDto(personId, facade.findPrincipalProfileById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong principal profile-id: '{}'", personId);
            throw new ResourceNotFoundException("Wrong principal profile-id: '" + personId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot get principal profile for id: " + personId, e);
        }
    }

    private StudentProfileDto studentResultToDto(String profileId, Optional<StudentProfile> profile) {
        log.debug("Converting {} to DTO for student profile-id '{}'", profile, profileId);
        return mapper.toDto(
                profile.orElseThrow(() -> new ResourceNotFoundException("Profile with id: " + profileId + " is not found"))
        );
    }

    private PrincipalProfileDto principalResultToDto(String profileId, Optional<PrincipalProfile> profile) {
        log.debug("Converting {} to DTO for principal profile-id '{}'", profile, profileId);
        return mapper.toDto(
                profile.orElseThrow(() -> new ResourceNotFoundException("Profile with id: " + profileId + " is not found"))
        );
    }
}
