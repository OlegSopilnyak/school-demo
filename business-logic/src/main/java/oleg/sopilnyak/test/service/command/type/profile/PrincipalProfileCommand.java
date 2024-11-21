package oleg.sopilnyak.test.service.command.type.profile;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

/**
 * Type for school-principal-profile commands
 */
public interface PrincipalProfileCommand<T> extends ProfileCommand<T> {
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
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(PrincipalProfileCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Object macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
