package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.*;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.mapstruct.factory.Mappers;

import javax.persistence.*;
import java.util.*;

@Data
@EqualsAndHashCode(exclude = {"studentSet"})
@ToString(exclude = {"studentSet"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class CourseEntity implements Course {
    private static EntityMapper mapper = Mappers.getMapper(EntityMapper.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String name;
    private String description;
    @ManyToMany(mappedBy = "courseSet")
    private Set<StudentEntity> studentSet;

    /**
     * To get the list of students enrolled to the course
     *
     * @return list of students
     */
    @Override
    public List<Student> getStudents() {
        return studentSet == null ? Collections.EMPTY_LIST :
                studentSet.stream()
                        .map(student -> (Student) student)
                        .sorted(Comparator.comparingLong(Student::getId))
                        .toList();
    }

    public void setStudents(List<Student> students) {
        studentSet = studentSet == null ? new HashSet<>() : studentSet;
        studentSet.clear();
        students.forEach(student -> {
            StudentEntity entity = mapper.toEntity(student);
            studentSet.add(entity);
            entity.add(this);
        });
    }

    public boolean add(StudentEntity student) {
        if (studentSet == null) {
            studentSet = new HashSet<>();
        }
        studentSet.add(student);
        Set<CourseEntity> courses = student.getCourseSet();
        if (courses == null || !courses.contains(this)) {
            student.add(this);
        }
        return true;
    }

    public boolean remove(StudentEntity student) {
        if (studentSet == null) {
            studentSet = new HashSet<>();
            return false;
        }
        studentSet.remove(student);
        Set<CourseEntity> courses = student.getCourseSet();
        if (courses != null && courses.contains(this)) {
            student.remove(this);
        }
        return true;
    }
}
