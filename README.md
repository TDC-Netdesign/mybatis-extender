# mybatis-extender
OSGi-based mybatis following the extender pattern. Expose your mapper interfaces and get an implementation in return, just like the guice or spring integrations.

### How does it work?
To use the extender, the user must register a Mybatis Configuration as a service. This configuration contains the URL, username, password and mappers to use with Mybatis. When the configuration has been registered, the Mybatis Extender will itself register each mapper as a service, under the mappers class. These services can then be consumed by the application in order to communicate with the database.

#### Important note
In order for the Mybatis-Extender to do it's job, it is important to create a class extending the [MybatisConfiguration Interface](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MybatisConfiguration.java "Go to source"). Do not under any circumstance create an anonymous subclass of the MybatisConfiguration and expose that. It must _**always**_ be a named class.

### Registering a Mybatis Configuration
Registering a MyBatis Configuration is done simply by registering an OSGi service based on the [MybatisConfiguration Class](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MybatisConfiguration.java "Go to source"). The interface contains a number of methods which will each return part of the required configuration.

+ ##### List<Class> getMappers()
  Returns the class of each [mapper interface](http://www.mybatis.org/mybatis-3/sqlmap-xml.html "Mybatis Documentation") to use. A mapper can be either XML or annotation based. When the MybatisConfiguration service is registered, each mapper will be exposed as a service, which can then be consumed to communicate with the database.
+ ##### Map<Class,TypeHandler> getTypeHandlers()
  Returns the [TypeHandlers](http://www.mybatis.org/mybatis-3/configuration.html#typeHandlers "Mybatis Documentation") to register for this Mybatis instance. The TypeHandlers will be registered along with the Mappers.
+ ##### String getJDBCUrl()
  The full URL to the database to connect to. Database depenent. Must include the name of the database, and not just the database server.
+ ##### String getUser()
  The username to use when connecting to the database
+ ##### String getPassword()
  The password to use when connecting to the database
+ ##### Class getDriver()
  The class of the driver to use to connect to the database. The driver should be placed either in the bundle registering the MybatisConfiguration, or the driver should be available to that bundle via an import/export.

### Consuming the correct services
In case of several MybatisConfiguration services being used in a single OSGi container, several versions of the same Service might be exposed. To solve this, every service exposed by Mybatis-Extender uses a property called **configurationClass** to identify which MybatisConfiguration the service is exposed for. The property is also specified in the [MybatisConfiguration Class](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MybatisConfiguration.java "Go to source").

    public static final String CONFIGURATION_CLASS="configurationClass"
    
The property will be the implementation class of the MybatisConfiguration. So if a bundle registers a MybatisConfiguration with an implementation class of "org.example.MyClass", every Mapper and configuration interface exposed by the Mybatis-Extender in response to that will have **configurationClass=org.example.MyClass** set on them.

### Reconnecting
In the case that the database username or password changes, the Mybatis Extender has a built in way to prompt it to reconnect programatically. When a MybatisConfiguration is built, a service is exposed under the [MybatisCommands interface](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MybatisCommands.java "Go to source"). This interface contains a single method, which when called will cause the Mybatis Extender to rebuild the Environment part of its configuration. It does this by rereading the **user**, **password** and **url** parts of the MybatisConfiguration which it will then use to create a new Environment. Each instance of MybatisConfiguration will cause its own MybatisCommands service to be registered. In order to tell these apart, they are registered with a property called **configurationClass**. The property points to the implementation class of the MybatisConfiguration it was registered for.

### Using the builtin Mybatis Migrations support
Mybatis includes a subproject used to handle changes to the database called [Migrations](http://www.mybatis.org/migrations/ "Documentation"). The Mybatis Extender can also expose this functionality if needed.
In order to get Migrations Support, instead of registering a [MybatisConfiguration](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MybatisConfiguration.java "Go to source"), you should register its extended interface [MigrationConfiguration](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MigrationConfiguration.java "Go to source") instead. The MigrationConfiguration contains extra methods beyond what is found in the MybatisConfiguration.
When Migrations is activated, a new [MigrationCommand service](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MigrationCommands.java "Go to source") is exposed, which allows access to the Migrations commands on the database. In case of several MybatisConfiguration services being used, there will be an equal number of identical MigrationCommand services. In order for a consumer to tell which MigrationCommand service belongs to it, the **configurationClass** property used on the other services exposed by Mybatis-Extender is also used here. Alternatively, the MigrationsConfiguration class also has a method called **getMigrationRemoteRegistrationType**. This method allows the user to change the Class the Commands is registered on. In order to use the method correctly, extend the [MigrationConfiguration](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MigrationConfiguration.java "Go to source") interface, and don't add any new methods or parameters. Then return the class of that interface in the **getMigrationRemoteRegistrationType** method. The Migration commands will now be exposed as before, but will ALSO be exposed on the class defined in the method.




+ ##### BootstrapScript getBootstrapScript()
  The [Bootstrap Script](https://github.com/mybatis/migrations/blob/master/src/main/java/org/apache/ibatis/migration/BootstrapScript.java "Go to source") which defines the default state of your database. Read more on [bootstrap scripts here](http://www.mybatis.org/migrations/bootstrap.html "Go to documentation").
+ ##### OnAbortScript getOnAbortScript()
  The [Abort Script](https://github.com/mybatis/migrations/blob/master/src/main/java/org/apache/ibatis/migration/OnAbortScript.java "Go to source") to use when something goes wrong. This can be null. 
+ ##### List<MigrationScript> getMigrationScripts()
  This method returns the [Migration Scripts](https://github.com/mybatis/migrations/blob/master/src/main/java/org/apache/ibatis/migration/MigrationScript.java "Go to source") which should be available to the Migrations framework. 
+ ##### Class<? extends MigrationCommands> getMigrationRemoteRegistrationType()
  Specifying a class here will cause the [MigrationCommand service](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MigrationCommands.java "Go to source") exposed by Mybatis-Extender  to be exposed on the returned class as well. In order to use this method correctly, you must first create a new interface which extends MigrationCommands, and then return the class of that in this method.
+ ##### DatabaseOperationOption getDatabaseOptions()
  Use this method to define the [Database Options](https://github.com/mybatis/migrations/blob/master/src/main/java/org/apache/ibatis/migration/options/DatabaseOperationOption.java "Go to source") which should be used by Migrations.
+ ##### void executeOnRegistration(MigrationCommands migrator)
  This method is executed just before the [MigrationCommand service](https://github.com/TDC-Netdesign/mybatis-extender/blob/master/mybatis-extender-api/src/main/java/dk/netdesign/mybatis/extender/api/MigrationCommands.java "Go to source") is exposed. This method should be used to execute a static list of Migrations commands upon registration of the MybatisConfiguration. If your use case is something simple like just calling **bootstrap()** and **up()**, this method can be used instead of consuming the MigrationCommands service.
+ ##### Collection<Exception> getExceptionHolder()
  In case of any exceptions being thrown when **executeOnRegistration()** is called, they will always be logged, but if the application should be made aware of the error, it can supply a Collection which should be filled with any exceptions thrown by that method.