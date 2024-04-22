package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.CannotDoRestCallException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.business.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.PRINCIPAL_PROFILES)
public class PrincipalProfileRestController {
    public static final String PROFILE_ID_VAR_NAME = "personProfileId";
    private static final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);
    private PrincipalProfileFacade facade;


    @GetMapping("/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<PrincipalProfileDto> findById(@PathVariable(PROFILE_ID_VAR_NAME) String personId) {
        log.debug("Trying to get principal-profile by Id: '{}'", personId);
        try {
            final Long id = Long.parseLong(personId);
            log.debug("Getting principal profile for id: {}", id);

            return ResponseEntity.ok(toDto(id, facade.findPrincipalProfileById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong principal profile-id: '{}'", personId);
            throw new ResourceNotFoundException("Wrong principal profile-id: '" + personId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new CannotDoRestCallException("Cannot get principal-profile for id: " + personId, e);
        }
    }

    @PostMapping
    public ResponseEntity<PrincipalProfileDto> create(@RequestBody PrincipalProfileDto profileDto) {
        log.debug("Trying to create principal-profile {}", profileDto);
        try {
            profileDto.setId(null);
            return ResponseEntity.ok(toDto(null, facade.createOrUpdateProfile(profileDto)));
        } catch (Exception e) {
            log.error("Cannot create new principal-profile {}", profileDto.toString(), e);
            throw new CannotDoRestCallException("Cannot create new principal-profile " + profileDto, e);
        }
    }

    @PutMapping
    public ResponseEntity<PrincipalProfileDto> update(@RequestBody PrincipalProfileDto profileDto) {
        log.debug("Trying to update principal-profile {}", profileDto);
        try {
            final Long id = profileDto.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException("Wrong principal profile-id: '" + id + "'");
            }
            return ResponseEntity.ok(toDto(id, facade.createOrUpdateProfile(profileDto)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cannot update principal-profile {}", profileDto.toString(), e);
            throw new CannotDoRestCallException("Cannot update principal-profile " + profileDto, e);
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody PrincipalProfileDto profileDto) {
        log.debug("Trying to delete principal-profile : {}", profileDto);
        try {
            log.debug("Deleting principal-profile : {}", profileDto);

            facade.delete(profileDto);

            return ResponseEntity.ok().build();
        } catch (NotExistProfileException e) {
            log.error("Wrong principal-profile to delete {}", profileDto);
            throw new ResourceNotFoundException("Wrong principal-profile to delete " + profileDto);
        } catch (Exception e) {
            log.error("Cannot delete principal-profile {}", profileDto, e);
            throw new CannotDeleteResourceException("Cannot delete principal-profile " + profileDto, e);
        }
    }

    @DeleteMapping("/{" + PROFILE_ID_VAR_NAME + "}")
    public ResponseEntity<Void> deleteById(@PathVariable(PROFILE_ID_VAR_NAME) String profileId) {
        log.debug("Trying to delete principal-profile for Id: '{}'", profileId);
        try {
            final Long id = Long.parseLong(profileId);
            log.debug("Deleting principal-profile for id: {}", id);
            if (isInvalid(id)) {
                throw new NumberFormatException();
            }

            facade.deleteById(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | NotExistProfileException e) {
            log.error("Wrong principal profile-id: '{}'", profileId);
            throw new ResourceNotFoundException("Wrong principal profile-id: '" + profileId + "'");
        } catch (Exception e) {
            log.error("Cannot delete principal-profile for id = {}", profileId, e);
            throw new CannotDeleteResourceException("Cannot delete principal-profile for id = " + profileId, e);
        }
    }

    private static PrincipalProfileDto toDto(Long profileId, Optional<PrincipalProfile> profile) {
        log.debug("Converting {} to DTO for principal profile-id '{}'", profile, profileId);
        return mapper.toDto(
                profile.orElseThrow(() -> new ResourceNotFoundException("Profile with id: " + profileId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }

}
