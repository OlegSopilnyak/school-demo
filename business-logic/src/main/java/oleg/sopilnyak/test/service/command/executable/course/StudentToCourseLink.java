package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;

/**
 * Bean to connect student and course
 */
@Data
@AllArgsConstructor
public class StudentToCourseLink {
    private Student student;
    private Course course;
}
