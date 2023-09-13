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
@EqualsAndHashCode(exclude = {"courseSet"})
@ToString(exclude = {"courseSet"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
public class StudentEntity implements Student {
    private static SchoolEntityMapper mapper = Mappers.getMapper(SchoolEntityMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private String description;
    @ManyToMany(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    @JoinTable(name = "student_course",
            joinColumns = {@JoinColumn(referencedColumnName = "ID")},
            inverseJoinColumns = {@JoinColumn(referencedColumnName = "ID")}
    )
    private Set<CourseEntity> courseSet;

    /**
     * To get the list of courses, the student is registered to
     *
     * @return list of courses (ordered by course::id)
     */
    @Override
    public List<Course> getCourses() {
        return isNull(courseSet) ? Collections.emptyList() :
                courseSet.stream()
                        .map(course -> (Course) course)
                        .sorted(Comparator.comparingLong(Course::getId))
                        .toList();
    }

    /**
     * To replace the student's courses list by new one
     *
     * @param courses new student's courses list
     */
    public void setCourses(List<Course> courses) {
        refreshStudentCourses();
        courseSet.clear();
        courses.forEach(course -> {
            final CourseEntity studentCourse;
            courseSet.add(studentCourse = mapper.toEntity(course));
            studentCourse.add(this);
        });
    }

    /**
     * Register the student to the new course
     *
     * @param course new student's course
     * @return true if success
     */
    public boolean add(CourseEntity course) {
        refreshStudentCourses();
        courseSet.add(course);
        final Set<StudentEntity> students = course.getStudentSet();
        if (isNull(students) || !students.contains(this)) {
            return course.add(this) || true;
        }
        return true;
    }

    /**
     * Unregister the student from the course
     *
     * @param course course to remove
     * @return true if success
     */
    public boolean remove(CourseEntity course) {
        return isNull(courseSet) ? cannotUnregisterCourse() : courseUnregistered(course);
    }

    private void refreshStudentCourses() {
        if (isNull(courseSet)) courseSet = new HashSet<>();
    }

    private boolean courseUnregistered(CourseEntity course) {
        this.courseSet.remove(course);
        final Set<StudentEntity> students;

        if (nonNull(students = course.getStudentSet()) && students.contains(this)) {
            return course.remove(this) || true;
        }
        return true;
    }

    private boolean cannotUnregisterCourse() {
        this.courseSet = new HashSet<>();
        return false;
    }
}
