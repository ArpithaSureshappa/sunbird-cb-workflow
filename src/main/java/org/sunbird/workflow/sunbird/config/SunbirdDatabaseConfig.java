package org.sunbird.workflow.sunbird.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;


@Configuration
@EnableJpaRepositories(
        basePackages = "org.sunbird.workflow.sunbird.repo",
        entityManagerFactoryRef = "sunbirdEntityManagerFactory",
        transactionManagerRef = "sunbirdTransactionManager")
@EnableTransactionManagement
public class SunbirdDatabaseConfig {

    @Autowired
    Environment env;

    @Bean(name = "sunbirdDataSource")
    @ConfigurationProperties(prefix = "spring.db2.datasource")
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.db2.datasource.driver-class-name"));
        dataSource.setUrl(env.getProperty("spring.db2.datasource.jdbcUrl"));
        dataSource.setUsername(env.getProperty("spring.db2.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.db2.datasource.password"));
        return dataSource;
    }

    @Bean(name = "sunbirdEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.sunbird.workflow.sunbird.entity");
        factory.setDataSource(dataSource());
        return factory;
    }

    @Bean(name = "sunbirdTransactionManager")
    public JpaTransactionManager sunbirdTransactionManager(
            @Qualifier("sunbirdEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}


