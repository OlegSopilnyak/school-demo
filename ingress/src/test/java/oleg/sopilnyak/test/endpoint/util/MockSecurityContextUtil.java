package oleg.sopilnyak.test.endpoint.util;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class MockSecurityContextUtil {
    public static void mockingSecurityContext() {
        mockingSecurityContext(null);
    }

    public static void mockingSecurityContext(String username) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        doReturn(username).when(authentication).getName();
        doReturn(true).when(authentication).isAuthenticated();
        doReturn(authentication).when(context).getAuthentication();
        SecurityContextHolder.setContext(context);
    }

    private MockSecurityContextUtil() {
    }
}
