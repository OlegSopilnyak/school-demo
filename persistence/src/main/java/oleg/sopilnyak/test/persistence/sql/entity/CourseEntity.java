package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.*;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.mapstruct.factory.Mappers;

import javax.persistence.*;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Data
@EqualsAndHashCode(exclude = {"studentSet"})
@ToString(exclude = {"studentSet"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class CourseEntity implements Course {
    private static SchoolEntityMapper mapper = Mappers.getMapper(SchoolEntityMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String name;
    private String description;
    @ManyToMany(mappedBy = "courseSet", fetch = FetchType.LAZY)
    private Set<StudentEntity> studentSet;

    /**
     * To get the list of students enrolled to the course
     *
     * @return list of students (ordered by student::id)
     */
    @Override
    public List<Student> getStudents() {
        return isNull(studentSet) ? Collections.emptyList() :
                studentSet.stream()
                        .map(student -> (Student) student)
                        .sorted(Comparator.comparingLong(Student::getId))
                        .toList();
    }

    /**
     * To enroll the students to the course
     *
     * @param students students list to enroll
     */
    public void setStudents(List<Student> students) {
        refreshCourseStudents();
        studentSet.clear();
        students.forEach(student -> {
            final StudentEntity courseStudent;
            studentSet.add(courseStudent = mapper.toEntity(student));
            courseStudent.add(this);
        });
    }

    /**
     * To enroll the student to the course
     *
     * @param student to enroll
     * @return true if success
     */
    public boolean add(StudentEntity student) {
        refreshCourseStudents();
        studentSet.add(student);

        final Set<CourseEntity> courses;
        if (isNull(courses = student.getCourseSet()) || !courses.contains(this)) {
            return student.add(this) || true;
        }
        return true;
    }

    /**
     * To un-enroll the student from the course
     *
     * @param student to un-enroll
     * @return true if success
     */
    public boolean remove(StudentEntity student) {
        return isNull(studentSet) ? cannotUnEnrollStudent() : studentUnEnrolled(student);
    }

    private boolean studentUnEnrolled(StudentEntity student) {
        studentSet.remove(student);
        final Set<CourseEntity> courses;
        if (nonNull(courses = student.getCourseSet()) && courses.contains(this)) {
            return student.remove(this) || true;
        }
        return true;
    }

    private boolean cannotUnEnrollStudent() {
        studentSet = new HashSet<>();
        return false;
    }

    private void refreshCourseStudents() {
        if (isNull(studentSet)) studentSet = new HashSet<>();
    }

}
