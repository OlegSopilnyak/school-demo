package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;
import org.mapstruct.factory.Mappers;

import javax.persistence.*;
import java.util.*;

import static java.util.Objects.isNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "faculties")
public class FacultyEntity implements Faculty {
    private static final SchoolEntityMapper mapper = Mappers.getMapper(SchoolEntityMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    private AuthorityPersonEntity dean;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, mappedBy = "faculty")
    private Set<CourseEntity> courseEntitySet;
    @ManyToOne(fetch = FetchType.LAZY)
    private AuthorityPersonEntity group;

    /**
     * To get the list of courses, provided by faculty
     *
     * @return list of courses
     */
    public List<Course> getCourses() {
        return isNull(courseEntitySet) ? Collections.emptyList() :
                courseEntitySet.stream()
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
        courseEntitySet.clear();
        courses.forEach(course -> courseEntitySet.add(mapper.toEntity(course)));
    }

    /**
     * To add new course to faculty
     *
     * @param course new course instance
     * @return true if success
     */
    public boolean add(CourseEntity course) {
        refreshStudentCourses();
        courseEntitySet.add(course);
        return true;
    }

    /**
     * To remove the course from faculty
     *
     * @param course course to remove
     * @return true if success
     */
    public boolean remove(CourseEntity course) {
        return isNull(courseEntitySet) ? cannotRemoveCourse() : courseRemoved(course);
    }

    private void refreshStudentCourses() {
        if (isNull(courseEntitySet)) courseEntitySet = new HashSet<>();
    }

    private boolean courseRemoved(CourseEntity course) {
        courseEntitySet.remove(course);
        return true;
    }

    private boolean cannotRemoveCourse() {
        courseEntitySet = new HashSet<>();
        return false;
    }
}
