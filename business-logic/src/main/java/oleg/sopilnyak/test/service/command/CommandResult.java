package oleg.sopilnyak.test.service.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandResult<T> {
    private Optional<T> result;
    private Exception exception;
    private boolean success;
}
