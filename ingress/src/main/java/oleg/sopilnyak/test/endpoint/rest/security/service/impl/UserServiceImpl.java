package oleg.sopilnyak.test.endpoint.rest.security.service.impl;

import oleg.sopilnyak.test.endpoint.rest.security.service.UserService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
//import com.truongbn.security.repository.UserRepository;
//import com.truongbn.security.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
//    private final UserRepository userRepository;
    @Override
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                return null;
//                return userRepository.findByEmail(username)
//                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            }
        };
    }
}
