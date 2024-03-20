package oleg.sopilnyak.test.school.common.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = AFTER_CLASS)
public class MysqlTestModelFactory extends TestModelFactory {
    private static final String TEST_DB_DOCKER_IMAGE_NAME = "mysql:8.0";
    private static final String TEST_DB_DOCKER_CONTAINER_NAME = "school-test-database";
    @Container
    private static final MySQLContainer<?> database = new MySQLContainer<>(TEST_DB_DOCKER_IMAGE_NAME)
            .withCreateContainerCmdModifier(cmd ->
                    cmd.withName(TEST_DB_DOCKER_CONTAINER_NAME + "-" + UUID.randomUUID()));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

    @BeforeAll
    static void startContainer() {
        database.start();
    }

    @AfterAll
    static void stopContainer() {
        database.stop();
    }
}
