package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.io.result.EmptyResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadSetResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.util.Set;

import static java.util.Objects.isNull;


/**
 * Type: I/O school-command execution result
 *
 * @param <O> the type of command execution result value
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#executeDo(Context)
 * @see Context#setResult(Object)
 */
public interface Output<O> extends IOBase<O> {

    /**
     * To create empty result output
     *
     * @return new instance of the output
     * @see EmptyResult
     */
    static Output<Void> empty() {
        return new EmptyResult();
    }

    /**
     * To create boolean result output
     *
     * @return new instance of the output
     * @see BooleanResult
     */
    static Output<Boolean> of(final Boolean result) {
        return new BooleanResult(result);
    }

    /**
     * To create payload result output
     *
     * @return new instance of the output
     * @see PayloadResult
     * @see BasePayload
     * @see BaseType
     */
    static <P extends BasePayload<? extends BaseType>> Output<P> of(final P result) {
        return new PayloadResult<>(result);
    }

    /**
     * To create payload-set result output
     *
     * @return new instance of the output
     * @see PayloadSetResult
     * @see Set
     * @see BasePayload
     * @see BaseType
     */
    static <P extends BasePayload<? extends BaseType>> Output<Set<P>> of(final Set<P> result) {
        return new PayloadSetResult<>(result);
    }
}
