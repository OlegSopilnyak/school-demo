package oleg.sopilnyak.test.service.facade.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.CommandsFactory;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.CommandExecutor.executeSimpleCommand;

/**
 * Service: To process commands for school's person profiles facade
 */
@Slf4j
@AllArgsConstructor
public class PersonProfileFacadeImpl implements ProfileCommandFacade {
    private final CommandsFactory factory;

    /**
     * To get the person's profile by ID
     *
     * @param id system-id of the profile
     * @return profile instance or empty() if not exists
     * @see PersonProfile
     * @see PersonProfile#getId()
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PersonProfile> findById(Long id) {
        return executeSimpleCommand(FIND_BY_ID, id, factory);
    }

    /**
     * To get the student's profile by ID
     *
     * @param id system-id of the student profile
     * @return profile instance or empty() if not exists
     * @see StudentProfile
     * @see StudentProfile#getId()
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentProfile> findStudentProfileById(Long id) {
        return executeSimpleCommand(FIND_STUDENT_BY_ID, id, factory);
    }

    /**
     * To get the principal's profile by ID
     *
     * @param id system-id of the principal profile
     * @return profile instance or empty() if not exists
     * @see PrincipalProfile
     * @see PrincipalProfile#getId()
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<PrincipalProfile> findPrincipalProfileById(Long id) {
        return executeSimpleCommand(FIND_PRINCIPAL_BY_ID, id, factory);
    }
}
