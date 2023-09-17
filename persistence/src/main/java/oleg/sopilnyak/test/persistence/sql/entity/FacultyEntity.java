package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.*;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;
import org.mapstruct.factory.Mappers;

import javax.persistence.*;
import java.util.*;

import static java.util.Objects.isNull;

@Data
@EqualsAndHashCode(exclude = {"courseEntitySet", "dean"})
@ToString(exclude = {"courseEntitySet", "dean"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "faculties")
public class FacultyEntity implements Faculty {
    private static final SchoolEntityMapper mapper = Mappers.getMapper(SchoolEntityMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    private AuthorityPersonEntity dean;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, mappedBy = "faculty")
    private Set<CourseEntity> courseEntitySet;

    /**
     * To get the list of courses, provided by faculty
     *
     * @return list of courses
     */
    public List<Course> getCourses() {
        refreshStudentCourses();
        return getCourseEntitySet().stream()
                .map(course -> (Course) course)
                .sorted(Comparator.comparing(Course::getName))
                .toList();
    }

    /**
     * To replace the faculty's courses list by new one
     *
     * @param courses new student's courses list
     */
    public void setCourses(List<Course> courses) {
        refreshStudentCourses();
        new HashSet<>(getCourseEntitySet()).forEach(this::remove);
        courses.forEach(this::add);
    }

    /**
     * To add new course to faculty
     *
     * @param course new course attach
     * @return true if success
     */
    public boolean add(Course course) {
        refreshStudentCourses();
        final Set<CourseEntity> courseEntities = getCourseEntitySet();
        final Optional<CourseEntity> existsCourse = courseEntities.stream()
                .filter(c -> equals(c, course)).findFirst();

        if (existsCourse.isPresent()) {
            // course exists
            return false;
        }

        final CourseEntity courseToAdd;
        courseEntities.add(courseToAdd = mapper.toEntity(course));
        courseToAdd.setFaculty(this);
        return true;
    }

    /**
     * To remove the course from faculty (keeping it in the database)
     *
     * @param course course to detach
     * @return true if success
     */
    public boolean remove(Course course) {
        refreshStudentCourses();
        final Set<CourseEntity> courseEntities = getCourseEntitySet();
        final CourseEntity existsCourse = courseEntities.stream()
                .filter(c -> equals(c, course)).findFirst().orElse(null);

        if (isNull(existsCourse)) {
            // course does not exist
            return false;
        }

        if (courseEntities.removeIf(c -> c == existsCourse)) {
            existsCourse.setFaculty(null);
        }
        return true;
    }

    private void refreshStudentCourses() {
        if (isNull(courseEntitySet)) courseEntitySet = new HashSet<>();
    }

    private static boolean equals(Course first, Course second) {
        return !isNull(first) && !isNull(second) &&
                equals(first.getName(), second.getName()) &&
                equals(first.getDescription(), second.getDescription())
                ;
    }

    private static boolean equals(String first, String second) {
        return isNull(first) ? isNull(second) : first.equals(second);
    }
}
