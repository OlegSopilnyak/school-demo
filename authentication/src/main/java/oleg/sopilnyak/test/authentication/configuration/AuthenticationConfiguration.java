package oleg.sopilnyak.test.authentication.configuration;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import oleg.sopilnyak.test.authentication.AuthenticationFacadeImpl;
import oleg.sopilnyak.test.authentication.http.filter.JwtAuthenticationFilter;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.TokenStorage;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.authentication.service.impl.AuthEntryPointJwt;
import oleg.sopilnyak.test.authentication.service.impl.JwtServiceImpl;
import oleg.sopilnyak.test.authentication.service.impl.TokenStorageImpl;
import oleg.sopilnyak.test.authentication.service.impl.UserServiceImpl;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;

import jakarta.servlet.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthenticationConfiguration {
    private final ProfilePersistenceFacade profilePersistenceFacade;
    @Bean
    public JwtService jwtService() {
        return new JwtServiceImpl();
    }

    @Bean
    public TokenStorage tokenStorage() {
        return new TokenStorageImpl(jwtService());
    }

    @Bean
    public UserService userService() {
        return new UserServiceImpl(profilePersistenceFacade);
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedHandler() {
        return new AuthEntryPointJwt();
    }

    @Bean
    public Filter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService(), userService());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF
                .cors(AbstractHttpConfigurer::disable) // Disable CORS (or configure if needed)
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(unauthorizedHandler())
                )
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(
                        sessionManagement -> sessionManagement.sessionCreationPolicy(STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationFacade  authenticationFacade() {
        return new AuthenticationFacadeImpl(userService(), jwtService(), tokenStorage());
    }
}
