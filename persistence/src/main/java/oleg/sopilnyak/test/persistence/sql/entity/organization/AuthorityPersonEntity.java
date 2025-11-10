package oleg.sopilnyak.test.persistence.sql.entity.organization;

import lombok.*;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
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
@Table(name = "authorities", indexes = {
        @Index(name = "person_profile_id", columnList = "profileId", unique = true)
})
public class AuthorityPersonEntity implements AuthorityPerson {
    private static final EntityMapper mapper = Mappers.getMapper(EntityMapper.class);
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private Long profileId;
    private String title;
    private String firstName;
    private String lastName;
    private String gender;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "dean")
    private Set<FacultyEntity> facultyEntitySet;

    /**
     * To get the list of faculties where person is a dean
     *
     * @return list of faculties
     */
    public List<Faculty> getFaculties() {
        refreshFaculties();
        return getFacultyEntitySet().stream()
                .map(Faculty.class::cast)
                .sorted(Comparator.comparing(Faculty::getName))
                .toList();
    }

    /**
     * To replace the list of faculties where person is a dean by new one
     *
     * @param faculties new faculties list
     */
    public void setFaculties(List<Faculty> faculties) {
        refreshFaculties();
        new HashSet<>(getFacultyEntitySet()).forEach(this::remove);
        faculties.forEach(this::add);
    }

    /**
     * To add new faculty to the person
     *
     * @param faculty new faculty attach
     * @return true if success
     */
    public boolean add(Faculty faculty) {
        refreshFaculties();
        final Set<FacultyEntity> facultyEntities = getFacultyEntitySet();
        final Optional<FacultyEntity> existsFaculty = facultyEntities.stream()
                .filter(f -> equals(f, faculty)).findFirst();

        if (existsFaculty.isPresent()) {
            // faculty exists
            return false;
        }

        final FacultyEntity facultyToAdd = faculty instanceof FacultyEntity fe ? fe : mapper.toEntity(faculty);
        facultyEntities.add(facultyToAdd);
        facultyToAdd.setDean(this);
        return true;
    }

    /**
     * To remove the faculty from person (keeping it in the database)
     *
     * @param faculty faculty to detach
     * @return true if success
     */
    public boolean remove(Faculty faculty) {
        refreshFaculties();
        final Set<FacultyEntity> facultyEntities = getFacultyEntitySet();
        final FacultyEntity existsFaculty = facultyEntities.stream()
                .filter(f -> equals(f, faculty)).findFirst().orElse(null);

        if (isNull(existsFaculty)) {
            // faculty does not exist
            return false;
        }

        if (facultyEntities.removeIf(s -> s == existsFaculty)) {
            existsFaculty.setDean(null);
        }
        return true;
    }

    private void refreshFaculties() {
        if (isNull(facultyEntitySet)) facultyEntitySet = new HashSet<>();
    }

    private static boolean equals(Faculty first, Faculty second) {
        return !isNull(first) && !isNull(second) &&
                equals(first.getName(), second.getName())
                ;
    }

    private static boolean equals(String first, String second) {
        return isNull(first) ? isNull(second) : first.equals(second);
    }
}
