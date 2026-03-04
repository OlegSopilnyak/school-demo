package oleg.sopilnyak.test.service.command.io;


/**
 * Type: I/O school-command array of input parameters as one parameter (for multiple inputs gathered)
 * @see Input
 */
public interface CompositeInput<T>  extends Input<Input<T>[]> {
}
