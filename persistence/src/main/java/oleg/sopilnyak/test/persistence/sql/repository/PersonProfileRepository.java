package oleg.sopilnyak.test.persistence.sql.repository;

import oleg.sopilnyak.test.persistence.sql.entity.profile.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonProfileRepository<T extends PersonProfileEntity> extends JpaRepository<T, Long> {
    /**
     * To find principal-profile instance by profile's login
     *
     * @param login the value of profile's login to get
     * @return found instance or empty()
     * @see PrincipalProfileEntity
     * @see Optional
     * @see Optional#empty()
     */
    @Query("""
            select profile from PrincipalProfileEntity profile where profile.login=:login
            """)
    Optional<T> findByLogin(@Param("login") String login);
}
