package oleg.sopilnyak.test.persistence.configuration;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Objects;

@AllArgsConstructor
@Configuration
public class MySqlDataSourceConfiguration {
    private final Environment env;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(Objects.requireNonNull(env.getProperty("school.jdbc.driverClassName")));
        dataSource.setUrl(env.getProperty("school.jdbc.url"));
        dataSource.setUsername(env.getProperty("school.jdbc.username"));
        dataSource.setPassword(env.getProperty("school.jdbc.password"));
        return dataSource;
    }
}
