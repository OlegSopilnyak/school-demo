package oleg.sopilnyak.test.school.common.test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.ObjectUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@TestPropertySource(properties = {"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DirtiesContext
@SuppressWarnings("unchecked")
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
        final Supplier<Object> jdbcUrlSupplier =
                () -> database.getJdbcUrl()  + "?verifyServerCertificate=false&useSSL=false&requireSSL=false";
        registry.add("spring.datasource.url", jdbcUrlSupplier);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    protected <T> T findEntity(Class<T> entityClass, Object pk) {
        return findEntity(entityClass, pk, null);
    }

    protected <T> T findEntity(Class<T> entityClass, Object pk, Consumer<T> refreshEntity) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            T entity = em.find(entityClass, pk);
            if (entity != null) {
                em.refresh(entity);
                if (refreshEntity != null) {
                    refreshEntity.accept(entity);
                }
            }
            return entity;
        }
    }

    protected <T> T findEntityByFK(Class<T> entityClass, String fkAttributeName, Object fk) {
        return findEntityByFK(entityClass, fkAttributeName, fk, null);
    }

    protected <T> T findEntityByFK(Class<T> entityClass, String fkAttributeName, Object fk, Consumer<T> refreshEntity) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
            Root<T> entityRoot = criteriaQuery.from(entityClass);
            criteriaQuery.select(entityRoot).where(criteriaBuilder.equal(entityRoot.get(fkAttributeName), fk));
            List<T> entities = em.createQuery(criteriaQuery).getResultList();
            if (ObjectUtils.isEmpty(entities)) {
                return null;
            }
            final T entity = entities.get(0);
            if (refreshEntity != null) {
                refreshEntity.accept(entity);
            }
            return entity;
        }
    }


    protected <T> T deleteEntity(Class<T> entityClass, Object pk) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            T entity = em.find(entityClass, pk);
            if (entity != null) {
                EntityTransaction transaction = em.getTransaction();
                transaction.begin();
                em.remove(entity);
                em.flush();
                transaction.commit();
                em.clear();
                return em.find(entityClass, pk);
            }
            return null;
        }
    }

    protected <T> void deleteEntities(Class<T> entityClass) {
        EntityManager em = entityManagerFactory.createEntityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<T> entityRoot = criteriaQuery.from(entityClass);
        criteriaQuery.select(entityRoot);
        Query query = em.createQuery(criteriaQuery);
        try{
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            List<T> entities = query.getResultList();
            if (entities != null && !entities.isEmpty()) {
                entities.forEach(em::remove);
                em.flush();
            }
            em.clear();
            transaction.commit();
        } finally {
            em.close();
        }
    }

    protected <T> boolean isEmpty(Class<T> entityClass) {
        return findAllFor(entityClass).isEmpty();
    }

    protected <T> List<T>  findAllFor(Class<T> entityClass) {
        return findAllFor(entityClass, null);
    }
        protected <T> List<T>  findAllFor(Class<T> entityClass, Consumer<T> refreshEntity) {
        EntityManager em = entityManagerFactory.createEntityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<T> entityRoot = criteriaQuery.from(entityClass);
        criteriaQuery.select(entityRoot);
        TypedQuery<T> query = em.createQuery(criteriaQuery);
        try{
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            List<T> entities = query.getResultList();
            if (refreshEntity != null) {
                entities.forEach(refreshEntity::accept);
            }
            em.clear();
            transaction.commit();
            return entities;
        } finally {
            em.close();
        }
    }

    protected <T> T transactional(Supplier<T> activity) {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            T result = activity.get();
            em.flush();
            transaction.commit();
            return result;
        }
    }

    @Autowired
    PlatformTransactionManager ptm;
    protected Object transactCommand(final Object targetBean) {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(ptm);
        // Configure transaction attributes for methods if needed
        // Example:
         Properties transactionAttributes = new Properties();
         transactionAttributes.setProperty("execute*", "PROPAGATION_REQUIRES_NEW");
         interceptor.setTransactionAttributes(transactionAttributes);
        interceptor.afterPropertiesSet();
        final Object realBean = MockUtil.isSpy(targetBean)
                ?
                Mockito.mockingDetails(targetBean).getMockCreationSettings().getSpiedInstance()
                :
                targetBean;
        ProxyFactory proxyFactory = new ProxyFactory(realBean);
        proxyFactory.addAdvice(interceptor);
        return proxyFactory.getProxy();
    }
}
