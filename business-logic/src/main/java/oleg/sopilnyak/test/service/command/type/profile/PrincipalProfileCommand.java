package oleg.sopilnyak.test.service.command.type.profile;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.composite.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

/**
 * Type for school-principal-profile commands
 */
public interface PrincipalProfileCommand extends ProfileCommand {
    /**
     * ID of findById principal profile command
     */
    String FIND_BY_ID = "profile.principal.findById";
    /**
     * ID of deleteById principal profile command
     */
    String DELETE_BY_ID = "profile.principal.deleteById";
    /**
     * ID of createOrUpdate principal profile command
     */
    String CREATE_OR_UPDATE = "profile.principal.createOrUpdate";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "principalProfileCommandsFactory";

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(PrincipalProfileCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default <T> Context<T> acceptPreparedContext(PrepareContextVisitor visitor, Object input) {
        return visitor.prepareContext(this, input);
    }
}
