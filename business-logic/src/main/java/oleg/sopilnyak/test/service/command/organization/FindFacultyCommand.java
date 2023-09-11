package oleg.sopilnyak.test.service.command.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to get faculty by id
 */
@Slf4j
@AllArgsConstructor
public class FindFacultyCommand implements SchoolCommand<Optional<Faculty>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To find faculty by id
     *
     * @param parameter system faculty-id
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<Faculty>> execute(Object parameter) {
        try {
            log.debug("Trying to find faculty by ID:{}", parameter);
            Long id = (Long) parameter;
            Optional<Faculty> faculty = persistenceFacade.findFacultyById(id);
            log.debug("Got faculty {} by ID:{}", faculty, id);
            return CommandResult.<Optional<Faculty>>builder()
                    .result(Optional.ofNullable(faculty))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the faculty by ID:{}", parameter, e);
            return CommandResult.<Optional<Faculty>>builder()
                    .result(Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
    }
}
