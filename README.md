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

@ConditionalOnProperty(name = "usemysql", havingValue = "local")

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

dataSource.setUsername(env.getProperty("mysql.user") != null? env.getProperty("mysql.user") : "");
    
dataSource.setPassword(env.getProperty("mysql.pass") != null? env.getProperty("mysql.pass") : "");
         
return dataSource;

}

The mysql.properties file will contain the usemysql property:

usemysql=local

If an application that uses the MySQLAutoconfiguration wishes to override the default properties, all it needs to do is add different values for the mysql.url, mysql.user and mysql.pass properties and the usemysql=custom line in the mysql.properties file.

 ### 4. Resource Conditions
Adding the @ConditionalOnResource annotation means that the configuration will only be loaded when a specified resource is present.

Let's define a method called additionalProperties() that will return a Properties object containing Hibernate-specific properties to be used by the entityManagerFactory bean, only if the resource file mysql.properties is present:

@ConditionalOnResource(resources = "classpath:mysql.properties")

@Conditional(HibernateCondition.class)

Properties additionalProperties() {

Properties hibernateProperties = new Properties();
 

hibernateProperties.setProperty("hibernate.hbm2ddl.auto", 

env.getProperty("mysql-hibernate.hbm2ddl.auto"));

hibernateProperties.setProperty("hibernate.dialect", 

env.getProperty("mysql-hibernate.dialect"));

hibernateProperties.setProperty("hibernate.show_sql", 

env.getProperty("mysql-hibernate.show_sql") != null? env.getProperty("mysql-hibernate.show_sql") : "false");

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
 
private static String[] CLASS_NAMES= { "org.hibernate.ejb.HibernateEntityManager", "org.hibernate.jpa.HibernateEntityManager" };
 
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


## Creating a Custom Auto-Configuration Example 

Here we will create a custom spring boot starter and then use this starter in our custom-app-spring-boot starter.

Spring Boot provides several starters for most of the open source projects. It’s possible to develop your own auto-configuration either for your projects or for your organization. We can also create Custom Starter with Spring Boot.


## 1.  Spring Boot Auto Configuration

#### 1.1 Locating Auto Configuration Classes

On starting our application, Spring Boot checks for a specific file named as spring.factories. This file is located in the META-INF directory. Here is an entry from the spring-boot-autoconfigure.


# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration,\

All auto configuration classes should list under EnableAutoConfiguration key in the spring.factories property file.Let’s pay our attention to few key points in the auto-configuration file entry.

#### 1. Based on the configuration file, Spring Boot will try to run all these configurations.

#### 2. Actual class configuration load will depend upon the classes on the classpath (e.g. if Spring find JPA in classpath, it will load JPA configuration class)


### 1.2 Conditional Annotation
Spring Boot use annotations to determine if an autoconfiguration class needs to be configured or not.The @ConditionalOnClass and @ConditionalOnMissingClass annotations help Spring Boot to determine if an auto-configuration class needs to be included or not.In the similar fashion @ConditionalOnBean and @ConditionalOnMissingBean are used for spring bean level autoconfiguration.

#### Here is an example of MailSenderAutoConfiguration class

@Configuration
@ConditionalOnClass({ MimeMessage.class, MimeType.class })
@ConditionalOnMissingBean(MailSender.class)
@Conditional(MailSenderCondition.class)
@EnableConfigurationProperties(MailProperties.class)
@Import(JndiSessionConfiguration.class)
public class MailSenderAutoConfiguration {
// required configurations
}

### 1.3 Conditional Annotation
Spring Boot use default values for the beans initialization. These defaults are based on the Spring environment properties.@EnableConfigurationProperties is declared with MailProperties class.Here is the code snippet for this class

@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {
   private static final Charset DEFAULT_CHARSET = 
   StandardCharsets.UTF_8;
   
   private Integer port; 
}

Properties defined in the MailProperties file are the default properties for MailSenderAutoConfiguration class while initializing beans. Spring Boot allows us to override these configuration properties using application.properties file. To override default port, we need to add the following entry in our application.properties file.

spring.mail.port=445 .  (prefix+property name)

 

## 2. Custom Starter with Spring Boot
To create our own custom starter, we require following components

#### The auto-configure module with auto configuration class.
The stater module which will bring all required dependencies using pom.xml
For this post, we are creating only a single module combining both auto-configuration code and starter module for getting all required dependencies. We will create a simple hello service stater with following features the hello-service-spring-boot-starter with HelloService which takes the name as input to say hello.HelloService will use the default configuration for the default name.
We will create Spring Boot demo application for using our hello-service-starter.
 

### 2.1 The Auto-Configure Module
The hello-service-spring-boot-starter will have the following classes and configurations

HelloServiveProperties file for default name.
HelloService interface and HelloServiceImpl class.
HelloServiceAutoConfiguration to create HelloService Bean.
The pom.xml file for bringing required dependencies to our custom starter. 
 

### 2.2 Property and Service Class
package com.javadevjournal.service;

public interface HelloService {

    void hello();
}

//Impl Service
public class HelloServiceImpl implements HelloService {

    @Override
    public void hello() {
        System.out.println("Hello from the default starter");
    }
}

#### 2.3 The AutoConfigure Module and Class
@Configuration
@ConditionalOnClass(HelloService.class)
public class HelloServiceAutoConfiguration {


    //conditional bean creation
    @Bean
    @ConditionalOnMissingBean
    public HelloService helloService(){

        return new HelloServiceImpl();
    }
}

The final piece of our auto-configuration is the addition of this class in the spring.factories property file located in the /src/main/resources/META-INF.

org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.javadevjournal.config.HelloServiceAutoConfiguration
Copy
On application startup

HelloServiceAutoConfiguration will run if HelloService class is available in the classpath. ( @ConditionOnClass annotation).
HelloService Bean will be created by Spring Boot if it is not available (@ConditionalOnMissingBean).
If developer defines their own HelloService bean, our customer starter will not create HelloService Bean.
 

### 2.4 The pom.xml
The last part of the custom starter is the pom.xml to bring in all the required dependencies.


<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starters</artifactId>
		<version>1.5.9.RELEASE</version>
	</parent>

	<groupId>com.javadevjournal</groupId>
	<artifactId>hello-service-spring-boot-starter</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<packaging>jar</packaging>

	<name>hello-service-spring-boot-starter</name>
	<description>Custom Starter for Spring Boot</description>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
		<java.version>1.8</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-autoconfigure</artifactId>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>


Let’s cover some interesting points in the pom.xml

We defined the parent as spring-boot-starters. We needed to pull in required dependencies.
For more information on parent pom. Please read our article Spring Boot Starter Parent  

 

### 2.4  Naming Convention
While creating a custom starter with Spring Boot, read below guidelines for the naming convention.

Your custom starter module should not start with Spring Boot.
Use name-spring-boot-starter as a guideline. In our case, we named our starter as hello-service-spring-boot-starter.
 

### 3. Using the custom starter
Let’s create a sample Spring Boot application to use our custom starter. Once We create starter app, add the custom starter as a dependency in pom.xml.

<dependency>
	<groupId>com.javadevjournal</groupId>
	<artifactId>hello-service-spring-boot-starter</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>

Here is our Spring Boot starter class

@SpringBootApplication
public class CustomStarterAppApplication implements CommandLineRunner {

	@Autowired
    HelloService service;

    public static void main(String[] args) {

		SpringApplication.run(CustomStarterAppApplication.class, args);
	}

    @Override
    public void run(String... strings) throws Exception {
        service.hello();
    }

If we run our application, you will see following output in the console

018-01-23 20:27:52.138  INFO 20441 --- [           main] s.c.a.AnnotationConfigApplicationContext : Refreshing org.springframework.context.annotation.....
Hello from the default starter
2018-01-23 20:27:52.620  INFO 20441 --- [           main] c.j.CustomStarterAppApplication          : Started CustomStarterAppApplication in ....



We have defined no HelloService is our demo application. When Spring Boot started, auto-configuration did not find any custom bean definition. Our custom starter auto configuration class created default “HelloService” bean. (as visible from the output).To understand Spring Boot auto-configuration logic and functionality, let’s create a custom HelloService bean in our sample application

public class CustomHelloService implements HelloService {

    @Override
    public void hello() {
        System.out.println("We are overriding our custom Hello Service");
    }
}

//bean bean definition
@SpringBootApplication
public class CustomStarterAppApplication implements CommandLineRunner {

	@Autowired
    HelloService service;

    public static void main(String[] args) {

		SpringApplication.run(CustomStarterAppApplication.class, args);
	}

    @Override
    public void run(String... strings) throws Exception {
        service.hello();
    }

    @Bean
    public  HelloService helloService(){
        return new CustomHelloService();
    }
}

Here is the output on running this application

2018-01-23 20:36:48.991  INFO 20529 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
We are overriding our custom Hello Service
2018-01-23 20:36:49.000  INFO 20529 --- [           main] c.j.CustomStarterAppApplication          : Started CustomStarterAppApplication in 0.701 seconds
Copy
When we defined our custom bean, Spring Boot default HelloService is no longer available. This enables developers to completely override default bean definition by creating/ providing their own bean definition.
