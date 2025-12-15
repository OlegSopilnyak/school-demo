package oleg.sopilnyak.test.service.command.io.result;

import oleg.sopilnyak.test.service.command.io.Output;

import java.util.Optional;

/**
 * Type: I/O school-command optional command execution result
 *
 * @see Output
 * @see Optional
 */
public record OptionalValueResult<T>(Optional<T> value) implements Output<Optional<T>> {
}
