package dk.netdesign.mybatis.extender.runtime;

import dk.netdesign.mybatis.extender.runtime.migrations.MigrationCommandsLayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dk.netdesign.mybatis.extender.api.MigrationCommands;
import dk.netdesign.mybatis.extender.api.MigrationConfiguration;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import net.bytebuddy.implementation.MethodDelegation;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dk.netdesign.mybatis.extender.api.MybatisConfiguration;
import java.io.UnsupportedEncodingException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * Created by nmw on 27-04-2017.
 */
public class MybatisExtenderServiceListener implements ServiceListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private BundleContext ctx;


    private HashMap<MybatisConfiguration, List<ServiceRegistration<?>>> serviceRegistry = new HashMap<>();

    public MybatisExtenderServiceListener(BundleContext ctx) {

        this.ctx = ctx;

    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
        LOGGER.debug("got service event {}", serviceEvent.getType());

        MybatisConfiguration service = (MybatisConfiguration) ctx.getService(serviceEvent.getServiceReference());

        switch (serviceEvent.getType()) {

            case ServiceEvent.REGISTERED:
                //Register mappers


                List<Interceptor> interceptorList = new ArrayList<>();
                Environment environment = new Environment(service.getClass().getCanonicalName(), new JdbcTransactionFactory(), getDataSource(service));
                final Configuration configuration = new Configuration(environment);

                if (service.getMappers().size() > 0) {
                    //setup mybatis context
                    if (service.getTypeHandlers() != null) {
                        service.getTypeHandlers().entrySet().forEach(classTypeHandlerEntry -> {
                                    configuration.getTypeHandlerRegistry().register(classTypeHandlerEntry.getKey(), classTypeHandlerEntry.getValue());
                                }

                        );
                    }
                }
                List<Object> proxyMappers = new ArrayList<>();

                for (Class clazz : service.getMappers()) {
                    LOGGER.info("registering {}", clazz.getCanonicalName());
                    configuration.addMapper(clazz);

                    ByteBuddy bb = new ByteBuddy();

                    Interceptor interceptor = null;
                    interceptor = new Interceptor(clazz);
                    interceptorList.add(interceptor);
                    Class<?> clz = bb.subclass(clazz).name("MybatisExtenderRuntime_" + clazz.getName())
                            .method(any()).intercept(MethodDelegation.to(interceptor))
                            .make()
                            .load(new MultipleParentClassLoader(Arrays.asList(clazz.getClassLoader(), this.getClass().getClassLoader())), ClassLoadingStrategy.Default.WRAPPER)
                            .getLoaded();
                    try {
                        Object test = clz.newInstance();
                        proxyMappers.add(test);
                    } catch (InstantiationException e) {
                        LOGGER.error(e);
                    } catch (IllegalAccessException e) {
                        LOGGER.error(e);
                    }
                }
                SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);


                interceptorList.stream().forEach(interceptor -> interceptor.setSqlSessionFactory(sqlSessionFactory));

                List<ServiceRegistration<?>> serviceRegistrationList = new ArrayList<>();
                Dictionary serviceProperties = new Hashtable();
                serviceProperties.put(MybatisConfiguration.CONFIGURATION_CLASS, service.getClass().getCanonicalName());
                    
                
                
                
                if (service instanceof MigrationConfiguration) {

                    MigrationConfiguration migrationConfig = (MigrationConfiguration) service;
                    LOGGER.info("Registering MigrationCommands under service {}", migrationConfig.getMigrationRemoteRegistrationType().getCanonicalName());
                    Collection<Exception> exceptionReturnList = migrationConfig.getExceptionHolder();
                    try {
                        MigrationCommands migrationRemote = new MigrationCommandsLayer(sqlSessionFactory, migrationConfig);
                        String[] migrationCommandsServiceClasses;
                        if (migrationConfig.getMigrationRemoteRegistrationType().equals(MigrationCommands.class)) {
                            migrationCommandsServiceClasses = new String[]{MigrationCommands.class.getCanonicalName()};
                        } else {
                            migrationCommandsServiceClasses = new String[]{MigrationCommands.class.getCanonicalName(), migrationConfig.getMigrationRemoteRegistrationType().getCanonicalName()};
                        }
                        ServiceRegistration commandsRegistration = ctx.registerService(migrationCommandsServiceClasses, migrationRemote, serviceProperties);
                        serviceRegistrationList.add(commandsRegistration);

                        migrationConfig.executeOnRegistration(migrationRemote);
                    } catch (UnsupportedEncodingException | RuntimeException ex) {
                        if (exceptionReturnList != null) {
                            exceptionReturnList.add(ex);
                        }
                        LOGGER.error("Exception occured when executing Migration setup", ex);
                    }

                }
                proxyMappers.forEach(o -> {
                    Class[] implementedInterfaces = o.getClass().getInterfaces();
                    LOGGER.info("class implements {} interfaces, interfaces are {}", implementedInterfaces.length, Arrays.asList(implementedInterfaces).toString());
                    ServiceRegistration<?> serviceRegistration = ctx.registerService(implementedInterfaces[0].getCanonicalName(), o, serviceProperties);
                    serviceRegistrationList.add(serviceRegistration);
                    LOGGER.info("registerered {} as service {}", o.getClass().getCanonicalName(), implementedInterfaces[0].getCanonicalName());
                });
                serviceRegistry.put(service, serviceRegistrationList);


                break;
            case ServiceEvent.UNREGISTERING:
                //Unregister mappers
                serviceRegistry.remove(service).stream().forEach(
                        serviceRegistration -> {
                            try {
                                serviceRegistration.unregister();
                                LOGGER.info("unregistering {}", service.getClass().getCanonicalName());
                            } catch (RuntimeException ex) {
                                LOGGER.error(" caugth an RuntimeException {}, unregistering {}", service.getClass().getCanonicalName());
                            }
                        }
                );


                break;
        }


    }

    private DataSource getDataSource(MybatisConfiguration service) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(service.getDriver().getCanonicalName());
        config.setJdbcUrl(service.getJDBCUrl());
        config.setUsername(service.getUser());
        config.setPassword(service.getPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        HikariDataSource ds = new HikariDataSource(config);
        return ds;
    }
}
