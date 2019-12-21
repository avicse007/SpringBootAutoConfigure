# SpringBootAutoConfigure

Spring Boot autoconfiguration
===============================

Spring Boot autoconfiguration represents a way to automatically configure a Spring application based on the dependencies that are present on the classpath.

### Creating a Custom Auto-Configuration

To create a custom auto-configuration, we need to create a class annotated as @Configuration and register it.

Let's create a custom configuration for a MySQL data source:

@Configuration
public class MySQLAutoconfiguration {
    //...
}
The next mandatory step is registering the class as an auto-configuration candidate, by adding the name of the class under the key org.springframework.boot.autoconfigure.EnableAutoConfiguration in the standard file resources/META-INF/spring.factories:

org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.baeldung.autoconfiguration.MySQLAutoconfiguration

If we want our auto-configuration class to have priority over other auto-configuration candidates, we can add the @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE) annotation.


Auto-configuration is designed using classes and beans marked with @Conditional annotations so that the auto-configuration or specific parts of it can be replaced.

 #### Note that the auto-configuration is only in effect if the auto-configured beans are not defined in the application. If you define your bean, then the default one will be overridden.

 ## 1. Class Conditions
Class conditions allow us to specify that a configuration bean will be included if a specified class is present using the @ConditionalOnClass annotation, or if a class is absent using the @ConditionalOnMissingClass annotation.

Let's specify that our MySQLConfiguration will only be loaded if the class DataSource is present, in which case we can assume the application will use a database:

@Configuration
@ConditionalOnClass(DataSource.class)
public class MySQLAutoconfiguration {
    //...
}


 ### 2. Bean Conditions
If we want to include a bean only if a specified bean is present or not, we can use the @ConditionalOnBean and @ConditionalOnMissingBean annotations.

To exemplify this, let's add an entityManagerFactory bean to our configuration class, and specify we only want this bean to be created if a bean called dataSource is present and if a bean called entityManagerFactory is not already defined:

@Bean
@ConditionalOnBean(name = "dataSource")
@ConditionalOnMissingBean
public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean em
      = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource());
    em.setPackagesToScan("com.baeldung.autoconfiguration.example");
    em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    if (additionalProperties() != null) {
        em.setJpaProperties(additionalProperties());
    }
    return em;
}
Let's also configure a transactionManager bean that will only be loaded if a bean of type JpaTransactionManager is not already defined:

@Bean
@ConditionalOnMissingBean(type = "JpaTransactionManager")
JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(entityManagerFactory);
    return transactionManager;
}

 ### 3. Property Conditions
The @ConditionalOnProperty annotation is used to specify if a configuration will be loaded based on the presence and value of a Spring Environment property.

First, let's add a property source file for our configuration that will determine where the properties will be read from:

@PropertySource("classpath:mysql.properties")
public class MySQLAutoconfiguration {
    //...
}
We can configure the main DataSource bean that will be used to create connections to the database in such a way that it will only be loaded if a property called usemysql is present.

We can use the attribute havingValue to specify certain values of the usemysql property that have to be matched.

Let's define the dataSource bean with default values that connect to a local database called myDb if the usemysql property is set to local:

@Bean
@ConditionalOnProperty(
  name = "usemysql", 
  havingValue = "local")
@ConditionalOnMissingBean
public DataSource dataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
  
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl("jdbc:mysql://localhost:3306/myDb?createDatabaseIfNotExist=true");
    dataSource.setUsername("mysqluser");
    dataSource.setPassword("mysqlpass");
 
    return dataSource;
}
If the usemysql property is set to custom, the dataSource bean will be configured using custom properties values for the database URL, user, and password:

@Bean(name = "dataSource")
@ConditionalOnProperty(
  name = "usemysql", 
  havingValue = "custom")
@ConditionalOnMissingBean
public DataSource dataSource2() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
         
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl(env.getProperty("mysql.url"));
    dataSource.setUsername(env.getProperty("mysql.user") != null
      ? env.getProperty("mysql.user") : "");
    dataSource.setPassword(env.getProperty("mysql.pass") != null
      ? env.getProperty("mysql.pass") : "");
         
    return dataSource;
}

The mysql.properties file will contain the usemysql property:

usemysql=local

If an application that uses the MySQLAutoconfiguration wishes to override the default properties, all it needs to do is add different values for the mysql.url, mysql.user and mysql.pass properties and the usemysql=custom line in the mysql.properties file.

 ### 4. Resource Conditions
Adding the @ConditionalOnResource annotation means that the configuration will only be loaded when a specified resource is present.

Let's define a method called additionalProperties() that will return a Properties object containing Hibernate-specific properties to be used by the entityManagerFactory bean, only if the resource file mysql.properties is present:

@ConditionalOnResource(
  resources = "classpath:mysql.properties")
@Conditional(HibernateCondition.class)
Properties additionalProperties() {
    Properties hibernateProperties = new Properties();
 
    hibernateProperties.setProperty("hibernate.hbm2ddl.auto", 
      env.getProperty("mysql-hibernate.hbm2ddl.auto"));
    hibernateProperties.setProperty("hibernate.dialect", 
      env.getProperty("mysql-hibernate.dialect"));
    hibernateProperties.setProperty("hibernate.show_sql", 
      env.getProperty("mysql-hibernate.show_sql") != null
      ? env.getProperty("mysql-hibernate.show_sql") : "false");
    return hibernateProperties;
}
We can add the Hibernate specific properties to the mysql.properties file:

mysql-hibernate.dialect=org.hibernate.dialect.MySQLDialect
mysql-hibernate.show_sql=true
mysql-hibernate.hbm2ddl.auto=create-drop

 ### 5. Custom Conditions
If we don't want to use any of the conditions available in Spring Boot, we can also define custom conditions by extending the SpringBootCondition class and overriding the getMatchOutcome() method.

Let's create a condition called HibernateCondition for our additionalProperties() method that will verify whether a HibernateEntityManager class is present on the classpath:

static class HibernateCondition extends SpringBootCondition {
 
    private static String[] CLASS_NAMES
      = { "org.hibernate.ejb.HibernateEntityManager", 
          "org.hibernate.jpa.HibernateEntityManager" };
 
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, 
      AnnotatedTypeMetadata metadata) {
  
        ConditionMessage.Builder message
          = ConditionMessage.forCondition("Hibernate");
        return Arrays.stream(CLASS_NAMES)
          .filter(className -> ClassUtils.isPresent(className, context.getClassLoader()))
          .map(className -> ConditionOutcome
            .match(message.found("class")
            .items(Style.NORMAL, className)))
          .findAny()
          .orElseGet(() -> ConditionOutcome
            .noMatch(message.didNotFind("class", "classes")
            .items(Style.NORMAL, Arrays.asList(CLASS_NAMES))));
    }
}
Then we can add the condition to the additionalProperties() method:

@Conditional(HibernateCondition.class)
Properties additionalProperties() {
  //...
}

 ### 6. Application Conditions
We can also specify that the configuration can be loaded only inside/outside a web context, by adding the @ConditionalOnWebApplication or @ConditionalOnNotWebApplication annotation.


 ### Annotations 

#### @EnableAutoConfiguration 


 #### @Conditional

 #### @ConditionalOnMissingBean

 #### @ConditionalOnBean

 #### @ConditionalOnClass

#### @ConditionalOnProperty

 #### @Import

 #### @AutoConfigureAfter
