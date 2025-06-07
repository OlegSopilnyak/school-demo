package oleg.sopilnyak.test.school.common.business.facade;

/**
 * Service-Facade: The parent of any Service Facades (has name of the facade)
 *
 * @see ActionContext#getFacadeName()
 */
public interface BusinessFacade {

    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    String getName();
}
