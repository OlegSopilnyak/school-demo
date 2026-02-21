package oleg.sopilnyak.test.persistence.configuration;

import oleg.sopilnyak.test.persistence.sql.PersistenceFacadeImpl;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;

import javax.sql.DataSource;

import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "oleg.sopilnyak.test.persistence.sql.repository")
public class PersistenceConfiguration {
    public static final String REAL_PERSISTENCE_FACADE = "realPersistenceFacade";
    public static final String PERSISTENCE_SQL_SCAN_PACKAGE = "oleg.sopilnyak.test.persistence.sql";
    private final Boolean showSQL;
    private final String databaseUnitName;

    public PersistenceConfiguration(
            @Value("${school.spring.jpa.show-sql:false}") Boolean showSQL,
            @Value("${school.persistence.unit.name:testDatabaseUnit}") String databaseUnitName
    ) {
        this.showSQL = showSQL;
        this.databaseUnitName = databaseUnitName;
    }

    @Bean
    public EntityMapper entityMapper() {
        return Mappers.getMapper(EntityMapper.class);
    }

    @Bean(REAL_PERSISTENCE_FACADE)
    public PersistenceFacade persistenceFacade() {
        return new PersistenceFacadeImpl(entityMapper());
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

        final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(showSQL);
        vendorAdapter.setShowSql(showSQL);

        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan(PERSISTENCE_SQL_SCAN_PACKAGE);
        factory.setPersistenceUnitName(databaseUnitName);
        factory.afterPropertiesSet();
        return factory;
    }
}
