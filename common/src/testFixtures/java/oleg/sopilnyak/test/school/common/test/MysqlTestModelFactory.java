package oleg.sopilnyak.test.school.common.test;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@DataJpaTest
@TestPropertySource(properties = {"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DirtiesContext
public abstract class MysqlTestModelFactory extends TestModelFactory {
    private static final String TEST_DB_DOCKER_IMAGE_NAME = "mysql:8.0";
    private static final String TEST_DB_DOCKER_CONTAINER_NAME = "school-test-database";
    private static final MySQLContainer<?> database;

    static {
        database = new MySQLContainer<>(TEST_DB_DOCKER_IMAGE_NAME)
                .withCreateContainerCmdModifier(cmd ->
                        cmd.withName(TEST_DB_DOCKER_CONTAINER_NAME + "-" + UUID.randomUUID()))
        ;
        database.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

}
