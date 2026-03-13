package oleg.sopilnyak.test.endpoint.rest.authentication;

import static oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade.LOGIN;
import static oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade.LOGOUT;

import oleg.sopilnyak.test.endpoint.dto.AccessCredentialsDto;
import oleg.sopilnyak.test.endpoint.mapper.EndpointMapper;
import oleg.sopilnyak.test.endpoint.rest.RequestMappingRoot;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;

@Slf4j
@Getter
@AllArgsConstructor
@RestController
@RequestMapping(RequestMappingRoot.AUTHENTICATION)
@ResponseStatus(HttpStatus.OK)
public class AuthenticationRestController {
    private static final String USER_NAME = "username";
    private static final String PASS_NAME = "password";
    private static final String TOKEN_NAME = "token";
    // delegate for requests processing through commands' execution engine
    private final AuthorityPersonFacade personFacade;
    // security access layer of the school application
    private final AuthenticationFacade facade;
    private final EndpointMapper mapper = Mappers.getMapper(EndpointMapper.class);

    @PostMapping("/login")
    public AccessCredentialsDto login(
            @RequestParam(USER_NAME) String username, @RequestParam(PASS_NAME) String password
    ) {
        log.debug("Trying to login authority person with login: '{}'", username);
        try {
            return resultToDto(personFacade.doActionAndResult(LOGIN, username, password));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot login authority person for login: " + username, e);
        }
    }

    @PreAuthorize("isFullyAuthenticated()")
    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestParam(TOKEN_NAME) String refreshToken) {
        log.debug("Trying to refresh access token using: '{}'", refreshToken);
        try {
            final SecurityContext securityContext = SecurityContextHolder.getContext();
            if (securityContext == null) {
                return new ResponseEntity<>("SecurityContext is empty", HttpStatus.UNAUTHORIZED);
            }
            final Authentication authentication = securityContext.getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>("Authentication is empty", HttpStatus.UNAUTHORIZED);
            }
            final String username = authentication.getName();
            if (username == null) {
                throw new SchoolAccessDeniedException("Authentication username is empty");
            }
            log.debug("Trying to refresh token of user with login: '{}'", username);
            return ResponseEntity.ok(resultToDto(facade.refresh(refreshToken, username)));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot refresh access token using: " + refreshToken, e);
        }
    }

    @PreAuthorize("isFullyAuthenticated()")
    @DeleteMapping("/logout")
    public ResponseEntity<String> logout() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext == null) {
            return new ResponseEntity<>("SecurityContext is empty", HttpStatus.UNAUTHORIZED);
        }
        final Authentication authentication = securityContext.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("Authentication is empty", HttpStatus.UNAUTHORIZED);
        }
        final String username = authentication.getName();
        if (username == null) {
            return new ResponseEntity<>("Authentication username is empty", HttpStatus.UNAUTHORIZED);
        }
        log.debug("Trying to sign out authority person with login: '{}'", username);

        final Optional<AccessCredentials> result = personFacade.doActionAndResult(LOGOUT, username);

        if (result.isEmpty()) {
            log.warn("No signed in person with username {}", username);
            return new ResponseEntity<>("Not signed in person with username " + username, HttpStatus.NOT_FOUND);
        }

        log.debug("Authority person with login: '{}' is signed out.", username );
        return  ResponseEntity.ok().build();
    }

    // private methods
    private AccessCredentialsDto resultToDto(Optional<AccessCredentials> credentials) {
        log.debug("Converting {} to DTO", credentials);
        return mapper.toDto(credentials
                .orElseThrow(() -> new SchoolAccessDeniedException("Authority Person is not authorized"))
        );
    }
}
