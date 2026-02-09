package oleg.sopilnyak.test.endpoint.rest.security.service;

import oleg.sopilnyak.test.endpoint.rest.security.service.dao.request.SignUpRequest;
import oleg.sopilnyak.test.endpoint.rest.security.service.dao.request.SigninRequest;
import oleg.sopilnyak.test.endpoint.rest.security.service.dao.response.JwtAuthenticationResponse;

@Deprecated
public interface AuthenticationService {
    JwtAuthenticationResponse signup(SignUpRequest request);

    JwtAuthenticationResponse signin(SigninRequest request);
}
