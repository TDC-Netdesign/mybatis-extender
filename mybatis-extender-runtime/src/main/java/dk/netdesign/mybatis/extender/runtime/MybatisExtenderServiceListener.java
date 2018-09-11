package dk.netdesign.mybatis.extender.runtime;

import dk.netdesign.mybatis.extender.runtime.migrations.MigrationCommandsLayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dk.netdesign.mybatis.extender.api.MigrationCommands;
import dk.netdesign.mybatis.extender.api.MigrationConfiguration;
import dk.netdesign.mybatis.extender.api.MybatisCommands;
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
import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * Created by nmw on 27-04-2017.
 */
public class MybatisExtenderServiceListener implements ServiceListener, Closeable {

    private static final Logger LOGGER = LogManager.getLogger();
    private BundleContext ctx;

    private final HashMap<MybatisConfiguration, List<ServiceRegistration<?>>> serviceRegistry = new HashMap<>();
    private final HashMap<MybatisConfiguration, MybatisCommandsInvocationHandler> configurations = new HashMap<>();

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
                Environment environment = null;
                try {

                    List<Interceptor> interceptorList = new ArrayList<>();
                    environment = buildEnvironment(service);
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
                    ClassLoader multiClassLoader = new MultipleParentClassLoader(Arrays.asList(service.getClass().getClassLoader(), this.getClass().getClassLoader()));
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
                                .load(multiClassLoader, ClassLoadingStrategy.Default.WRAPPER)
                                .getLoaded();

                        Object test = clz.newInstance();
                        proxyMappers.add(test);

                    }
                    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

                    interceptorList.stream().forEach(interceptor -> interceptor.setSqlSessionFactory(sqlSessionFactory));

                    List<ServiceRegistration<?>> serviceRegistrationList = new ArrayList<>();
                    Dictionary serviceProperties = new Hashtable();
                    serviceProperties.put(MybatisConfiguration.CONFIGURATION_CLASS, service.getClass().getCanonicalName());

                    List<Class<?>> cmdServiceClasses = new ArrayList<>();
                    cmdServiceClasses.add(MybatisCommands.class);

                    MigrationCommandsLayer migrationRemote = null;

                    if (service instanceof MigrationConfiguration) {

                        MigrationConfiguration migrationConfig = (MigrationConfiguration) service;
                        LOGGER.info("Registering MigrationCommands under service {}", migrationConfig.getMigrationRemoteRegistrationType().getCanonicalName());
                        Collection<Exception> exceptionReturnList = migrationConfig.getExceptionHolder();
                        try {
                            migrationRemote = new MigrationCommandsLayer(sqlSessionFactory, migrationConfig);
                            cmdServiceClasses.add(MigrationCommands.class);
                            if (!migrationConfig.getMigrationRemoteRegistrationType().equals(MigrationCommands.class)) {
                                cmdServiceClasses.add(migrationConfig.getMigrationRemoteRegistrationType());
                            }

                            migrationConfig.executeOnRegistration(migrationRemote);
                        } catch (UnsupportedEncodingException | RuntimeException ex) {
                            if (exceptionReturnList != null) {
                                exceptionReturnList.add(ex);
                            }
                            throw ex;
                        }

                    }

                    MybatisCommandsInvocationHandler handler = new MybatisCommandsInvocationHandler(service, configuration, migrationRemote);
                    configurations.put(service, handler);

                    ServiceRegistration commandsRegistration = ctx.registerService(getNamesFromClasses(cmdServiceClasses), Proxy.newProxyInstance(multiClassLoader, cmdServiceClasses.toArray(new Class[cmdServiceClasses.size()]), handler), serviceProperties);
                    serviceRegistrationList.add(commandsRegistration);

