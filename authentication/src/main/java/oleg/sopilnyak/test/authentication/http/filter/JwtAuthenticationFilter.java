package oleg.sopilnyak.test.authentication.http.filter;

import oleg.sopilnyak.test.authentication.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Predicate;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Predicate<String> EMPTY_HEADER = ObjectUtils::isEmpty;
    private static final Predicate<String> HEADER_WITH_BEARER = header -> header.toUpperCase().startsWith(BEARER_PREFIX.toUpperCase());
    // JWT management service reference
    private final JwtService jwtService;
    // user-details service reference
    private final UserDetailsService users;

    /**
     * Same contract as for {@code doFilter}, but guaranteed to be
     * just invoked once per request within a single request thread.
     * See {@link #shouldNotFilterAsyncDispatch()} for details.
     * <p>Provides HttpServletRequest and HttpServletResponse arguments instead of the
     * default ServletRequest and ServletResponse ones.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (EMPTY_HEADER.or(HEADER_WITH_BEARER.negate()).test(authHeader)) {
            // there is no valid Authorization:Bearer header in request
            // do default filter-chain flow
            filterChain.doFilter(request, response);
            return;
        }
        // preparing security context for accepted request
        final String jsonWebToken = authHeader.substring(BEARER_PREFIX.length());
        // extracting username from the token
        final String userName = jwtService.extractUserName(jsonWebToken);
        if (!ObjectUtils.isEmpty(userName) && SecurityContextHolder.getContext().getAuthentication() == null) {
            // retrieving user-details for valid access token
            final UserDetails userDetails = users.loadUserByUsername(userName);
            if (jwtService.isTokenValid(jsonWebToken, userDetails)) {
                final var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                final var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
        }
        filterChain.doFilter(request, response);
    }
}
