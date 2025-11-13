package oleg.sopilnyak.test.persistence.configuration;

import oleg.sopilnyak.test.persistence.sql.PersistenceFacadeImpl;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;

import javax.sql.DataSource;
import java.util.Properties;
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
    private final Boolean showSQL;
    private final String hbm2ddlAuto;

    public PersistenceConfiguration(
            @Value("${school.spring.jpa.show-sql:false}") Boolean showSQL,
            @Value("${school.hibernate.hbm2ddl.auto:validate}") String hbm2ddlAuto
    ) {
        this.showSQL = showSQL;
        this.hbm2ddlAuto = hbm2ddlAuto;
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
        factory.setPackagesToScan("oleg.sopilnyak.test.persistence.sql");
        factory.setPersistenceUnitName("schoolDatabaseUnit");

        final Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
        factory.setJpaProperties(jpaProperties);

        factory.afterPropertiesSet();
        return factory;
    }
}
