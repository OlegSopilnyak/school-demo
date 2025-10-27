package oleg.sopilnyak.test.persistence.sql.entity.profile;

import static org.springframework.util.ObjectUtils.isEmpty;

import oleg.sopilnyak.test.school.common.model.PersonProfile;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DatabaseEntity: Base Entity for Profile Subsystem entities
 *
 * @see PersonProfile
 * @see PrincipalProfileEntity
 * @see StudentProfileEntity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder

@Entity
@Table(name = "profiles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.INTEGER)
public abstract class PersonProfileEntity implements PersonProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;
    private String photoUrl;
    private String email;
    private String phone;
    private String location;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_extras", joinColumns = @JoinColumn(name = "person_id"))
    @MapKeyColumn(name = "extra_key")
    @Column(name = "extra_value")
    private Map<String, String> extras;

    /**
     * To get the value of extra parameter by key
     *
     * @param key key of extra parameter
     * @return parameter's value or empty
     * @see Optional
     * @see Optional#empty()
     * @see Map#get(Object key)
     */
    @Override
    public Optional<String> getExtra(String key) {
        return isEmpty(extras) ? Optional.empty() : Optional.ofNullable(extras.get(key));
    }

    /**
     * To get keys of profile's extra parameters
     *
     * @return array of keys of extra parameters
     * @see Map#keySet()
     */
    @Override
    public String[] getExtraKeys() {
        return isEmpty(extras) ? new String[0] : extras.keySet().stream().sorted().toArray(String[]::new);
    }
}
