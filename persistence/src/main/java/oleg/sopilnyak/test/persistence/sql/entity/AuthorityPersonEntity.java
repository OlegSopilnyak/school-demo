package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
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
@Table(name = "authorities")
public class AuthorityPersonEntity implements AuthorityPerson {
    private static final SchoolEntityMapper mapper = Mappers.getMapper(SchoolEntityMapper.class);
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String title;
    private String firstName;
    private String lastName;
    private String gender;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, mappedBy = "group")
    private Set<FacultyEntity> facultyEntitySet;

    /**
     * To get the list of faculties where person is a dean
     *
     * @return list of faculties
     */
    public List<Faculty> getFaculties() {
        return isNull(facultyEntitySet) ? Collections.emptyList() :
                facultyEntitySet.stream()
                        .map(faculty -> (Faculty) faculty)
                        .sorted(Comparator.comparingLong(Faculty::getId))
                        .toList();
    }

    /**
     * To replace the list of faculties where person is a dean by new one
     *
     * @param faculties new faculties list
     */
    public void setFaculties(List<Faculty> faculties) {
        refreshStudentCourses();
        facultyEntitySet.clear();
        faculties.forEach(course -> facultyEntitySet.add(mapper.toEntity(course)));
    }

    private void refreshStudentCourses() {
        if (isNull(facultyEntitySet)) facultyEntitySet = new HashSet<>();
    }
}
