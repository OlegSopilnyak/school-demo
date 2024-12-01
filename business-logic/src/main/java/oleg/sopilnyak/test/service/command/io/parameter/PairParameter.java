package oleg.sopilnyak.test.service.command.io.parameter;

import oleg.sopilnyak.test.service.command.io.Input;

/**
 * Type: I/O school-command pair input parameter
 */
public interface PairParameter<P> extends Input<PairParameter<P>> {
    String FIRST_FIELD_NAME = "first";
    String SECOND_FIELD_NAME = "second";
    /**
     * To get the value of command input parameter
     *
     * @return value of the parameter
     */
    @Override
    default PairParameter<P> value() {
        return this;
    }

    /**
     * To get the value of first part of the pair
     *
     * @return the value
     */
    P first();

    /**
     * To get the value of second part of the pair
     *
     * @return the value
     */
    P second();

}
