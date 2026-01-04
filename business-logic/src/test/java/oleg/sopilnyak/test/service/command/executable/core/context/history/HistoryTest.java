package oleg.sopilnyak.test.service.command.executable.core.context.history;

import static org.assertj.core.api.Assertions.assertThat;

import oleg.sopilnyak.test.service.command.type.base.Context;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryTest {

    History history = History.builder().build();

    @Test
    void shouldAddStateHistories() {
        history.add(Context.State.INIT);
        history.add(Context.State.READY);

        assertThat(history.states()).isEqualTo(List.of(Context.State.INIT, Context.State.READY));
    }

    @Test
    void shouldAddStartedHistories() {
        Instant start = Instant.now();
        history.add(start, Context.State.INIT);
        history.add(start.plusSeconds(10L), Context.State.READY);

        Deque<Instant> started = history.started();

        assertThat(started.getLast()).isEqualTo(started.getFirst().plusSeconds(10L));
    }

    @Test
    void shouldAddWorkedDurationHistories() {
        history.add(Duration.ofSeconds(1), Context.State.FAIL);
        history.add(Duration.ofSeconds(2), Context.State.DONE);
        history.add(Duration.ofSeconds(3), Context.State.UNDONE);

        Deque<Duration> durations = history.durations();

        int seconds = 1;
        assertThat(durations.pop().getSeconds()).isEqualTo(seconds++);
        assertThat(durations.pop().getSeconds()).isEqualTo(seconds++);
        assertThat(durations.pop().getSeconds()).isEqualTo(seconds);
    }
}