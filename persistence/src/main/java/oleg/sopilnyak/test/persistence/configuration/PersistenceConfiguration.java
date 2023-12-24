package oleg.sopilnyak.test.persistence.configuration;

import oleg.sopilnyak.test.persistence.sql.PersistenceFacadeImpl;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@SuppressWarnings("SpellCheckingInspection")
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "oleg.sopilnyak.test.persistence.sql.repository")
public class PersistenceConfiguration {

    public static final String REAL_PERSISTENCE_FACADE = "realPersistenceFacade";
    @Value("${school.spring.jpa.show-sql:false}")
    private Boolean showSQL;
    @Value("${school.hibernate.hbm2ddl.auto:validate}")
    private String hbm2ddlAuto;

    @Bean
    public SchoolEntityMapper entityMapper() {
        return Mappers.getMapper(SchoolEntityMapper.class);
    }

    @Bean(REAL_PERSISTENCE_FACADE)
    public PersistenceFacade persistenceFacade() {
        return new PersistenceFacadeImpl(entityMapper());
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(showSQL);
        vendorAdapter.setShowSql(showSQL);

        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("oleg.sopilnyak.test.persistence.sql");
        factory.setPersistenceUnitName("schoolDatabaseUnit");

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
        factory.setJpaProperties(jpaProperties);

        factory.afterPropertiesSet();
        factory.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
        return factory;
    }
}
