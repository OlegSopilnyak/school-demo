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
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/refresh")
    public AccessCredentialsDto refresh(@RequestParam(TOKEN_NAME) String refreshToken) {
        log.debug("Trying to refresh access token using: '{}'", refreshToken);
        try {
            return resultToDto(facade.refresh(refreshToken));
        } catch (Exception e) {
            throw new CannotProcessActionException("Cannot refresh access token using: " + refreshToken, e);
        }
    }

    @PreAuthorize("isFullyAuthenticated()")
    @DeleteMapping("/logout")
    public void logout() {
        final String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Trying to sign out authority person with login: '{}'", username);

        personFacade.doActionAndResult(LOGOUT, username);

        log.debug("Authority person with login: '{}' is signed out.", username );
    }

    // private methods
    private AccessCredentialsDto resultToDto(Optional<AccessCredentials> credentials) {
        log.debug("Converting {} to DTO", credentials);
        return mapper.toDto(credentials
                .orElseThrow(() -> new SchoolAccessDeniedException("authority person is not authorized"))
        );
    }
}
