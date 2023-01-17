package edu.iu.uits.lms.courseattributeeditor.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration("caeDbConfig")
@EnableJpaRepositories(
        entityManagerFactoryRef = "caeEntityMgrFactory",
        transactionManagerRef = "caeTransactionMgr",
        basePackages = {"edu.iu.uits.lms.courseattributeeditor.repository"})
@EnableTransactionManagement
public class PostgresDBConfig {

    @Primary
    @Bean(name = "caeDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "caeEntityMgrFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean caeEntityMgrFactory(
            final EntityManagerFactoryBuilder builder,
            @Qualifier("caeDataSource") final DataSource dataSource) {
        // dynamically setting up the hibernate properties for each of the datasource.
        final Map<String, String> properties = new HashMap<>();
        return builder
                .dataSource(dataSource)
                .properties(properties)
                .packages("edu.iu.uits.lms.courseattributeeditor.model")
                .build();
    }

    @Bean(name = "caeTransactionMgr")
    @Primary
    public PlatformTransactionManager caeTransactionMgr(
            @Qualifier("caeEntityMgrFactory") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
