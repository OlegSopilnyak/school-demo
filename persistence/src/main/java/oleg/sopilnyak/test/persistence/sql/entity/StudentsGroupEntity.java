package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import org.mapstruct.factory.Mappers;

import javax.persistence.*;
import java.util.*;

import static java.util.Objects.isNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_groups")
public class StudentsGroupEntity implements StudentsGroup {
    private static final EntityMapper mapper = Mappers.getMapper(EntityMapper.class);
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String name;
    @ManyToOne
    private StudentEntity leader;
    @OneToMany
    private Set<StudentEntity> studentEntitySet;

    /**
     * To get the leader of the group
     *
     * @return leader's instance
     */
    public Student getLeader() {
        return leader;
    }

    /**
     * To get the list of students attached to the group
     *
     * @return list of students
     */
    public List<Student> getStudents() {
        return isNull(studentEntitySet) ? Collections.emptyList() :
                studentEntitySet.stream()
                        .map(student -> (Student) student)
                        .sorted(Comparator.comparingLong(Student::getId))
                        .toList();
    }

    public void setStudents(List<Student> students) {
        refreshStudentCourses();
        studentEntitySet.clear();
        students.forEach(course -> studentEntitySet.add(mapper.toEntity(course)));
    }

    private void refreshStudentCourses() {
        if (isNull(studentEntitySet)) studentEntitySet = new HashSet<>();
    }
}
