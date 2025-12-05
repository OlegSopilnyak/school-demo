package oleg.sopilnyak.test.end2end.facade.edication.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.end2end.configuration.TestConfig;
import oleg.sopilnyak.test.persistence.configuration.PersistenceConfiguration;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.StudentProfileEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.MysqlTestModelFactory;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateOrUpdateStudentCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.CreateStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.DeleteStudentMacroCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.FindEnrolledStudentsCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.FindNotEnrolledStudentsCommand;
import oleg.sopilnyak.test.service.command.executable.education.student.FindStudentCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.profile.student.DeleteStudentProfileCommand;
import oleg.sopilnyak.test.service.command.factory.StudentCommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.facade.education.impl.StudentsFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SchoolCommandsConfiguration.class, PersistenceConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {"school.spring.jpa.show-sql=true", "school.hibernate.hbm2ddl.auto=update"})
class StudentsFacadeImplTest extends MysqlTestModelFactory {
    private static final String STUDENT_FIND_BY_ID = "student.findById";
    private static final String STUDENT_FIND_ENROLLED_TO = "student.findEnrolledTo";
    private static final String STUDENT_FIND_NOT_ENROLLED = "student.findNotEnrolled";
    private static final String STUDENT_CREATE_OR_UPDATE = "student.createOrUpdate";
    private static final String STUDENT_CREATE_NEW = "student.create.macro";
    private static final String STUDENT_DELETE = "student.delete.macro";

    @Autowired
    ConfigurableApplicationContext context;
    @Autowired
    ApplicationContext applicationContext;

    @MockitoSpyBean
    @Autowired
    ActionExecutor actionExecutor;
    @MockitoSpyBean
    @Autowired
    SchedulingTaskExecutor schedulingTaskExecutor;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    EntityMapper entityMapper;
    @MockitoSpyBean
    @Autowired
    PersistenceFacade persistenceFacade;
    @Autowired
    BusinessMessagePayloadMapper payloadMapper;

    CommandsFactory<StudentCommand<?>> factory;
    StudentsFacadeImpl facade;

    @BeforeEach
    void setUp() {
        factory = buildFactory(persistenceFacade);
        facade = spy(new StudentsFacadeImpl(factory, payloadMapper, actionExecutor));
        ActionContext.setup("test-facade", "test-processing");
    }

    @AfterEach
    void tearDown() {
        deleteEntities(StudentProfileEntity.class);
        deleteEntities(StudentEntity.class);
        deleteEntities(CourseEntity.class);
    }

    @Test
    void allComponentsShouldBeValid() {
        assertThat(payloadMapper).isNotNull();
        assertThat(persistenceFacade).isNotNull();
        assertThat(factory).isNotNull();
        assertThat(facade).isNotNull();
    }

    @Test
    void shouldNotFindById() {
        Long studentId = 100L;

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isEmpty();
        verify(factory).command(STUDENT_FIND_BY_ID);
        verify(factory.command(STUDENT_FIND_BY_ID)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    void shouldFindById() {
        Student newStudent = makeClearTestStudent();
        Long studentId = getPersistentStudent(newStudent).getId();

        Optional<Student> student = facade.findById(studentId);

        assertThat(student).isNotEmpty();
        assertStudentEquals(newStudent, student.get(), false);
        verify(factory.command(STUDENT_FIND_BY_ID)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_FIND_BY_ID)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
    }

    @Test
    void shouldFindEnrolledTo() {
        Student newStudent = makeClearTestStudent();
        Student saved = getPersistentStudent(newStudent);
        Long courseId = saved.getCourses().get(0).getId();

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).hasSize(1);
        assertStudentEquals(newStudent, students.iterator().next(), false);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(Input.of(courseId));
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldNotFindEnrolledTo_NoCourseById() {
        Long courseId = 200L;

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_ENROLLED_TO);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(Input.of(courseId));
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldNotFindEnrolledTo_NoEnrolledStudents() {
        Course course = makeClearCourse(0);
        Long courseId = getPersistentCourse(course).getId();

        Set<Student> students = facade.findEnrolledTo(courseId);

        assertCourseEquals(course, findCourseById(courseId), false);
        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_ENROLLED_TO);
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).createContext(Input.of(courseId));
        verify(factory.command(STUDENT_FIND_ENROLLED_TO)).doCommand(any(Context.class));
        verify(persistenceFacade).findEnrolledStudentsByCourseId(courseId);
    }

