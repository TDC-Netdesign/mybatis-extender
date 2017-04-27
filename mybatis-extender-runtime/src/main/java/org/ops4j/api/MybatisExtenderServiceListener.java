package org.ops4j.api;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import org.ops4j.mybatis.extender.api.MybatisConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * Created by nmw on 27-04-2017.
 */
public class MybatisExtenderServiceListener  implements ServiceListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private BundleContext ctx;

    public MybatisExtenderServiceListener(BundleContext ctx) {

        this.ctx = ctx;
    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
        LOGGER.info("got service event {}",serviceEvent.getType());

        MybatisConfiguration service =(MybatisConfiguration) ctx.getService(serviceEvent.getServiceReference());

        switch (serviceEvent.getType()){

            case ServiceEvent.REGISTERED:
                //Register mappers

                Configuration configuration =null;
                List<Interceptor> interceptorList=new ArrayList<>();
                if(service.getMappers().size()>0)
                {
                    //setup mybatis context
                    Environment environment = new Environment(service.getClass().getCanonicalName(), new JdbcTransactionFactory(), getDataSource(service));
                    configuration = new Configuration(environment);


                }

                for (Class clazz:service.getMappers()) {
                    LOGGER.info("registering {}",clazz.getCanonicalName());
                    configuration.addMapper(clazz);

                    ByteBuddy bb = new ByteBuddy();
                    Interceptor interceptor=null;
                    interceptor=new Interceptor(clazz);
                    interceptorList.add(interceptor);
                    Class<?> clz = bb
                            .subclass(clazz).name("MybatisExtenderMapper"+clazz.getName())
                            .method(any()).intercept(MethodDelegation.to(interceptor))
                            .make()
                            .load(new MultipleParentClassLoader(Arrays.asList( clazz.getClassLoader(),this.getClass().getClassLoader())), ClassLoadingStrategy.Default.WRAPPER)
                            .getLoaded();
                    try {
                        Object test = clz.newInstance();
                        ServiceRegistration<?> serviceRegistration = ctx.registerService(clazz.getName(), test, null);
                        LOGGER.info("registerered {}",test.getClass().getCanonicalName());
                    } catch (InstantiationException e) {
                        LOGGER.error(e);
                    } catch (IllegalAccessException e) {
                        LOGGER.error(e);
                    }
                }
                SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
                interceptorList.stream().forEach(interceptor -> interceptor.setSqlSessionFactory(sqlSessionFactory));


                break;
            case ServiceEvent.UNREGISTERING:
                //Unregister mappers
                for (Class clazz:service.getMappers()) {

                    LOGGER.info("unregistering {}",clazz.getCanonicalName());
                }
                break;
        }



    }
    private DataSource getDataSource(MybatisConfiguration service) {
        HikariConfig config = new HikariConfig();
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
