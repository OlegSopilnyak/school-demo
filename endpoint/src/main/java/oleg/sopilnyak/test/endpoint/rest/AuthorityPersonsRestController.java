package oleg.sopilnyak.test.endpoint.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.endpoint.dto.AuthorityPersonDto;
import oleg.sopilnyak.test.endpoint.exception.CannotDeleteResourceException;
import oleg.sopilnyak.test.endpoint.exception.ResourceNotFoundException;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonIsNotExistsException;
import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.AUTHORITIES)
public class AuthorityPersonsRestController {
    public static final String VAR_NAME = "personId";
    // delegate for requests processing
    private final OrganizationFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @GetMapping
    public ResponseEntity<List<AuthorityPersonDto>> findAllAuthorities() {
        log.debug("Trying to get all school's authorities");
        try {
            return ResponseEntity.ok(resultToDto(facade.findAllAuthorityPerson()));
        } catch (Exception e) {
            throw new RuntimeException("Cannot get all school's authorities", e);
        }
    }

    @GetMapping("/{" + VAR_NAME + "}")
    public ResponseEntity<AuthorityPersonDto> findById(@PathVariable(VAR_NAME) String personId) {
        log.debug("Trying to get authority person by Id: '{}'", personId);
        try {
            Long id = Long.parseLong(personId);
            log.debug("Getting authority person for id: {}", id);

            return ResponseEntity.ok(resultToDto(personId, facade.getAuthorityPersonById(id)));
        } catch (NumberFormatException e) {
            log.error("Wrong authority-person-id: '{}'", personId);
            throw new ResourceNotFoundException("Wrong authority-person-id: '" + personId + "'");
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get authority person for id: " + personId, e);
        }
    }

    @PostMapping
    public ResponseEntity<AuthorityPersonDto> createPerson(@RequestBody AuthorityPersonDto personDto) {
        log.debug("Trying to create the authority person {}", personDto);
        try {
            return ResponseEntity.ok(resultToDto(facade.createOrUpdateAuthorityPerson(personDto)));
        } catch (Exception e) {
            throw new RuntimeException("Cannot create new authority person " + personDto.toString(), e);
        }
    }

    @PutMapping
    public ResponseEntity<AuthorityPersonDto> updatePerson(@RequestBody AuthorityPersonDto personDto) {
        log.debug("Trying to update authority person {}", personDto);
        try {
            Long id = personDto.getId();
            if (isInvalid(id)) {
                throw new ResourceNotFoundException("Wrong authority-person-id: '" + id + "'");
            }
            return ResponseEntity.ok(resultToDto(facade.createOrUpdateAuthorityPerson(personDto)));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot update authority person " + personDto.toString());
        }
    }

    @DeleteMapping("/{" + VAR_NAME + "}")
    public ResponseEntity<Void> deletePerson(@PathVariable(VAR_NAME) String personId) {
        log.debug("Trying to delete authority person for Id: '{}'", personId);
        try {
            Long id = Long.parseLong(personId);
            log.debug("Deleting course for id: {}", id);
            if (isInvalid(id)) {
                throw new NumberFormatException();
            }

            facade.deleteAuthorityPersonById(id);

            return ResponseEntity.ok().build();
        } catch (NumberFormatException | AuthorityPersonIsNotExistsException e) {
            log.error("Wrong authority-person-id: '{}'", personId);
            throw new ResourceNotFoundException("Wrong authority-person-id: '" + personId + "'");
        } catch (Exception e) {
            log.error("Cannot delete authority person for id = {}", personId, e);
            throw new CannotDeleteResourceException("Cannot delete authority person for id = " + personId, e);
        }
    }

    // private methods
    private AuthorityPersonDto resultToDto(Optional<AuthorityPerson> course) {
        log.debug("Converting {} to DTO", course);
        return mapper.toDto(
                course.orElseThrow(() -> new ResourceNotFoundException("authority person is not updated"))
        );
    }

    private AuthorityPersonDto resultToDto(String personId, Optional<AuthorityPerson> person) {
        log.debug("Converting {} to DTO for authority-person-id '{}'", person, personId);
        return mapper.toDto(
                person.orElseThrow(() -> new ResourceNotFoundException("AuthorityPerson with id: " + personId + " is not found"))
        );
    }

    private List<AuthorityPersonDto> resultToDto(Collection<AuthorityPerson> persons) {
        return persons.stream()
                .map(mapper::toDto)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AuthorityPerson::getFullName))
                .toList();
    }

    private static boolean isInvalid(Long id) {
        return isNull(id) || id <= 0;
    }

}
