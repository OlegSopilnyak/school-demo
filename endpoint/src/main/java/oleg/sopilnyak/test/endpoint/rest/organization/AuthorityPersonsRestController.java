package oleg.sopilnyak.test.endpoint.rest.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.organization.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.AUTHORITIES)
@ResponseStatus(HttpStatus.OK)
public class AuthorityPersonsRestController {
    public static final String FACADE_NAME = "AuthorityPersonFacade";
    public static final String USER_NAME = "username";
    public static final String PASS_NAME = "password";
    public static final String VAR_NAME = "personId";
    public static final String WRONG_AUTHORITY_PERSON_ID_MESSAGE = "Wrong authority-person-id: '";
    public static final String BEARER_PREFIX = "Bearer ";
    // delegate for requests processing
    private final AuthorityPersonFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @PostMapping("/login")
    public AuthorityPersonDto login(@RequestParam(USER_NAME) String username,
                                    @RequestParam(PASS_NAME) String password) {
        ActionContext.setup(FACADE_NAME, "login");
        log.debug("Trying to login authority person with login: '{}'", username);
        try {
            return resultToDto(username, facade.login(username, password));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot login authority person for login: " + username, e);
        }
    }

    @DeleteMapping("/logout")
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ActionContext.setup(FACADE_NAME, "logout");
        log.debug("Trying to logout authority person using: '{}'", authorization);
        if (!ObjectUtils.isEmpty(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            final String token = authorization.substring(7);
            log.debug("Trying to logout authority person with token: '{}'", token);

            facade.logout(token);

            log.debug("Authority person is logged out.");
        }
    }

    @GetMapping
    public List<AuthorityPersonDto> findAll() {
        ActionContext.setup(FACADE_NAME, "findAll");
        log.debug("Trying to get all school's authorities");
        try {
            return resultToDto(facade.findAllAuthorityPersons());
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot get all school's authorities", e);
        }
    }

    @GetMapping("/{" + VAR_NAME + "}")
    public AuthorityPersonDto findById(@PathVariable(VAR_NAME) String personId) {
        ActionContext.setup(FACADE_NAME, "findById");
        log.debug("Trying to get authority person by Id: '{}'", personId);
        try {
            final long id = Long.parseLong(personId);
            log.debug("Getting authority person for id: {}", id);

            return resultToDto(personId, facade.findAuthorityPersonById(id));
        } catch (NumberFormatException | AuthorityPersonNotFoundException e) {
            throw new AuthorityPersonNotFoundException(WRONG_AUTHORITY_PERSON_ID_MESSAGE + personId + "'");
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot get authority person for id: " + personId, e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorityPersonDto createPerson(@RequestBody AuthorityPersonDto person) {
        ActionContext.setup(FACADE_NAME, "createNew");
        log.debug("Trying to create the authority person {}", person);
        try {
            return resultToDto(facade.create(person));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot create new authority person " + person.toString(), e);
        }
    }

    @PutMapping
    public AuthorityPersonDto updatePerson(@RequestBody AuthorityPersonDto person) {
        ActionContext.setup(FACADE_NAME, "createOrUpdate");
        log.debug("Trying to update authority person {}", person);
        try {
            final Long id = person.getId();
            if (isInvalid(id)) {
                throw new AuthorityPersonNotFoundException(WRONG_AUTHORITY_PERSON_ID_MESSAGE + id + "'");
            }
            return resultToDto(facade.createOrUpdateAuthorityPerson(person));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot update authority person " + person.toString(), e);
        }
    }

    @DeleteMapping("/{" + VAR_NAME + "}")
    public void deletePerson(@PathVariable(VAR_NAME) String personId) {
        ActionContext.setup(FACADE_NAME, "deleteById");
        log.debug("Trying to delete authority person for Id: '{}'", personId);
        try {
            final long id = Long.parseLong(personId);
            log.debug("Deleting authority person for id: {}", id);
            if (isInvalid(id)) {
                throw new AuthorityPersonNotFoundException(WRONG_AUTHORITY_PERSON_ID_MESSAGE + id + "'");
            }

            facade.deleteAuthorityPersonById(id);

        } catch (NumberFormatException | AuthorityPersonNotFoundException e) {
            throw new AuthorityPersonNotFoundException(WRONG_AUTHORITY_PERSON_ID_MESSAGE + personId + "'");
        } catch (Exception e) {
            log.error("Cannot delete authority person for id = {}", personId, e);
            throw new CannotProcessActionException("Cannot delete authority person for id = " + personId, e);
        }
    }

    // private methods
    private AuthorityPersonDto resultToDto(Optional<AuthorityPerson> course) {
        log.debug("Converting {} to DTO", course);
        return mapper.toDto(
                course.orElseThrow(() -> new AuthorityPersonNotFoundException("authority person is not updated"))
        );
    }

    private AuthorityPersonDto resultToDto(String personId, Optional<AuthorityPerson> person) {
        log.debug("Converting {} to DTO for authority-person-id '{}'", person, personId);
        return mapper.toDto(
                person.orElseThrow(
                        () -> new AuthorityPersonNotFoundException("AuthorityPerson with id: " + personId + " is not found"))
        );
    }

    private List<AuthorityPersonDto> resultToDto(Collection<AuthorityPerson> persons) {
        return persons.stream().map(mapper::toDto).filter(Objects::nonNull)
                .sorted(Comparator.comparing(AuthorityPerson::getFullName))
                .toList();
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0L;
    }

}
