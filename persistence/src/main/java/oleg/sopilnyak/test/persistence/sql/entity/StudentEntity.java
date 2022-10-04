package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.*;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.mapstruct.factory.Mappers;

import javax.persistence.*;
import java.util.*;

@Data
@EqualsAndHashCode(exclude = {"courseSet"})
@ToString(exclude = {"courseSet"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
public class StudentEntity implements Student {
    private static EntityMapper mapper = Mappers.getMapper(EntityMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private String description;
    @ManyToMany(cascade = {CascadeType.ALL})
    @JoinTable(name = "student_course",
            joinColumns = {@JoinColumn(referencedColumnName = "ID")},
            inverseJoinColumns = {@JoinColumn(referencedColumnName = "ID")}
    )
    private Set<CourseEntity> courseSet;

    /**
     * To get the list of courses, the student is registered to
     *
     * @return list of courses
     */
    @Override
    public List<Course> getCourses() {
        return courseSet == null ? Collections.emptyList() :
                courseSet.stream()
                        .map(course -> (Course) course)
                        .sorted(Comparator.comparingLong(Course::getId))
                        .toList();
    }

    public void setCourses(List<Course> courses) {
        courseSet = courseSet == null ? new HashSet<>() : courseSet;
        courseSet.clear();
        courses.forEach(course -> {
            CourseEntity entity = mapper.toEntity(course);
            courseSet.add(entity);
            entity.add(this);
        });
    }

    public boolean add(CourseEntity course) {
        if (courseSet == null) {
            courseSet = new HashSet<>();
        }
        courseSet.add(course);
        Set<StudentEntity> students = course.getStudentSet();
        if (students == null || !students.contains(this)) {
            course.add(this);
        }
        return true;
    }

    public boolean remove(CourseEntity course) {
        if (courseSet == null) {
            courseSet = new HashSet<>();
            return false;
        }
        courseSet.remove(course);
        Set<StudentEntity> students = course.getStudentSet();
        if (students != null && students.contains(this)) {
            course.remove(this);
        }
        return true;
    }
}
