package oleg.sopilnyak.test.authentication.service.impl;


import oleg.sopilnyak.test.authentication.service.UserService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
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
