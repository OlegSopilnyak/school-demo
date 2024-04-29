package oleg.sopilnyak.test.service.command.type.profile;

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
}
