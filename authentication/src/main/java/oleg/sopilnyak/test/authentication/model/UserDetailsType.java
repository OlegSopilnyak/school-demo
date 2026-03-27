package oleg.sopilnyak.test.authentication.model;

import oleg.sopilnyak.test.school.common.model.Person;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Model Type: Container for school's authenticated user-details
 *
 * @see UserDetails
 */
public interface UserDetailsType extends UserDetails {
    /**
     * To get ID of person associated with user-details
     *
     * @return person id value
     * @see Person#getId()
     */
    Long getId();
}
