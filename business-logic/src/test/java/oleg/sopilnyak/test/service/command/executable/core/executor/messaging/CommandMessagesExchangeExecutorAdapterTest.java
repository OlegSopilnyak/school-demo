package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;
import lombok.experimental.SuperBuilder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class CommandMessagesExchangeExecutorAdapterTest {
    public static final String REQUESTS_PROCESSOR_NAME = "requests-processor";
    public static final String RESPONSES_PROCESSOR_NAME = "responses-processor";
    @Mock
    Logger logger;
    AtomicBoolean serviceActive = spy(new AtomicBoolean(false));
    @Mock
    CommandMessage<?> request;
    @Mock
    Context context;
    @Mock
    RootCommand command;

    MessagesProcessor requestsProcessor;
    MessagesProcessor responsesProcessor;
    CommandMessagesExchangeExecutorStub messagesExecutor;

    @BeforeEach
    void setUp() {
        messagesExecutor = spy(new CommandMessagesExchangeExecutorStub());
        messagesExecutor.initialize();
        serviceActive = (AtomicBoolean) ReflectionTestUtils.getField(messagesExecutor, "serviceActive");
        requestsProcessor = (MessagesProcessor) ReflectionTestUtils.getField(messagesExecutor, "requestsProcessor");
        responsesProcessor = (MessagesProcessor) ReflectionTestUtils.getField(messagesExecutor, "responsesProcessor");
        reset(messagesExecutor, logger);
    }

    @AfterEach
    void tearDown() {
        messagesExecutor.shutdown();
    }

    @Test
    void shouldBeActive() {
        serviceActive.getAndSet(true);

        assertThat(messagesExecutor.isActive()).isTrue();
    }

    @Test
    void shouldNotBeActive() {
        serviceActive.getAndSet(false);

        assertThat(messagesExecutor.isActive()).isFalse();
    }

    @Test
    void shouldInitialize() {
        // Init
        serviceActive.getAndSet(false);

        // Act
        messagesExecutor.initialize();

        requestsProcessor = (MessagesProcessor) ReflectionTestUtils.getField(messagesExecutor, "requestsProcessor");
        responsesProcessor = (MessagesProcessor) ReflectionTestUtils.getField(messagesExecutor, "responsesProcessor");
        // Verification
        verify(messagesExecutor, atLeastOnce()).isActive();
        verify(logger).info(startsWith("Initializing school-commands"));
        verify(messagesExecutor).buildExecutorService();
        verify(messagesExecutor).initializeTakenMessagesExecutor();
        verify(messagesExecutor).prepareRequestsProcessor();
        verify(messagesExecutor).prepareResponsesProcessor();
        verify(logger).info(startsWith("The executor is"));
        // Check data updates
        assertThat(messagesExecutor.isActive()).isTrue();
        assertThat(requestsProcessor.isProcessorActive()).isTrue();
        assertThat(responsesProcessor.isProcessorActive()).isTrue();
    }

    @Test
    void shouldNotInitialize_AlreadyInitialized() {
        // Init
        serviceActive.getAndSet(true);

        // Act
        messagesExecutor.initialize();

        // Verification
        verify(messagesExecutor, atLeastOnce()).isActive();
        verify(logger).warn(startsWith("The executor is already"));
        verify(logger, never()).info(anyString());
        verify(messagesExecutor, never()).buildExecutorService();
    }

    @Test
    void shouldShutdown() {
        // Init
        serviceActive.getAndSet(true);

        // Act
        messagesExecutor.shutdown();

        // Verification
        verify(messagesExecutor, atLeastOnce()).isActive();
        verify(logger).info(startsWith("Shutting down school-commands"));
        verify(requestsProcessor).shutdown();
        verify(responsesProcessor).shutdown();
        verify(messagesExecutor).shutdownTakenMessagesExecutor();
        // Check data updates
        assertThat(messagesExecutor.isActive()).isFalse();
        assertThat(ReflectionTestUtils.getField(messagesExecutor, "requestsProcessor")).isNull();
        assertThat(ReflectionTestUtils.getField(messagesExecutor, "responsesProcessor")).isNull();
    }

    @Test
    void shouldNotShutdown_AlreadyShutdown() {
        // Init
        serviceActive.getAndSet(false);

        // Act
        messagesExecutor.shutdown();

        // Verification
        verify(messagesExecutor, atLeastOnce()).isActive();
        verify(logger).warn("The executor is already stopped.");
        verify(logger, never()).info(anyString());
        verify(requestsProcessor, never()).shutdown();
        verify(responsesProcessor, never()).shutdown();
        verify(messagesExecutor, never()).shutdownTakenMessagesExecutor();
    }

    @Test
    void shouldProcessActionCommand() throws InterruptedException {
        // Init
        ActionContext actionContext = ActionContext.setup("test-facade", "test-action");
        String commandId = "command-id";
        String correlationId = "correlation-id";
        doReturn(actionContext).when(request).getActionContext();
        doReturn(correlationId).when(request).getCorrelationId();
        doReturn(CommandMessage.Direction.DO).when(request).getDirection();
        doReturn(context).when(request).getContext();
        doReturn(command).when(context).getCommand();
        doReturn(commandId).when(command).getId();

        // Act
        messagesExecutor.processActionCommand(request);

        // Verification
        // all processors are launched and ready to process messages
        verify(requestsProcessor, atLeastOnce()).isProcessorActive();
        verify(requestsProcessor, atLeastOnce()).takeMessage();
        verify(responsesProcessor, atLeastOnce()).isProcessorActive();
        verify(responsesProcessor, atLeastOnce()).takeMessage();
        // processActionCommand
        verify(logger).debug(startsWith("Validating input command-message before"));
        verify(messagesExecutor).validateInput(request);
        verify(logger).info(startsWith("=== Sending command-message to start "), eq(correlationId));
        // launchCommandMessageProcessing
        verify(messagesExecutor).makeMessageInProgress(correlationId, request);
        // launchCommandMessageProcessing::initiateProcessingMessage
        verify(requestsProcessor).accept(request);
        verify(logger).info(startsWith("Launch: message with correlationId="), eq(correlationId));
        verify(requestsProcessor).runAsyncTakenMessage(any(Consumer.class ), eq(request));
        verify(requestsProcessor).onTakenMessage(request);
        verify(logger, times(2)).debug("Taken message {}", request);
        verify(messagesExecutor).executeWithActionContext(request);
        verify(messagesExecutor).onTakenRequestMessage(request);
        verify(messagesExecutor, never()).onErrorRequestMessage(any(CommandMessage.class), any(Throwable.class));
        verify(logger).debug("Processing request message with correlationId='{}'", correlationId);
        verify(messagesExecutor).localExecutionResult(request);
        verify(command).doCommand(context);
        verify(logger).debug("Processed request message with correlationId='{}'", correlationId);
        verify(logger).debug("Result: message with correlationId='{}' is processed and put to responses processor", correlationId);
        verify(responsesProcessor).accept(request);
        // waitingProcessedCommandMessage
        verify(logger).info(startsWith("=== Waiting for processed command message of "), eq(commandId), eq(correlationId));
        verify(responsesProcessor).runAsyncTakenMessage(any(Consumer.class ), eq(request));
//        verify(responsesProcessor).runAsyncTakenMessage(any(Runnable.class));
        verify(responsesProcessor).onTakenMessage(request);
        verify(messagesExecutor).onTakenResponseMessage(request);
        verify(logger).info("Successfully processed response with correlationId='{}'", correlationId);
        // waitingProcessedCommandMessage::retrieveProcessedMessage
        verify(messagesExecutor, times(3)).messageWatchdogFor(correlationId);
        verify(messagesExecutor).stopWatchingMessage(correlationId);
        verify(logger).debug("= Retrieve: the result of command '{}' after processing is {}", commandId, request);
    }

    @Test
    void shouldNotProcessActionCommand_InvalidMessageDirection_Null() {
        // Init
        var message = CommandMessage.EMPTY;

        // Act
        var exception = assertThrows(Exception.class, () -> messagesExecutor.processActionCommand(message));

        // Verification
        verify(logger).debug(startsWith("Validating input command-message before"));
        verify(messagesExecutor).validateInput(message);
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getMessage()).isEqualTo("Message direction is not defined.");
    }

    @Test
    void shouldNotProcessActionCommand_InvalidMessageDirection_Unknown() {
        // Init
        var message = new BaseCommandMessage<>(null, null, null) {
            @Override
            public Direction getDirection() {
                return Direction.UNKNOWN;
            }
        };

        // Act
        var exception = assertThrows(Exception.class, () -> messagesExecutor.processActionCommand(message));

        // Verification
        verify(logger).debug(startsWith("Validating input command-message before"));
        verify(messagesExecutor).validateInput(message);
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getMessage()).isEqualTo("Unknown message direction: UNKNOWN");
    }

    @Test
    void shouldNotProcessActionCommand_DidNotLaunchRequestsProcessing() {
        // Init
        String commandId = "command-id";
        String correlationId = "correlation-id";
        doReturn(correlationId).when(request).getCorrelationId();
        doReturn(CommandMessage.Direction.DO).when(request).getDirection();
        doReturn(context).when(request).getContext();
        doReturn(command).when(context).getCommand();
        doReturn(commandId).when(command).getId();
        // stopping requests processor
        requestsProcessor.shutdown();
        reset(requestsProcessor);

        // Act
        var exception = assertThrows(Exception.class, () -> messagesExecutor.processActionCommand(request));

        // Verification
        verify(requestsProcessor).isProcessorActive();
        // processActionCommand
        verify(logger).debug(startsWith("Validating input command-message before"));
        verify(messagesExecutor).validateInput(request);
        verify(logger).info("=== Sending command-message to start processing it, correlationId='{}'", correlationId);
        verify(logger).warn(
                "Launch: '{}' is NOT active. Message with correlationId='{}' won't accept for processing.",
                REQUESTS_PROCESSOR_NAME, correlationId
        );
        // launchCommandMessageProcessing throws
        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(exception.getMessage()).startsWith("Cannot execute command '" + commandId);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo(REQUESTS_PROCESSOR_NAME + " isn't in active state.");
    }

    @Test
    void shouldNotProcessActionCommand_DidNotLaunchResponsesProcessing() throws InterruptedException {
        // Init
        String commandId = "command-id";
        String correlationId = "correlation-id";
        doReturn(correlationId).when(request).getCorrelationId();
        doReturn(CommandMessage.Direction.DO).when(request).getDirection();
        doReturn(context).when(request).getContext();
        doReturn(command).when(context).getCommand();
        doReturn(commandId).when(command).getId();
        // stopping responses processor
        responsesProcessor.shutdown();
        reset(responsesProcessor);

        // Act
        var exception = assertThrows(Exception.class, () -> messagesExecutor.processActionCommand(request));

        // Verification
        verify(requestsProcessor, atLeastOnce()).isProcessorActive();
        verify(requestsProcessor, atLeastOnce()).takeMessage();
        verify(responsesProcessor, atLeastOnce()).isProcessorActive();
        // processActionCommand
        verify(logger).debug(startsWith("Validating input command-message before"));
        verify(messagesExecutor).validateInput(request);
        verify(logger).info("=== Sending command-message to start processing it, correlationId='{}'", correlationId);
        // launchCommandMessageProcessing
        verify(messagesExecutor).makeMessageInProgress(correlationId, request);
        // launchCommandMessageProcessing::initiateProcessingMessage
        verify(requestsProcessor).accept(request);
        verify(logger).info("Launch: message with correlationId='{}' is accepted for processing.", correlationId);
        verify(logger).info("=== Waiting for processed command message of command '{}' with correlationId='{}'", commandId, correlationId);
        // waitingProcessedCommandMessage
        verify(logger).warn("{} is NOT active. Message with correlationId='{}' is won't receive",
                RESPONSES_PROCESSOR_NAME, correlationId);
        // verifying thrown exception
        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(exception.getMessage()).startsWith("Cannot execute command '" + commandId);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo(RESPONSES_PROCESSOR_NAME + " isn't in active state.");
    }

    @Test
    void shouldNotProcessActionCommand_CommandExecutionThrows() throws InterruptedException {
        // Init
        ActionContext actionContext = ActionContext.setup("test-facade", "test-action");
        String commandId = "command-id";
        String correlationId = "correlation-id";
        doReturn(actionContext).when(request).getActionContext();
        doReturn(correlationId).when(request).getCorrelationId();
        doReturn(CommandMessage.Direction.DO).when(request).getDirection();
        doReturn(context).when(request).getContext();
        doReturn(true).when(context).isReady();
        doReturn(command).when(context).getCommand();
        doReturn(command).when(command).self();
        doReturn(commandId).when(command).getId();
        doCallRealMethod().when(command).doCommand(any(Context.class));
        var exception = new ArithmeticException("ArithmeticException");
        doThrow(exception).when(command).executeDo(context);

        // Act
        messagesExecutor.processActionCommand(request);

        // Verification
        // all processors are launched and ready to process messages
        verify(requestsProcessor, atLeastOnce()).isProcessorActive();
        verify(requestsProcessor, atLeastOnce()).takeMessage();
        verify(responsesProcessor, atLeastOnce()).isProcessorActive();
        verify(responsesProcessor, atLeastOnce()).takeMessage();
        // processActionCommand
        verify(logger).debug(startsWith("Validating input command-message before"));
        verify(messagesExecutor).validateInput(request);
        verify(logger).info(startsWith("=== Sending command-message to start "), eq(correlationId));
        // launchCommandMessageProcessing
        verify(messagesExecutor).makeMessageInProgress(correlationId, request);
        // launchCommandMessageProcessing::initiateProcessingMessage
        verify(requestsProcessor).accept(request);
        verify(logger).info("Launch: message with correlationId='{}' is accepted for processing.", correlationId);
        verify(requestsProcessor).runAsyncTakenMessage(any(Consumer.class ), eq(request));
        verify(requestsProcessor).onTakenMessage(request);
        verify(logger).debug("Taken message {}", request);
        verify(messagesExecutor).executeWithActionContext(request);
        verify(logger).debug("Executing request with correlation-id:{} in action-context:{}", correlationId, actionContext);
        verify(messagesExecutor).onTakenRequestMessage(request);
        verify(logger).debug("Processing request message with correlationId='{}'", correlationId);
        verify(messagesExecutor).localExecutionResult(request);
        verify(command).doCommand(context);
        verify(command).executeDo(context); // exception was thrown
        verify(logger).error("== Couldn't process message request with correlation-id:{}", correlationId, exception);
        // on request error processing
        verify(messagesExecutor).onErrorRequestMessage(request, exception);
        verify(logger).error("=+= Context not failed but something thrown after {}", context, exception);
        verify(context).failed(exception);
        // waitingProcessedCommandMessage
        verify(logger).info("=== Waiting for processed command message of command '{}' with correlationId='{}'",
                commandId, correlationId);
        verify(responsesProcessor, times(2)).isProcessorActive();
        verify(responsesProcessor, never()).accept(request);
        // waitingProcessedCommandMessage::retrieveProcessedMessage
        verify(messagesExecutor, times(2)).messageWatchdogFor(correlationId);
        verify(messagesExecutor).stopWatchingMessage(correlationId);
        verify(logger).debug("= Retrieve: the result of command '{}' after processing is {}", commandId, request);
    }

    @Test
    void shouldLaunchInProcessor() throws InterruptedException {
        // Init
        ReflectionTestUtils.setField(messagesExecutor, "processorLaunchExecutor", Executors.newSingleThreadExecutor());
        CountDownLatch latchOfProcessors = new CountDownLatch(1);

        // Act
        ReflectionTestUtils.invokeMethod(messagesExecutor, "launchInProcessor", latchOfProcessors);

        // waiting for processor is started
        latchOfProcessors.await();
        // Verification
        verify(messagesExecutor).prepareRequestsProcessor();
        verify(requestsProcessor).doingMainLoop();
        // Check data updates
        assertThat(latchOfProcessors.getCount()).isZero();
    }

    @Test
    void shouldPrepareRequestsProcessor() {
        // Act
        var processor = messagesExecutor.prepareRequestsProcessor();

        // Check data updates
        assertThat(processor).isNotNull();
        assertThat(processor.getProcessorName()).isEqualTo(REQUESTS_PROCESSOR_NAME);
    }

    @Test
    void shouldLaunchOutProcessor() throws InterruptedException {
        // Init
        ReflectionTestUtils.setField(messagesExecutor, "processorLaunchExecutor", Executors.newSingleThreadExecutor());
        CountDownLatch latchOfProcessors = new CountDownLatch(1);

        // Act
        ReflectionTestUtils.invokeMethod(messagesExecutor, "launchOutProcessor", latchOfProcessors);

        // waiting for processor is started
        latchOfProcessors.await();
        // Verification
        verify(messagesExecutor).prepareResponsesProcessor();
        verify(responsesProcessor).doingMainLoop();
        // Check data updates
        assertThat(latchOfProcessors.getCount()).isZero();
    }

    @Test
    void shouldPrepareResponsesProcessor() {
        // Act
        var processor = messagesExecutor.prepareResponsesProcessor();

        // Check data updates
        assertThat(processor).isNotNull();
        assertThat(processor.getProcessorName()).isEqualTo(RESPONSES_PROCESSOR_NAME);
    }

    @Test
    void shouldInitializeTakenMessagesExecutor() {
        // Act
        messagesExecutor.initializeTakenMessagesExecutor();
        // Verification
        verify(messagesExecutor).nothingToExecute();
    }

    @Test
    void shouldShutdownTakenMessagesExecutor() {
        // Act
        messagesExecutor.shutdownTakenMessagesExecutor();
        // Verification
        verify(messagesExecutor).nothingToExecute();
    }

    @Test
    void shouldGetResponsesProcessor() {
        // Act
        var processor = messagesExecutor.getResponsesProcessor();
        // Check data updates
        assertThat(processor).isNotNull().isSameAs(responsesProcessor);
    }

    // class implementation
    class CommandMessagesExchangeExecutorStub extends CommandMessagesExchangeExecutorAdapter {

        @Override
        protected MessagesProcessor prepareRequestsProcessor() {
            return spy(MessageProcessorStub.builder()
                    .processorName(REQUESTS_PROCESSOR_NAME).processingTaken(this::executeWithActionContext)
                    .exchange(this).logger(logger).build()
            );
        }

        @Override
        protected MessagesProcessor prepareResponsesProcessor() {
            return spy(MessageProcessorStub.builder()
                    .processorName(RESPONSES_PROCESSOR_NAME).processingTaken(this::onTakenResponseMessage)
                    .exchange(this).logger(logger).build()
            );
        }

        @Override
        protected void initializeTakenMessagesExecutor() {
            // nothing to do
            nothingToExecute();
        }

        @Override
        protected void shutdownTakenMessagesExecutor() {
            // nothing to do
            nothingToExecute();
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        /**
         * To prepare and start message watcher for the command-message
         *
         * @param correlationId correlation-id of message to watch after
         * @param original      original message to watch after
         * @return true if it's made
         */
        @Override
        protected boolean makeMessageInProgress(String correlationId, CommandMessage<?> original) {
            return true;
        }

        /**
         * To get the watcher of in-progress message
         *
         * @param correlationId correlation-id of watching message
         * @return command-message watcher
         */
        @Override
        protected <T> Optional<CommandMessageWatchdog<T>> messageWatchdogFor(String correlationId) {
            return Optional.empty();
        }

        /**
         * To stop watching after of the command-message
         *
         * @param correlationId correlation-id of message to stop watching after
         */
        @Override
        protected void stopWatchingMessage(String correlationId) {

        }

        void nothingToExecute() {

        }
    }

    @SuperBuilder
    static class MessageProcessorStub extends RootMessageProcessor {
        private final AtomicReference<CommandMessage<?>> takenHolder = new AtomicReference<>(null);
        private final Object takenStateMonitor = new Object();

        @Override
        public <T> CommandMessage<T> takeMessage() throws InterruptedException {
            // waiting for accept message
            while (takenHolder.get() == null) synchronized (takenStateMonitor) {
                takenStateMonitor.wait(25);
            }
            CommandMessage<T> taken = (CommandMessage<T>) takenHolder.get();
            takenHolder.getAndSet(null);
            return taken;
        }

        @Override
        public boolean isEmpty() {
            return takenHolder.get() == null;
        }

        @Override
        public <T> boolean accept(CommandMessage<T> message) {
            return takenHolder.compareAndSet(null, message);
        }
    }
}