                    proxyMappers.forEach(o -> {
                        Class[] implementedInterfaces = o.getClass().getInterfaces();
                        LOGGER.info("class implements {} interfaces, interfaces are {}", implementedInterfaces.length, Arrays.asList(implementedInterfaces).toString());
                        ServiceRegistration<?> serviceRegistration = ctx.registerService(implementedInterfaces[0].getCanonicalName(), o, serviceProperties);
                        serviceRegistrationList.add(serviceRegistration);
                        LOGGER.info("registerered {} as service {}", o.getClass().getCanonicalName(), implementedInterfaces[0].getCanonicalName());
                    });
                    serviceRegistry.put(service, serviceRegistrationList);
                } catch (InstantiationException | IllegalAccessException | UnsupportedEncodingException | RuntimeException ex) {
                    LOGGER.error("An error occurred when registering service", ex);
                    if (environment != null) {
                        try {
                            closeDataSource(environment.getDataSource());
                        } catch (IOException ex1) {
                            LOGGER.error("Failed to close " + environment.getDataSource(), ex1);
                        }
                    }
                    removeService(service);
                }
                break;
            case ServiceEvent.UNREGISTERING:
                removeService(service);
                break;
        }

    }

    @Override
    public void close() throws IOException {
        List<MybatisConfiguration> configurationsToClose = new ArrayList<>(serviceRegistry.keySet());
        configurationsToClose.addAll(configurations.keySet());
        for(MybatisConfiguration configToClose : configurationsToClose){
            removeService(configToClose);
        }
    }
    
    

    private void removeService(MybatisConfiguration service) {
        //Unregister mappers
        List<ServiceRegistration<?>> services = serviceRegistry.remove(service);
        if (services != null) {
            services.stream().forEach(
                    serviceRegistration -> {
                        try {
                            serviceRegistration.unregister();
                            LOGGER.info("unregistering {}", service.getClass().getCanonicalName());
                        } catch (RuntimeException ex) {
                            LOGGER.error(" caught an RuntimeException {}, unregistering {}", service.getClass().getCanonicalName());
                        }
                    }
            );
        }

        try {
            MybatisCommandsInvocationHandler handler = configurations.remove(service);
            if (handler != null) {
                handler.close();
            }
        } catch (IOException ex) {
            LOGGER.error("Could not close DataSource when unregistering service " + service, ex);
        }

    }

    private String[] getNamesFromClasses(List<Class<?>> classList) {
        String[] strings = new String[classList.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = classList.get(i).getCanonicalName();
        }
        return strings;
    }

    private Environment buildEnvironment(MybatisConfiguration service) {
        return new Environment(service.getClass().getCanonicalName(), new JdbcTransactionFactory(), buildDataSource(service));
    }

    private DataSource buildDataSource(MybatisConfiguration service) {
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

    protected class MybatisCommandsInvocationHandler implements InvocationHandler, MybatisCommands, Closeable {

        private final MybatisConfiguration service;

        private final Configuration mybatisConfiguration;
        private final MigrationCommandsLayer migrationCommands;

        public MybatisCommandsInvocationHandler(MybatisConfiguration service, Configuration mybatisConfiguration, MigrationCommandsLayer migrationCommands) {
            this.service = service;
            this.mybatisConfiguration = mybatisConfiguration;
            this.migrationCommands = migrationCommands;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                if (containsMethod(MybatisCommands.class, method)) {
                    return method.invoke(this, args);
                } else if (containsMethod(MigrationCommands.class, method)) {
                    if (migrationCommands == null) {
                        throw new UnsupportedOperationException("This commands interface does not support " + MigrationCommands.class.getSimpleName() + " methods");
                    }
                    return method.invoke(migrationCommands, args);
                } else if (containsMethod(Object.class, method)) {
                    return method.invoke(this, args);
                } else {
                    throw new UnsupportedOperationException(method + " is unknown to this InvocationHandler");
                }
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }

        @Override
        public void rebuildConnection() throws IOException {
            Environment current = mybatisConfiguration.getEnvironment();
            mybatisConfiguration.setEnvironment(buildEnvironment(service));
            closeDataSource(current.getDataSource());
        }

        @Override
        public void close() throws IOException {
            Environment current = mybatisConfiguration.getEnvironment();
            closeDataSource(current.getDataSource());
        }

        private boolean containsMethod(Class toSearch, Method toFind) {
            methodloop:
            for (Method method : toSearch.getDeclaredMethods()) {
                if (method.getName().equals(toFind.getName())) {
                    Class<?>[] methodParameters = method.getParameterTypes();
                    Class<?>[] toFindParameters = toFind.getParameterTypes();
                    if (methodParameters.length != toFindParameters.length) {
                        continue;
                    }
                    for (int i = 0; i < methodParameters.length; i++) {
                        if (!methodParameters[i].equals(toFindParameters[i])) {
                            continue methodloop;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

    }

    private void closeDataSource(DataSource source) throws IOException {
        if (source != null && source instanceof Closeable) {
            ((Closeable) source).close();
        }
    }
}
