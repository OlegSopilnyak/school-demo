package oleg.sopilnyak.test.persistence.sql.entity.education;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import oleg.sopilnyak.test.persistence.sql.entity.organization.FacultyEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.mapstruct.factory.Mappers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(exclude = {"studentSet", "faculty"})
@ToString(exclude = {"studentSet", "faculty"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class CourseEntity implements Course {
    private static EntityMapper mapper = Mappers.getMapper(EntityMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String name;
    private String description;
    @ManyToMany(mappedBy = "courseSet", fetch = FetchType.LAZY)
    private Set<StudentEntity> studentSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private FacultyEntity faculty;

    /**
     * To get the list of students enrolled to the course
     *
     * @return list of students (ordered by student::id)
     */
    @Override
    public List<Student> getStudents() {
        refreshCourseSet();
        return getStudentSet().stream()
                .map(Student.class::cast)
                .sorted(Comparator.comparing(Student::getFullName))
                .toList();
    }

    /**
     * To enroll the students to the course
     *
     * @param students students list to enroll
     */
    public void setStudents(List<Student> students) {
        refreshCourseSet();
        new HashSet<>(getStudentSet()).forEach(this::remove);
        students.forEach(this::add);
    }

    /**
     * To enroll the student to the course
     *
     * @param student to enroll
     * @return true if success
     */
    public boolean add(final Student student) {
        refreshCourseSet();
        final Set<StudentEntity> studentEntities = getStudentSet();
        final boolean isExistsStudent =
                studentEntities.stream().anyMatch(se -> equals(se, student));

        if (isExistsStudent) {
            // student exists
            return false;
        }

        final StudentEntity studentToAdd = student instanceof StudentEntity se ? se : mapper.toEntity(student);
        refresh(studentToAdd);
        studentEntities.add(studentToAdd);
        studentToAdd.getCourseSet().add(this);
        return true;
    }


    /**
     * To un-enroll the student from the course
     *
     * @param student to un-enroll
     * @return true if success
     */
    public boolean remove(final Student student) {
        refreshCourseSet();
        final Set<StudentEntity> studentEntities = getStudentSet();
        final StudentEntity existsStudent = studentEntities.stream()
                .filter(se -> equals(se, student)).findFirst().orElse(null);

        if (isNull(existsStudent)) {
            // student does not exist
            return false;
        }

        if (studentEntities.removeIf(se -> se == existsStudent)) {
            existsStudent.getCourseSet().removeIf(c -> c == this);
        }
        return true;
    }

    private void refreshCourseSet() {
        if (isNull(studentSet)) studentSet = new HashSet<>();
    }

    private void refresh(final StudentEntity student) {
        if (isNull(student.getCourseSet())) student.setCourseSet(new HashSet<>());
    }

    private static boolean equals(Student first, Student second) {
        return !isNull(first) && !isNull(second) &&
                equals(first.getFullName(), second.getFullName()) &&
                equals(first.getDescription(), second.getDescription()) &&
                equals(first.getGender(), second.getGender())
                ;
    }

    private static boolean equals(String first, String second) {
        return isNull(first) ? isNull(second) : first.equals(second);
    }

}
