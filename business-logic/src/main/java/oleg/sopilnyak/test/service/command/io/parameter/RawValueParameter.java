package oleg.sopilnyak.test.service.command.io.parameter;

import oleg.sopilnyak.test.service.command.io.Input;

/**
 * Type: I/O school-command raw value command input parameter (for mocks)
 *
 * @see Input
 * @see Object
 * @see org.mockito.internal.util.MockUtil#isMock(Object)
 */
public record RawValueParameter(Object value) implements Input<Object> {
}
