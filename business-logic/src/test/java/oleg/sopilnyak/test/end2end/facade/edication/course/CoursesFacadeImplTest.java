package oleg.sopilnyak.test.end2end.facade.edication.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.course.CreateOrUpdateCourseCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.DeleteCourseCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.FindCourseCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.FindCoursesWithoutStudentsCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.FindRegisteredCoursesCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.RegisterStudentToCourseCommand;
import oleg.sopilnyak.test.service.command.executable.education.course.UnRegisterStudentFromCourseCommand;
import oleg.sopilnyak.test.service.command.factory.CourseCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.facade.education.impl.CoursesFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        SchoolCommandsConfiguration.class,PersistenceConfiguration.class, TestConfig.class
})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class CoursesFacadeImplTest extends MysqlTestModelFactory {

    public static final String COURSE_FIND_BY_ID = "course.findById";
    public static final String COURSE_FIND_REGISTERED_FOR = "course.findRegisteredFor";
    public static final String COURSE_FIND_WITHOUT_STUDENTS = "course.findWithoutStudents";
    public static final String COURSE_CREATE_OR_UPDATE = "course.createOrUpdate";
    public static final String COURSE_DELETE = "course.delete";
    public static final String COURSE_REGISTER = "course.register";
    public static final String COURSE_UN_REGISTER = "course.unRegister";

    @Autowired
    ConfigurableApplicationContext context;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    @MockitoSpyBean
    ActionExecutor actionExecutor;
    @Autowired
    @MockitoSpyBean
    PersistenceFacade persistenceFacade;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;


    CommandsFactory<CourseCommand<?>> factory;
    CoursesFacade facade;

    @BeforeEach
    void setUp() {
        factory = spy(buildFactory(persistenceFacade));
        facade = spy(new CoursesFacadeImpl(factory, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-processing");
    }

    @AfterEach
    void tearDown() {
        deleteEntities(StudentEntity.class);
        deleteEntities(CourseEntity.class);
    }

    @Test
    void shouldBeEverythingIsValid() {
        assertThat(emf).isNotNull();
        assertThat(applicationContext).isNotNull();
        assertThat(payloadMapper).isNotNull();
        assertThat(actionExecutor).isNotNull();
    }

    @Test
    void shouldNotFindById() {
        Long courseId = 100L;
        ActionContext.setup("test-facade", "test-processing");

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isEmpty();
        verify(factory).command(COURSE_FIND_BY_ID);
        verify(factory.command(COURSE_FIND_BY_ID)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    void shouldFindById() {
        Course newCourse = makeClearTestCourse();
        Long courseId = persist(newCourse).getId();
        ActionContext.setup("test-facade", "test-processing");

        Optional<Course> course = facade.findById(courseId);

        assertThat(course).isNotEmpty();
        assertCourseEquals(newCourse, course.orElseThrow(), false);
        verify(factory).command(COURSE_FIND_BY_ID);
        verify(factory.command(COURSE_FIND_BY_ID)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
    }

    @Test
    void shouldNotFindRegisteredFor() {
        Long studentId = 200L;

        Set<Course> courses = facade.findRegisteredFor(studentId);

        assertThat(courses).isEmpty();
        verify(factory).command(COURSE_FIND_REGISTERED_FOR);
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).createContext(Input.of(studentId));
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    void shouldFindRegisteredFor() {
        Course newCourse = makeClearTestCourse();
        Course savedCourse = persist(newCourse);
        Long studentId = savedCourse.getStudents().get(0).getId();
        assertThat(studentId).isNotNull();

        Set<Course> courses = facade.findRegisteredFor(studentId);

        assertThat(courses).isNotEmpty();
        assertCourseEquals(newCourse, courses.iterator().next(), false);
        assertThat(courses).hasSize(1);
        verify(factory).command(COURSE_FIND_REGISTERED_FOR);
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).createContext(Input.of(studentId));
        verify(factory.command(COURSE_FIND_REGISTERED_FOR)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesRegisteredForStudent(studentId);
    }

    @Test
    void shouldNotFindWithoutStudents() {

        Set<Course> courses = facade.findWithoutStudents();

        assertThat(courses).isEmpty();
        verify(factory).command(COURSE_FIND_WITHOUT_STUDENTS);
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).createContext(Input.empty());
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    void shouldFindWithoutStudents() {
        Course newCourse = makeClearCourse(0);
        persist(newCourse);

        Set<Course> courses = facade.findWithoutStudents();

        assertThat(courses).isNotEmpty();
        assertCourseEquals(newCourse, courses.iterator().next(), false);
        assertThat(courses).hasSize(1);
        verify(factory).command(COURSE_FIND_WITHOUT_STUDENTS);
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).createContext(Input.empty());
        verify(factory.command(COURSE_FIND_WITHOUT_STUDENTS)).doCommand(any(Context.class));
        verify(persistenceFacade).findCoursesWithoutStudents();
    }

    @Test
    void shouldCreateOrUpdate() {
        Course newCourse = makeClearCourse(1);
        Course courseToUpdate = persist(newCourse);

        Optional<Course> course = facade.createOrUpdate(courseToUpdate);

        assertThat(course).isPresent();
        assertCourseEquals(courseToUpdate, course.get(), false);
        verify(factory).command(COURSE_CREATE_OR_UPDATE);
        verify(factory.command(COURSE_CREATE_OR_UPDATE)).createContext(any(Input.class));
        verify(factory.command(COURSE_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).save(any(Course.class));
    }

    @Test
    void shouldDelete() throws CourseNotFoundException, CourseWithStudentsException, InterruptedException {
        Long courseId = persist(makeClearCourse(0)).getId();
        assertThat(findCourseById(courseId)).isPresent();

        facade.delete(courseId);
        Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(() -> findCourseById(courseId).isEmpty());

        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).deleteCourse(courseId);
        assertThat(findCourseById(courseId)).isEmpty();
    }

    @Test
    void shouldNotDelete_CourseNotExists() {
        Long courseId = 101L;

        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:101 is not exists.");
        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotDelete_CourseWithStudents() {
        Course newCourse = makeClearTestCourse();
        Long courseId = persist(newCourse).getId();
        assertThat(findCourseById(courseId)).isPresent();

        CourseWithStudentsException exception = assertThrows(CourseWithStudentsException.class, () -> facade.delete(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:" + courseId + " has enrolled students.");
        verify(factory).command(COURSE_DELETE);
        verify(factory.command(COURSE_DELETE)).createContext(Input.of(courseId));
        verify(factory.command(COURSE_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldRegister() throws CourseNotFoundException, CourseHasNoRoomException, StudentCoursesExceedException, StudentNotFoundException, InterruptedException {
        Student student = makeClearStudent(0);
        Long studentId = persist(student).getId();
        Course course = makeClearCourse(0);
        Long courseId = persist(course).getId();

        facade.register(studentId, courseId);
        Awaitility.await().atMost(200, TimeUnit.MILLISECONDS)
                .until(() -> findStudentById(studentId).orElseThrow().getCourses().contains(findCourseById(courseId).orElseThrow()));

        Optional<Course> courseEntity = findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents().get(0).getId()).isEqualTo(studentId);
        Optional<Student> studentEntity = findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses().get(0).getId()).isEqualTo(courseId);

        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).link(studentEntity.orElseThrow(), courseEntity.orElseThrow());
    }

    @Test
    void shouldRegister_AlreadyLinked() throws CourseNotFoundException, CourseHasNoRoomException, StudentCoursesExceedException, StudentNotFoundException {
        Student student = persist(makeClearStudent(0));
        Course course = persist(makeClearCourse(0));
        assertThat(((StudentEntity) student).add(course)).isTrue();
        merge(student);
        Long studentId = student.getId();
        Long courseId = course.getId();
        Optional<Course> courseEntity = findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents().get(0).getId()).isEqualTo(studentId);
        Optional<Student> studentEntity = findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses().get(0).getId()).isEqualTo(courseId);

        facade.register(studentId, courseId);


        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    void shouldNotRegister_StudentNotExists() {
        Long studentId = 202L;
        Long courseId = 102L;

        Exception exception = assertThrows(StudentNotFoundException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:202 is not exists.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(anyLong());
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    void shouldNotRegister_NoRoomInTheCourse() {
        Student student = makeClearStudent(0);
        Long studentId = persist(student).getId();
        Course course = makeClearTestCourse();
        Long courseId = persist(course).getId();

        CourseHasNoRoomException exception = assertThrows(CourseHasNoRoomException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:" + courseId + " does not have enough rooms.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    void shouldNotRegister_StudentCoursesExceed() {
        Student student = makeClearTestStudent();
        Long studentId = persist(student).getId();
        Course course = makeClearCourse(0);
        Long courseId = persist(course).getId();


        StudentCoursesExceedException exception = assertThrows(StudentCoursesExceedException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:" + studentId + " exceeds maximum courses.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    void shouldNotRegister_CourseNotExists() {
        Student student = makeClearStudent(0);
        Long studentId = persist(student).getId();
        Long courseId = 102L;

        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class, () -> facade.register(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:102 is not exists.");
        verify(factory).command(COURSE_REGISTER);
        verify(factory.command(COURSE_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).link(any(), any());
    }

    @Test
    void shouldUnRegister_NotLinked() throws CourseNotFoundException, StudentNotFoundException {
        Long studentId = persist(makeClearStudent(0)).getId();
        Long courseId = persist(makeClearCourse(0)).getId();

        facade.unRegister(studentId, courseId);

        Optional<Course> courseEntity = findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents()).isEmpty();
        Optional<Student> studentEntity = findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses()).isEmpty();
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(studentEntity.get(), courseEntity.get());
    }

    @Test
    void shouldUnRegister_Linked() throws CourseNotFoundException, StudentNotFoundException, InterruptedException {
        Student student = persist(makeClearStudent(0));
        Course course = persist(makeClearCourse(0));
        assertThat(((StudentEntity) student).add(course)).isTrue();
        merge(student);
        Long studentId = student.getId();
        Long courseId = course.getId();
        Optional<Course> courseEntity = findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents().get(0).getId()).isEqualTo(studentId);
        Optional<Student> studentEntity = findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses().get(0).getId()).isEqualTo(courseId);

        facade.unRegister(studentId, courseId);
        Awaitility.await().atMost(200, TimeUnit.MILLISECONDS)
                .until(() -> !findStudentById(studentId).orElseThrow().getCourses().contains(course));

        courseEntity = findCourseById(courseId);
        assertThat(courseEntity).isNotEmpty();
        assertThat(courseEntity.get().getId()).isEqualTo(courseId);
        assertThat(courseEntity.get().getStudents()).isEmpty();
        studentEntity = findStudentById(studentId);
        assertThat(studentEntity).isNotEmpty();
        assertThat(studentEntity.get().getId()).isEqualTo(studentId);
        assertThat(studentEntity.get().getCourses()).isEmpty();
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade).unLink(studentEntity.get(), courseEntity.get());
    }

    @Test
    void shouldNotUnRegister_StudentNotExists() {
        Long studentId = 203L;
        Long courseId = 103L;

        Exception exception = assertThrows(StudentNotFoundException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:203 is not exists.");
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).findCourseById(any());
        verify(persistenceFacade, never()).unLink(any(), any());
    }

    @Test
    void shouldNotUnRegister_CourseNotExists() {
        Long studentId = persist(makeClearStudent(0)).getId();
        Long courseId = 103L;

        Exception exception = assertThrows(CourseNotFoundException.class, () -> facade.unRegister(studentId, courseId));

        assertThat(exception.getMessage()).isEqualTo("Course with ID:103 is not exists.");
        verify(factory).command(COURSE_UN_REGISTER);
        verify(factory.command(COURSE_UN_REGISTER)).createContext(Input.of(studentId, courseId));
        verify(factory.command(COURSE_UN_REGISTER)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade).findCourseById(courseId);
        verify(persistenceFacade, never()).unLink(any(), any());
    }

    // private methods
    private void merge(Student instance) {
        StudentEntity entity = instance instanceof StudentEntity instanceEntity ? instanceEntity : entityMapper.toEntity(instance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private Student persist(Student newInstance) {
        StudentEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        } finally {
            em.close();
        }
    }

    private Course persist(Course newInstance) {
        CourseEntity entity = entityMapper.toEntity(newInstance);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            entity.getStudents().forEach(em::persist);
            em.getTransaction().commit();
            return entity;
        } finally {
            em.close();
        }
    }

    private Optional<Student> findStudentById(Long id) {
        return Optional.ofNullable(findEntity(StudentEntity.class, id, e -> e.getCourses().size()));
    }

    private Optional<Course> findCourseById(Long id) {
        return Optional.ofNullable(findEntity(CourseEntity.class, id, e -> e.getStudents().size()));
    }

    private CommandsFactory<CourseCommand<?>> buildFactory(PersistenceFacade persistenceFacade) {
        Map<CourseCommand<?>, String> commands = Map.of(
                spy(new FindCourseCommand(persistenceFacade, payloadMapper)), "courseFind",
                spy(new FindRegisteredCoursesCommand(persistenceFacade, payloadMapper)),"courseFindWithStudents",
                spy(new FindCoursesWithoutStudentsCommand(persistenceFacade, payloadMapper)),"courseFindNoStudents",
                spy(new CreateOrUpdateCourseCommand(persistenceFacade, payloadMapper)),"courseUpdate",
                spy(new DeleteCourseCommand(persistenceFacade, payloadMapper)),"courseDelete",
                spy(new RegisterStudentToCourseCommand(persistenceFacade, payloadMapper, 50, 5)),"courseRegisterStudent",
                spy(new UnRegisterStudentFromCourseCommand(persistenceFacade, payloadMapper)),"courseUnRegisterStudent"
        );
        String acName = "applicationContext";
        commands.entrySet().forEach(entry -> {
            CourseCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            String beanName = entry.getValue();
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            CourseCommand<?> transactionalCommand = (CourseCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        return new CourseCommandsFactory(commands.keySet());
    }
}