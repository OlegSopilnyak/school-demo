package oleg.sopilnyak.test.endpoint.rest.organization;

import static java.util.Objects.isNull;
import static oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade.CREATE_MACRO;
import static oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade.CREATE_OR_UPDATE;
import static oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade.DELETE_MACRO;
import static oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade.FIND_ALL;
import static oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade.FIND_BY_ID;

import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;

@Slf4j
@AllArgsConstructor
@Getter
@RestController
@RequestMapping(RequestMappingRoot.AUTHORITIES)
@ResponseStatus(HttpStatus.OK)
public class AuthorityPersonsRestController {
    public static final String VAR_NAME = "personId";
    public static final String WRONG_AUTHORITY_PERSON_ID_MESSAGE = "Wrong authority-person-id: '";
    // delegate for requests processing
    private final AuthorityPersonFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping
    @PreAuthorize("hasAuthority('ORG_LIST') and hasAuthority('ORG_GET')")
    public List<AuthorityPersonDto> findAll() {
        log.debug("Trying to get all school's authorities");
        try {
            return resultToDto(facade.<Collection<AuthorityPerson>>doActionAndResult(FIND_ALL));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot get all school's authorities", e);
        }
    }

    @GetMapping("/{" + VAR_NAME + "}")
    @PreAuthorize("hasAuthority('ORG_LIST') and hasAuthority('ORG_GET')")
    public AuthorityPersonDto findById(@PathVariable String personId) {
        log.debug("Trying to get authority person by Id: '{}'", personId);
        try {
            final long id = Long.parseLong(personId);
            log.debug("Getting authority person for id: {}", id);

            return resultToDto(personId, facade.doActionAndResult(FIND_BY_ID, id));
        } catch (NumberFormatException | AuthorityPersonNotFoundException _) {
            throw new AuthorityPersonNotFoundException(WRONG_AUTHORITY_PERSON_ID_MESSAGE + personId + "'");
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot get authority person for id: " + personId, e);
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORG_CREATE') and hasAuthority('ORG_GET')")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorityPersonDto createPerson(@RequestBody @Valid AuthorityPersonDto person, @RequestParam Role role) {
        log.debug("Trying to create the authority person {}", person);
        try {
            return resultToDto(facade.<Optional<AuthorityPerson>>doActionAndResult(CREATE_MACRO, person, role));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot create new authority person " + person.toString(), e);
        }
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ORG_UPDATE') and hasAuthority('ORG_GET')")
    public AuthorityPersonDto updatePerson(@RequestBody @Valid AuthorityPersonDto person) {
        log.debug("Trying to update authority person {}", person);
        try {
            final Long id = person.getId();
            if (isInvalid(id)) {
                throw new AuthorityPersonNotFoundException(WRONG_AUTHORITY_PERSON_ID_MESSAGE + id + "'");
            }
            return resultToDto(facade.<Optional<AuthorityPerson>>doActionAndResult(CREATE_OR_UPDATE, person));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot update authority person " + person.toString(), e);
        }
    }

    @DeleteMapping("/{" + VAR_NAME + "}")
    @PreAuthorize("hasAuthority('ORG_DELETE')")
    public void deletePerson(@PathVariable String personId) {
        log.debug("Trying to delete authority person for Id: '{}'", personId);
        try {
            final long id = Long.parseLong(personId);
            log.debug("Deleting authority person for id: {}", id);
            if (isInvalid(id)) {
                throw new AuthorityPersonNotFoundException(WRONG_AUTHORITY_PERSON_ID_MESSAGE + id + "'");
            }

            facade.doActionAndResult(DELETE_MACRO, id);

        } catch (NumberFormatException | AuthorityPersonNotFoundException _) {
            throw new AuthorityPersonNotFoundException(WRONG_AUTHORITY_PERSON_ID_MESSAGE + personId + "'");
        } catch (Exception e) {
            log.error("Cannot delete authority person for id = {}", personId, e);
            throw new CannotProcessActionException("Cannot delete authority person for id = " + personId, e);
        }
    }

    // private methods
    private AuthorityPersonDto resultToDto(Optional<AuthorityPerson> person) {
        log.debug("Converting {} to DTO", person);
        return mapper.toDto(
                person.orElseThrow(() -> new AuthorityPersonNotFoundException("authority person is not updated"))
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
