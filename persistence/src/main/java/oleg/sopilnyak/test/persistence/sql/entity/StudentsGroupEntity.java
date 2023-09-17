package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import org.mapstruct.factory.Mappers;

import javax.persistence.*;
import java.util.*;

import static java.util.Objects.isNull;
import static org.springframework.util.ObjectUtils.isEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "studentsGroups")
public class StudentsGroupEntity implements StudentsGroup {
    private static final SchoolEntityMapper mapper = Mappers.getMapper(SchoolEntityMapper.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String name;
    private int leaderIndex;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, mappedBy = "group")
    private Set<StudentEntity> studentEntitySet;

    /**
     * To get the leader of the group
     *
     * @return leader's instance
     */
    public Student getLeader() {
        return leaderIndex < 0 || isEmpty(studentEntitySet) ? null : getStudents().get(leaderIndex);
    }

    /**
     * To set the leader for the group
     *
     * @param leader leader instance (should be from students list)
     */
    public void setLeader(Student leader) {
        if (isNull(leader) || isEmpty(studentEntitySet)) {
            leaderIndex = -1;
        } else {
            final List<Student> students = getStudents();
            final Long index = students.stream().takeWhile(student -> !equals(student, leader)).count();
            leaderIndex = index >= students.size() ? 0 : index.intValue();
        }
    }

    /**
     * To get the list of students attached to the group
     *
     * @return list of students
     */
    public List<Student> getStudents() {
        refreshStudentCourses();
        return getStudentEntitySet().stream()
                .map(student -> (Student) student)
                .sorted(Comparator.comparing(Student::getFullName))
                .toList();
    }

    /**
     * To update the list of students attached to the group
     *
     * @param students new students list
     */
    public void setStudents(List<Student> students) {
        refreshStudentCourses();
        final Student oldLeader = getLeader();
        new HashSet<>(getStudentEntitySet()).forEach(this::remove);
        students.forEach(this::add);
        setLeader(oldLeader);
    }

    /**
     * To remove student from students list (keeps student exists)
     *
     * @param student instance to detach from group
     * @return true if success
     */
    public boolean remove(Student student) {
        refreshStudentCourses();
        final Set<StudentEntity> studentEntities = getStudentEntitySet();
        final StudentEntity extraStudent = studentEntities.stream()
                .filter(s -> equals(s, student))
                .findFirst().orElse(null);
        if (isNull(extraStudent)) {
            // student not exists
            return false;
        }
        final Student currentLeader = getLeader();
        if (studentEntities.removeIf(s -> s == extraStudent)) {
            extraStudent.setGroup(null);
            setLeader(currentLeader);
        }
        return true;
    }

    /**
     * To add student to the group
     *
     * @param student instance to add to the group
     * @return true if success
     */
    public boolean add(Student student) {
        refreshStudentCourses();
        final Set<StudentEntity> studentEntities = getStudentEntitySet();
        final Optional<StudentEntity> existsStudent = studentEntities.stream()
                .filter(s -> equals(s, student)).findFirst();
        if (existsStudent.isPresent()) {
            // student exists
            return false;
        }
        final Student currentLeader = getLeader();
        final StudentEntity studentToAdd;
        studentEntities.add(studentToAdd = mapper.toEntity(student));
        studentToAdd.setGroup(this);
        setLeader(currentLeader);
        return true;
    }

    // private methods
    private static boolean equals(Student first, Student second) {
        return !isNull(first) && !isNull(second) &&
                equals(first.getFullName(), second.getFullName()) &&
                equals(first.getGender(), second.getGender()) &&
                equals(first.getDescription(), second.getDescription())
                ;
    }

    private static boolean equals(String first, String second) {
        return isNull(first) ? isNull(second) : first.equals(second);
    }

    private void refreshStudentCourses() {
        if (isNull(studentEntitySet)) studentEntitySet = new HashSet<>();
    }
}
