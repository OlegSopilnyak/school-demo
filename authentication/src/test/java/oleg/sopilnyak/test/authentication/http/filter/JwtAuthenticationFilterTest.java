package oleg.sopilnyak.test.authentication.http.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import oleg.sopilnyak.test.authentication.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    JwtService jwtService;
    @Mock
    UserDetailsService users;

    @InjectMocks
    JwtAuthenticationFilter filter;

    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain filterChain;
    @Mock
    UserDetails userDetails;

    String requestHeader = "Authorization";
    String authorizationHeaderPrefix = "Bearer ";
    final String username = "username";
    final String activeToken = "active-token";

    @Test
    void shouldDoFilterInternal_NoAuthentication() throws ServletException, IOException {

        filter.doFilterInternal(request, response, filterChain);

        // check the result
        // check the behavior
        verify(filterChain).doFilter(request, response);
        verify(request).getHeader(requestHeader);
        verifyNoInteractions(jwtService, users);
    }

    @Test
    void shouldDoFilterInternal_WithAuthentication() throws ServletException, IOException {
        String authorization = authorizationHeaderPrefix + activeToken;
        doReturn(authorization).when(request).getHeader(requestHeader);
        doReturn(username).when(jwtService).extractUserName(activeToken);
        doReturn(userDetails).when(users).loadUserByUsername(username);
        doReturn(true).when(jwtService).isTokenValid(activeToken, userDetails);

        filter.doFilterInternal(request, response, filterChain);

        // check the result
        // check the behavior
        verify(request).getHeader(requestHeader);
        verify(jwtService).extractUserName(activeToken);
        verify(users).loadUserByUsername(username);
        verify(jwtService).isTokenValid(activeToken, userDetails);
        verify(userDetails).getAuthorities();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotDoFilterInternal_NoUserDetails() throws ServletException, IOException {
        String authorization = authorizationHeaderPrefix + activeToken;
        doReturn(authorization).when(request).getHeader(requestHeader);
        doReturn(username).when(jwtService).extractUserName(activeToken);

        filter.doFilterInternal(request, response, filterChain);

        // check the result
        // check the behavior
        verify(request).getHeader(requestHeader);
        verify(jwtService).extractUserName(activeToken);
        verify(users).loadUserByUsername(username);
        verify(jwtService, never()).isTokenValid(anyString(), any(UserDetails.class));
        verify(userDetails, never()).getAuthorities();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotDoFilterInternal_InvalidToken() throws ServletException, IOException {
        String authorization = authorizationHeaderPrefix + activeToken;
        doReturn(authorization).when(request).getHeader(requestHeader);
        doReturn(username).when(jwtService).extractUserName(activeToken);
        doReturn(userDetails).when(users).loadUserByUsername(username);

        filter.doFilterInternal(request, response, filterChain);

        // check the result
        // check the behavior
        verify(request).getHeader(requestHeader);
        verify(jwtService).extractUserName(activeToken);
        verify(users).loadUserByUsername(username);
        verify(jwtService).isTokenValid(activeToken, userDetails);
        verify(userDetails, never()).getAuthorities();
        verify(filterChain).doFilter(request, response);
    }
}
