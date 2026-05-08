package com.travel.system.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.travel.system.mapper", sqlSessionFactoryRef = "sqliteSqlSessionFactory")
public class SQLiteDataSourceConfig {
    /**
     * 创建 MyBatis 使用的 SqlSessionFactory，并加载 XML mapper，使 MyBatis 能通过 SQLite 数据源执行 SQL。
     */

    @Bean(name = "sqliteSqlSessionFactory")
    public SqlSessionFactory sqliteSqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml"));
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        return bean.getObject();
    }
    /**
     * 创建 SQLite 数据源对应的事务管理器，保证涉及数据库写入的方法可以按事务提交或回滚。
     */

    @Bean(name = "sqliteTransactionManager")
    public DataSourceTransactionManager sqliteTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
    /**
     * 创建线程安全的 SqlSessionTemplate，供 MyBatis mapper 在运行时执行具体 SQL。
     */

    @Bean(name = "sqliteSqlSessionTemplate")
    public SqlSessionTemplate sqliteSqlSessionTemplate(@Qualifier("sqliteSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
