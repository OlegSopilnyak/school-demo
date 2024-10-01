package oleg.sopilnyak.test.end2end.command.executable.sys;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.executable.student.CreateOrUpdateStudentCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {PersistenceConfiguration.class, CreateOrUpdateStudentCommand.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
@EnableTransactionManagement
@Rollback
public class ParallelMacroCommandTest extends MysqlTestModelFactory {
    // executor of parallel nested commands
    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    final int maxPoolSize = 100;
    //    @SpyBean
    @Autowired
    private PlatformTransactionManager transactionManager;
    @SpyBean
    @Autowired
    StudentsPersistenceFacade persistence;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @SpyBean
    @Autowired
    CreateOrUpdateStudentCommand command;
    final int maxIterationsCount = 10;


    @BeforeEach
    void setUp() {
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(maxPoolSize);
        executor.initialize();
    }

    @AfterEach
    void tearDown() {
        reset(command, persistence, payloadMapper);
        executor.shutdown();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void shouldBeEverythingIsValid() {
        assertThat(command).isNotNull();
        assertThat(persistence).isNotNull();
        assertThat(transactionManager).isNotNull();
        assertThat(payloadMapper).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    void shouldCreatedStudentsInOneThread() {
        IntStream.range(0, maxIterationsCount).forEach(this::createStudent);
        assertThat(persistence.isNoStudents()).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    void shouldSeeCreatedStudentsInManyThread() throws InterruptedException {
        IntStream.range(0, maxIterationsCount).forEach(this::createStudent);
        CountDownLatch countDownLatch = new CountDownLatch(maxIterationsCount);
        AtomicBoolean canSee = new AtomicBoolean(true);
        IntStream.range(0, maxIterationsCount).forEach(i -> executor.submit(new SeeCreatedStudentNoTransaction(canSee, countDownLatch)));
        countDownLatch.await();
        assertThat(canSee.get()).isTrue();
        assertThat(persistence.isNoStudents()).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    void shouldNotSeeCreatedStudentsInManyThread() throws InterruptedException {
        IntStream.range(0, maxIterationsCount).forEach(this::createStudent);
        CountDownLatch countDownLatch = new CountDownLatch(maxIterationsCount);
        AtomicBoolean canSee = new AtomicBoolean(true);
        boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        System.out.println("--Under Transactions(in the test)-- " + transactionName + " active :" + isActive);
        IntStream.range(0, maxIterationsCount).forEach(i -> executor.submit(new SeeCreatedStudentUnderTransaction(canSee, countDownLatch, new SharedTransactionData(transactionManager))));
        countDownLatch.await();
        assertThat(canSee.get()).isFalse();
        assertThat(persistence.isNoStudents()).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    void shouldSeeCreatedStudentsInManyThreadsThroughRootThread() throws InterruptedException {
        IntStream.range(0, maxIterationsCount).forEach(this::createStudent);
        CountDownLatch countDownLatch = new CountDownLatch(maxIterationsCount);
        AtomicBoolean canSee = new AtomicBoolean(true);
        IntStream.range(0, maxIterationsCount).forEach(i -> executor.submit(new SeeCreatedStudentThroughRootThread(canSee, countDownLatch)));
        processParallelActions(maxIterationsCount);
        countDownLatch.await();
        assertThat(canSee.get()).isTrue();
    }

    private final BlockingQueue<DoInRootThread> parallelActions = new LinkedBlockingQueue<>();

    private void registerParallelAction(DoInRootThread action) {
        parallelActions.offer(action);
    }

    private void processParallelActions(int capacity) {
        for (int i = 0; i < capacity; i++) {
            try {
                DoInRootThread action = parallelActions.take();
                action.doInRoot();
                if (Objects.isNull(action)) {
                    System.out.println("Null action in parallel actions");
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class DoInRootThread {
        final Object actionFinished = new Object();
        final Consumer<Void> toDo;

        DoInRootThread(Consumer<Void> toDo) {
            this.toDo = toDo;
        }

        void doInRoot() {
            try {
                toDo.accept(null);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                actionIsFinished();
            }
        }

        void actionIsFinished() {
            synchronized (actionFinished) {
                actionFinished.notify();
            }
        }
    }

    static class SharedTransactionData {
        private volatile boolean active = false;
        private final String currentTransactionName;
        private final Integer currentTransactionIsolationLevel;
        private final boolean currentTransactionReadOnly;
        private final Map<Object, Object> resourcesMap;
        private final TransactionTemplate transactionTemplate;

        public SharedTransactionData(PlatformTransactionManager transactionManager) {
            transactionTemplate = new TransactionTemplate(transactionManager);
            resourcesMap = TransactionSynchronizationManager.getResourceMap();
            currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
            currentTransactionIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
            currentTransactionReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        }

        public void synchronize() {
            if (active) return;
            resourcesMap.entrySet().forEach(entry ->
                    TransactionSynchronizationManager.bindResource(entry.getKey(), entry.getValue())
            );
            TransactionSynchronizationManager.setCurrentTransactionName(currentTransactionName);
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(currentTransactionIsolationLevel);
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(currentTransactionReadOnly);
            active = true;
        }
    }

    class SeeCreatedStudentNoTransaction implements Runnable {
        final AtomicBoolean canSee;
        final CountDownLatch countDownLatch;

        public SeeCreatedStudentNoTransaction(AtomicBoolean canSee, CountDownLatch countDownLatch) {
            this.canSee = canSee;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            boolean isNoStudents = persistence.isNoStudents();
            if (isNoStudents) {
                System.out.println("--No Transactions-- No students found");
                canSee.compareAndSet(true, false);
            } else {
                System.out.println("--No Transactions-- The students are found");
            }
            countDownLatch.countDown();
        }
    }

    class SeeCreatedStudentUnderTransaction implements Runnable {
        final AtomicBoolean canSee;
        final CountDownLatch countDownLatch;
        final SharedTransactionData transactionData;

        public SeeCreatedStudentUnderTransaction(AtomicBoolean canSee,
                                                 CountDownLatch countDownLatch,
                                                 SharedTransactionData transactionData) {
            this.canSee = canSee;
            this.countDownLatch = countDownLatch;
            this.transactionData = transactionData;
        }

        @Override
        public void run() {
            transactionData.synchronize();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            TransactionSynchronizationManager.initSynchronization();
            try {

                transactionData.transactionTemplate.execute(new TransactionCallbackWithoutResult() {

                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        findStudentInAnotherThread(canSee);
                    }

                });
            } catch (Exception e) {
                canSee.compareAndSet(true, false);
                e.printStackTrace();
            }
            countDownLatch.countDown();
        }
    }

    class SeeCreatedStudentThroughRootThread implements Runnable {
        final AtomicBoolean canSee;
        final CountDownLatch countDownLatch;

        public SeeCreatedStudentThroughRootThread(AtomicBoolean canSee,
                                                  CountDownLatch countDownLatch) {
            this.canSee = canSee;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            Consumer<Void> activity = unused -> findStudentInAnotherThread(canSee);
            DoInRootThread action = new DoInRootThread(activity);

            registerParallelAction(action);

            System.out.println(Thread.currentThread().getName() + "--- Transactions-- waiting for action...");

            waitForActionIsFinished(action.actionFinished);

            System.out.println(Thread.currentThread().getName() + "--- Transactions-- finishing...");
            countDownLatch.countDown();
        }

        void waitForActionIsFinished(final Object actionFinished) {
            synchronized (actionFinished) {
                try {
                    actionFinished.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void createStudent(int i) {
        Student student = makeClearStudent(i);
        Student entity = persistence.save(student).orElse(null);
        assertThat(entity).isNotNull();
    }

    void findStudentInAnotherThread(AtomicBoolean canSee) {
        boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        System.out.println("--Under Transactions(in the thread)-- " + transactionName + " active :" + isActive);
        boolean isNoStudents = persistence.isNoStudents();
        if (isNoStudents) {
            System.out.println(Thread.currentThread().getName() + "--Under Transaction-- No students found");
            canSee.compareAndSet(true, false);
        } else {
            System.out.println(Thread.currentThread().getName() + "--Under Transaction-- The students are found...");
        }
    }
}
