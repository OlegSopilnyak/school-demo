package oleg.sopilnyak.test.endpoint.rest.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.profile.PrincipalProfileDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.PRINCIPAL_PROFILES)
@ResponseStatus(HttpStatus.OK)
public class PrincipalProfileRestController {
    private static final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);
    public static final String FACADE_NAME = "PrincipalProfileFacade";
    public static final String PROFILE_ID_VAR_NAME = "personProfileId";
    public static final String WRONG_PRINCIPAL_PROFILE_ID = "Wrong principal profile-id: '";
    private PrincipalProfileFacade facade;


    @GetMapping("/{" + PROFILE_ID_VAR_NAME + "}")
    public PrincipalProfileDto findById(@PathVariable(PROFILE_ID_VAR_NAME) String personId) {
        ActionContext.setup(FACADE_NAME, "findById");
        log.debug("Trying to get principal-profile by Id: '{}'", personId);
        try {
            final Long id = Long.parseLong(personId);
            log.debug("Getting principal profile for id: {}", id);

            return toDto(id, facade.findPrincipalProfileById(id));
        } catch (NumberFormatException e) {
            throw new ProfileNotFoundException(WRONG_PRINCIPAL_PROFILE_ID + personId + "'");
        } catch (Exception e) {
            log.error("Cannot get principal-profile for id: {}", personId, e);
            throw new CannotProcessActionException("Cannot get principal-profile for id: " + personId, e);
        }
    }

    @PutMapping
    public PrincipalProfileDto update(@RequestBody PrincipalProfileDto profileDto) {
        ActionContext.setup(FACADE_NAME, "updateExists");
        log.debug("Trying to update principal-profile {}", profileDto);
        try {
            final Long id = profileDto.getId();
            if (isInvalid(id)) {
                throw new ProfileNotFoundException(WRONG_PRINCIPAL_PROFILE_ID + id + "'");
            }
            return toDto(id, facade.createOrUpdateProfile(profileDto));
        } catch (Exception e) {
            log.error("Cannot update principal-profile {}", profileDto.toString(), e);
            throw new CannotProcessActionException("Cannot update principal-profile " + profileDto, e);
        }
    }

    // private methods
    private static PrincipalProfileDto toDto(Long profileId, Optional<PrincipalProfile> profile) {
        log.debug("Converting {} to DTO for principal profile-id '{}'", profile, profileId);
        return mapper.toDto(
                profile.orElseThrow(() -> new ProfileNotFoundException("Profile with id: " + profileId + " is not found"))
        );
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }

}
