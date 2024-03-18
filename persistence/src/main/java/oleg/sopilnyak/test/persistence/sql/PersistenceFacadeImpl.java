package oleg.sopilnyak.test.persistence.sql;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.persistence.sql.entity.*;
import oleg.sopilnyak.test.persistence.sql.implementation.OrganizationPersistenceFacadeImplementation;
import oleg.sopilnyak.test.persistence.sql.implementation.ProfilePersistenceFacadeImplementation;
import oleg.sopilnyak.test.persistence.sql.implementation.StudentCourseLinkPersistenceFacadeImplementation;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.*;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Service-Facade-Implementation: Service for manage persistence layer of the school
 */
@Slf4j
@Component
@Getter
public class PersistenceFacadeImpl implements PersistenceFacade,
        StudentCourseLinkPersistenceFacadeImplementation,
        OrganizationPersistenceFacadeImplementation,
        ProfilePersistenceFacadeImplementation {
    @Resource
    StudentRepository studentRepository;
    @Resource
    CourseRepository courseRepository;
    @Resource
    AuthorityPersonRepository authorityPersonRepository;
    @Resource
    FacultyRepository facultyRepository;
    @Resource
    StudentsGroupRepository studentsGroupRepository;
    private final SchoolEntityMapper mapper;
    @Getter(AccessLevel.NONE)
    private PersistenceFacade delegate;

    public PersistenceFacadeImpl(SchoolEntityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Logger getLog() {
        return log;
    }

    @PostConstruct
    private void setup() {
        delegate = this;
    }

    /**
     * To initialize default minimal data-set for the application
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void initDefaultDataset() {
        StudentEntity malePupil = StudentEntity.builder()
                .firstName("John").lastName("Doe").gender("Mr")
                .description("The best male pupil in the School.")
                .build();
        if (studentExists(malePupil)) {
            log.info("Default data-set is installed already.");
            return;
        }
        AuthorityPersonEntity maleTeacher = AuthorityPersonEntity.builder()
                .title("Teacher").firstName("Bill").lastName("Clinton").gender("Mr")
                .build();
        AuthorityPersonEntity femaleTeacher = AuthorityPersonEntity.builder()
                .title("Teacher").firstName("Hillary").lastName("Clinton").gender("Mrs")
                .build();
        log.info("Saving authority persons set...");
        delegate.save(maleTeacher);
        delegate.save(femaleTeacher);

        FacultyEntity languageFaculty = FacultyEntity.builder()
                .name("Languages")
                .build();
        FacultyEntity mathFaculty = FacultyEntity.builder()
                .name("Math")
                .build();
        FacultyEntity natureFaculty = FacultyEntity.builder()
                .name("Nature")
                .build();
        log.info("Saving faculty set...");
        delegate.save(languageFaculty);
        delegate.save(mathFaculty);
        delegate.save(natureFaculty);

        StudentsGroupEntity group = StudentsGroupEntity.builder()
                .name("Pupils")
                .build();
        log.info("Saving students groups set...");
        delegate.save(group);

        StudentEntity femalePupil = StudentEntity.builder()
                .firstName("Jane").lastName("Doe").gender("Ms")
                .description("The best female pupil in the School.")
                .build();
        CourseEntity english = CourseEntity.builder()
                .name("English")
                .description("Internation language obligated for all around the world.")
                .build();
        CourseEntity mathematics = CourseEntity.builder()
                .name("Mathematics")
                .description("The queen of the sciences.")
                .build();
        CourseEntity geographic = CourseEntity.builder()
                .name("Geographic")
                .description("The science about sever countries location and habits.")
                .build();

        log.info("Saving students set...");
        delegate.save(malePupil);
        delegate.save(femalePupil);

        log.info("Saving courses set...");
        delegate.save(english);
        delegate.save(mathematics);
        delegate.save(geographic);

        log.info("Linking students with courses...");
        delegate.link(malePupil, english);
        delegate.link(malePupil, mathematics);
        delegate.link(malePupil, geographic);

        delegate.link(femalePupil, english);
        delegate.link(femalePupil, mathematics);
        delegate.link(femalePupil, geographic);

        log.info("Making organization structure of the school...");
        log.info("Authorities...");
        maleTeacher.add(mathFaculty);
        femaleTeacher.add(languageFaculty);
        femaleTeacher.add(natureFaculty);
        delegate.save(maleTeacher);
        delegate.save(femaleTeacher);

        log.info("Faculties...");
        languageFaculty.add(english);
        mathFaculty.add(mathematics);
        natureFaculty.add(geographic);
        delegate.save(languageFaculty);
        delegate.save(mathFaculty);
        delegate.save(natureFaculty);

        log.info("Students Groups...");
        group.add(malePupil);
        group.add(femalePupil);
        delegate.save(group);
    }


    // private methods
    private boolean studentExists(StudentEntity pupil) {
        return studentRepository.findAll().stream().anyMatch(student -> theSame(student, pupil));
    }

    private static boolean theSame(StudentEntity student, StudentEntity pupil) {
        return student.getFirstName().equals(pupil.getFirstName()) &&
                student.getLastName().equals(pupil.getLastName()) &&
                student.getGender().equals(pupil.getGender()) &&
                student.getDescription().equals(pupil.getDescription())
                ;
    }

}
