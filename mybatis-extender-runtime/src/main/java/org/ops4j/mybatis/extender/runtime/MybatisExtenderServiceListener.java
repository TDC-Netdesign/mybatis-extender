package org.ops4j.mybatis.extender.runtime;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import net.bytebuddy.implementation.MethodDelegation;
import org.apache.ibatis.io.ClassLoaderWrapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ops4j.mybatis.extender.api.MybatisConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * Created by nmw on 27-04-2017.
 */
public class MybatisExtenderServiceListener implements ServiceListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private BundleContext ctx;

    private MultipleParentClassLoader multipleParentClassLoader;

    private HashMap<MybatisConfiguration, List<ServiceRegistration<?>>> serviceRegistry = new HashMap<>();

    public MybatisExtenderServiceListener(BundleContext ctx) {

        this.ctx = ctx;
        multipleParentClassLoader = new MultipleParentClassLoader(Arrays.asList(this.getClass().getClassLoader()));

    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
        LOGGER.info("got service event {}", serviceEvent.getType());

        MybatisConfiguration service = (MybatisConfiguration) ctx.getService(serviceEvent.getServiceReference());

        switch (serviceEvent.getType()) {

            case ServiceEvent.REGISTERED:
                //Register mappers


                List<Interceptor> interceptorList = new ArrayList<>();
                Environment environment = new Environment(service.getClass().getCanonicalName(), new JdbcTransactionFactory(), getDataSource(service));
                final Configuration configuration = new Configuration(environment);

                if (service.getMappers().size() > 0) {
                    //setup mybatis context
                    service.getTypeHandlers().entrySet().forEach(classTypeHandlerEntry -> {
                                configuration.getTypeHandlerRegistry().register(classTypeHandlerEntry.getKey(), classTypeHandlerEntry.getValue());
                            }
                    );

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
                proxyMappers.forEach(o -> {
                    Class[] implementedInterfaces = o.getClass().getInterfaces();
                    LOGGER.info("class implements {} interfaces, interfaces are {}", implementedInterfaces.length, implementedInterfaces.toString());
                    ServiceRegistration<?> serviceRegistration = ctx.registerService(implementedInterfaces[0].getCanonicalName(), o, null);
                    serviceRegistrationList.add(serviceRegistration);
                    LOGGER.info("registerered {} as service {}", o.getClass().getCanonicalName(), implementedInterfaces[0].getClass().getCanonicalName());
                });
                ;
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