    @Test
    void shouldFindNotEnrolled() {
        Student newStudent = makeClearTestStudent();
        if (newStudent instanceof FakeStudent student) {
            student.setCourses(List.of());
        }
        getPersistentStudent(newStudent);

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).hasSize(1);
        assertStudentEquals(newStudent, students.iterator().next(), false);
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(Input.empty());
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldNotFindNotEnrolled_StudentNotExists() {

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(Input.empty());
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldNotFindNotEnrolled_StudentHasCourses() {
        getPersistentStudent(makeClearTestStudent());

        Set<Student> students = facade.findNotEnrolled();

        assertThat(students).isEmpty();
        verify(factory).command(STUDENT_FIND_NOT_ENROLLED);
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).createContext(Input.empty());
        verify(factory.command(STUDENT_FIND_NOT_ENROLLED)).doCommand(any(Context.class));
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldCreateOrUpdate_Create() {
        Student student = makeClearTestStudent();

        Optional<Student> result = facade.create(student);

        assertStudentEquals(student, result.orElseThrow(), false);
        verify(factory).command(STUDENT_CREATE_NEW);
        verify(factory.command(STUDENT_CREATE_NEW)).createContext(any(Input.class));
        verify(factory.command(STUDENT_CREATE_NEW)).doCommand(any(Context.class));
        verify(persistenceFacade).save(any(StudentPayload.class));
    }

    @Test
    void shouldCreateOrUpdate_Update() {
        StudentPayload student = createStudent(makeClearTestStudent());
        student.setFirstName(student.getFirstName() + "-newOne");

        Optional<Student> result = facade.createOrUpdate(student);

        assertThat(result).isNotEmpty();
        assertStudentEquals(student, result.orElseThrow());
        verify(factory).command(STUDENT_CREATE_OR_UPDATE);
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).createContext(Input.of(student));
        verify(factory.command(STUDENT_CREATE_OR_UPDATE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(student.getId());
        verify(persistenceFacade).save(student);
    }

    @Test
    void shouldDelete() throws StudentWithCoursesException, StudentNotFoundException {
        Student newStudent = makeClearTestStudent();
        if (newStudent instanceof FakeStudent student) {
            student.setCourses(List.of());
        }
        StudentPayload created = createStudent(newStudent);
        Long studentId = created.getId();
        Long profileId = created.getProfileId();
        assertThat(findStudentById(studentId)).isNotNull();
        assertThat(findStudentProfileById(profileId)).isNotNull();

        facade.delete(studentId);

        assertThat(findStudentById(studentId)).isNull();
        assertThat(findStudentProfileById(profileId)).isNull();
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).deleteStudent(studentId);
        verify(persistenceFacade).deleteProfileById(profileId);
        // 1. building context for delete
        // 2. deleting student
        verify(persistenceFacade, times(2)).findStudentById(studentId);
        verify(persistenceFacade).findStudentProfileById(profileId);
    }

    @Test
    void shouldNotDelete_StudentNotExists() {
        Long studentId = 101L;

        StudentNotFoundException exception = assertThrows(StudentNotFoundException.class, () -> facade.delete(studentId));

        assertThat(exception.getMessage()).isEqualTo("Student with ID:101 is not exists.");
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        verify(persistenceFacade).findStudentById(studentId);
        verify(persistenceFacade, never()).deleteStudent(anyLong());
    }

    @Test
    void shouldNotDelete_StudentWithCourses() {
        Long studentId = createStudent(makeClearTestStudent()).getId();
        assertThat(studentId).isNotNull();

        StudentWithCoursesException exception = assertThrows(StudentWithCoursesException.class, () -> facade.delete(studentId));

        assertThat("Student with ID:" + studentId + " has registered courses.").isEqualTo(exception.getMessage());
        verify(factory).command(STUDENT_DELETE);
        verify(factory.command(STUDENT_DELETE)).createContext(Input.of(studentId));
        verify(factory.command(STUDENT_DELETE)).doCommand(any(Context.class));
        // 1. building context for delete
        // 2. deleting student
        verify(persistenceFacade, times(2)).findStudentById(studentId);
        verify(persistenceFacade, never()).deleteStudent(anyLong());
    }

    // private methods
    private Student findStudentById(Long id) {
        return findEntity(StudentEntity.class, id, e -> e.getCourseSet().size());
    }

    private Student findStudentProfileById(Long profileId) {
        return findEntityByFK(StudentEntity.class, "profileId", profileId, e -> e.getCourseSet().size());
    }

    private Course findCourseById(Long id) {
        return findEntity(CourseEntity.class, id, e -> e.getStudentSet().size());
    }

    private Student getPersistentStudent(Student newStudent) {
        return payloadMapper.toPayload(persist(newStudent));
    }

    private Course getPersistentCourse(Course newCourse) {
        return payloadMapper.toPayload(persist(newCourse));
    }

    private StudentPayload createStudent(Student newStudent) {
        try {
            StudentProfile profile = persist(makeStudentProfile(null));
            if (newStudent instanceof FakeStudent fake) {
                fake.setProfileId(profile.getId());
            } else {
                fail("Not a fake person type");
            }
            return payloadMapper.toPayload(persist(newStudent));
        } finally {
            reset(payloadMapper);
        }
    }

    private Student persist(Student newInstance) {
        Student entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private Course persist(Course newInstance) {
        Course entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private StudentProfile persist(StudentProfile newInstance) {
        StudentProfileEntity entity = entityMapper.toEntity(newInstance);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    private CommandsFactory<StudentCommand<?>> buildFactory(PersistenceFacade persistenceFacade) {
        CreateOrUpdateStudentCommand createStudentCommand = spy(new CreateOrUpdateStudentCommand(persistenceFacade, payloadMapper));
        CreateOrUpdateStudentProfileCommand createProfileCommand = spy(new CreateOrUpdateStudentProfileCommand(persistenceFacade, payloadMapper));
        DeleteStudentCommand deleteStudentCommand = spy(new DeleteStudentCommand(persistenceFacade, payloadMapper));
        DeleteStudentProfileCommand deleteProfileCommand = spy(new DeleteStudentProfileCommand(persistenceFacade, payloadMapper));
        Map<StudentProfileCommand<?>, String> profiles = Map.of(
                createProfileCommand, "profileStudentUpdate",
                deleteProfileCommand, "profileStudentDelete"
        );
        Map<StudentCommand<?>, String> commands = Map.of(
                spy(new FindStudentCommand(persistenceFacade, payloadMapper)), "studentFind",
                spy(new FindEnrolledStudentsCommand(persistenceFacade, payloadMapper)), "studentFindEnrolled",
                spy(new FindNotEnrolledStudentsCommand(persistenceFacade, payloadMapper)), "studentFindNotEnrolled",
                createStudentCommand, "studentUpdate",
                spy(new CreateStudentMacroCommand(createStudentCommand, createProfileCommand, payloadMapper, actionExecutor)), "studentMacroCreate",
                deleteStudentCommand, "studentDelete",
                spy(new DeleteStudentMacroCommand(
                        deleteStudentCommand, deleteProfileCommand, schedulingTaskExecutor, persistenceFacade, actionExecutor
                )), "studentMacroDelete"
        );
        String acName = "applicationContext";
        profiles.entrySet().forEach(entry -> {
            StudentProfileCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            String beanName = entry.getValue();
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            StudentProfileCommand<?> transactionalCommand = (StudentProfileCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        commands.entrySet().forEach(entry -> {
            StudentCommand<?> command = entry.getKey();
            if (ReflectionUtils.findField(command.getClass(), acName) != null) {
                ReflectionTestUtils.setField(command, acName, applicationContext);
            }
            String beanName = entry.getValue();
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            if (beanFactory.containsSingleton(beanName)) {
                beanFactory.destroySingleton(beanName);
            }
            StudentCommand<?> transactionalCommand = (StudentCommand<?>) transactCommand(command);
            context.getBeanFactory().registerSingleton(beanName, transactionalCommand);
        });
        return spy(new StudentCommandsFactory(commands.keySet()));
    }
}