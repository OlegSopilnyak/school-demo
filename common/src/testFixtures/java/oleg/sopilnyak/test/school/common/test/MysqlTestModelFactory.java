package oleg.sopilnyak.test.school.common.test;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
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

    static {
        // starting container
        database.start();
    }

}
