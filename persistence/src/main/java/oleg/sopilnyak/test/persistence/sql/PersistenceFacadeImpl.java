package oleg.sopilnyak.test.persistence.sql;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.AuthorityPersonEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.entity.organization.StudentsGroupEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PersonProfileEntity;
import oleg.sopilnyak.test.persistence.sql.entity.profile.PrincipalProfileEntity;
import oleg.sopilnyak.test.persistence.sql.implementation.EducationPersistence;
import oleg.sopilnyak.test.persistence.sql.implementation.OrganizationPersistence;
import oleg.sopilnyak.test.persistence.sql.implementation.ProfilePersistence;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.PersonProfileRepository;
import oleg.sopilnyak.test.persistence.sql.repository.education.CourseRepository;
import oleg.sopilnyak.test.persistence.sql.repository.education.StudentRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.AuthorityPersonRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.FacultyRepository;
import oleg.sopilnyak.test.persistence.sql.repository.organization.StudentsGroupRepository;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;

import static java.util.Objects.isNull;

/**
 * Service-Facade-Implementation: Service for manage persistence layer of the application
 */
@Slf4j
@Getter
public class PersistenceFacadeImpl implements PersistenceFacade,
        EducationPersistence, OrganizationPersistence, ProfilePersistence {
    @Resource
    private StudentRepository studentRepository;
    @Resource
    private CourseRepository courseRepository;
    @Resource
    private AuthorityPersonRepository authorityPersonRepository;
    @Resource
    private FacultyRepository facultyRepository;
    @Resource
    private StudentsGroupRepository studentsGroupRepository;
    @Resource
    private PersonProfileRepository<PersonProfileEntity> personProfileRepository;
    private final EntityMapper mapper;
    @Getter(AccessLevel.NONE)
    private PersistenceFacade delegate;

    public PersistenceFacadeImpl(EntityMapper mapper) {
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
        PrincipalProfileEntity maleProfile = PrincipalProfileEntity.builder()
                .email("bill@school.com")
                .location("Sweet Home")
                .build();
        AuthorityPersonEntity maleTeacher = AuthorityPersonEntity.builder()
                .title("Teacher").firstName("Bill").lastName("Clinton").gender("Mr")
                .build();
        PrincipalProfileEntity femaleProfile = PrincipalProfileEntity.builder()
                .email("hillary@school.com")
                .location("Sweet Home")
                .build();
        AuthorityPersonEntity femaleTeacher = AuthorityPersonEntity.builder()
                .title("Teacher").firstName("Hillary").lastName("Clinton").gender("Mrs")
                .build();
        log.info("Saving principal profiles set...");
        delegate.save(maleProfile);
        delegate.save(femaleProfile);
        maleTeacher.setProfileId(maleProfile.getId());
        femaleTeacher.setProfileId(femaleProfile.getId());
        log.info("Saving authority persons set...");
        delegate.save(maleTeacher);
        delegate.save(femaleTeacher);
        delegate.updateAccess(maleTeacher, "bill", "");
        delegate.updateAccess(femaleTeacher, "hillary", "");

        FacultyEntity languageFaculty = FacultyEntity.builder().name("Languages").build();
        FacultyEntity mathFaculty = FacultyEntity.builder().name("Math").build();
        FacultyEntity natureFaculty = FacultyEntity.builder().name("Nature").build();
        log.info("Saving faculty set...");
        delegate.save(languageFaculty);
        delegate.save(mathFaculty);
        delegate.save(natureFaculty);

        StudentsGroupEntity group = StudentsGroupEntity.builder().name("Pupils").build();
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

    /**
     * To update authority person's access parameters
     *
     * @param personId system-id of authority person
     * @param username new value of login's username
     * @param password new value of login's password
     * @return true if changes applied
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean updateAccess(Long personId, String username, String password) {
        log.debug("Updating authority person access...");
        log.debug("Looking for authority person with id: {}", personId);
        final AuthorityPersonEntity person = authorityPersonRepository.findById(personId).orElse(null);
        if (isNull(person)) {
            log.warn("Authority person with id {} is not found.", personId);
            return false;
        }
        final Long profileId = person.getProfileId();

        log.debug("Looking for principal profile with profileId: {}", profileId);
        final PrincipalProfileEntity profile = personProfileRepository.findById(profileId)
                .map(PrincipalProfileEntity.class::cast)
                .orElse(null);
        if (isNull(profile)) {
            log.warn("Principal profile with ID: {} is not found.", username);
            return false;
        }

        log.debug("Updating authority person profile...");
        try {
            profile.setLogin(username);
            profile.setSignature(profile.makeSignatureFor(password));
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot make signature", e);
            return false;
        }

        return username.equals(personProfileRepository.saveAndFlush(profile).getLogin());
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
