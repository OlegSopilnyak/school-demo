package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Type for update school-authority-person command
 */
public interface ChangeAuthorityPersonCommand {
    /**
     * To get reference to command's persistence facade
     *
     * @return reference to the persistence facade
     */
    AuthorityPersonPersistenceFacade getPersistenceFacade();

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To cache into context old value of the authority person instance for possible rollback
     *
     * @param inputId system-id of the authority person
     * @throws NotExistAuthorityPersonException if authority person is not exist
     * @see AuthorityPersonPersistenceFacade
     * @see AuthorityPersonPersistenceFacade#findAuthorityPersonById(Long)
     * @see AuthorityPersonPersistenceFacade#toEntity(AuthorityPerson)
     * @see Context
     * @see Context#setUndoParameter(Object)
     */
    default AuthorityPerson cacheEntityForRollback(Long inputId) throws NotExistAuthorityPersonException {
        final AuthorityPerson existsEntity = getPersistenceFacade().findAuthorityPersonById(inputId)
                .orElseThrow(() -> new NotExistAuthorityPersonException("AuthorityPerson with ID:" + inputId + " is not exists."));
        // return copy of exists entity for undo operation
        return getPersistenceFacade().toEntity(existsEntity);
    }

    /**
     * To restore course entity from cache(context)
     *
     * @param context command execution context
     */
    default void rollbackCachedEntity(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        if (parameter instanceof AuthorityPerson person) {
            getLog().debug("Restoring changed value of the authority person {}", person);
            getPersistenceFacade().save(person);
        }
    }

    /**
     * To persist entity
     *
     * @param context command's do context
     * @return saved instance or empty
     * @see AuthorityPerson
     * @see Optional#empty()
     */
    default Optional<AuthorityPerson> persistRedoEntity(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        if (parameter instanceof AuthorityPerson person) {
            return getPersistenceFacade().save(person);
        } else {
            final String message = "Wrong type of the authority person :" + parameter.getClass().getName();
            final Exception saveError = new NotExistAuthorityPersonException(message);
            saveError.fillInStackTrace();
            getLog().error(message, saveError);
            context.failed(saveError);
            return Optional.empty();
        }
    }
}
