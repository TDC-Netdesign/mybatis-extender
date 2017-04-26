package org.ops4j.api;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ops4j.mybatis.extender.api.MybatisConfiguration;
import org.osgi.framework.*;
import org.osgi.service.component.annotations.Component;

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * Created by nmw on 26-04-2017.
 */
@Component(immediate = true)
public class MybatisExtenderRuntime implements ServiceListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private BundleContext ctx=null;
    public void activate(BundleContext ctx) throws InvalidSyntaxException {
        System.out.print("Where are my logging!?");
        LOGGER.info("MybatisExtenderRuntime started");
        this.ctx=ctx;
        //org.ops4j.mybatis.extender.api.MybatisConfiguration

        String filter = "("+Constants.OBJECTCLASS+"=" + MybatisConfiguration.class.getName() + ")";
        ctx.addServiceListener(this, filter);

        ServiceReference<?> references[] = ctx.getServiceReferences((String) null, filter);
        for (int i = 0; references != null && i < references.length; i++) {
            this.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, references[i]));
        }

    }


    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {

        MybatisConfiguration service =(MybatisConfiguration) ctx.getService(serviceEvent.getServiceReference());

        switch (serviceEvent.getType()){

            case ServiceEvent.REGISTERED:
                //Register mappers
                for (Class clazz:service.getMappers()) {
                    LOGGER.info("registering {}",clazz.getCanonicalName());
                    ByteBuddy bb = new ByteBuddy();

                    Class<?> clz = bb
                            .subclass(clazz).name("MybatisExtenderMapper"+clazz.getName())
                            .method(any()).intercept(MethodDelegation.to(Interceptor.class))
                            .make()
                            .load(Object.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
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

                break;
            case ServiceEvent.UNREGISTERING:
                //Unregister mappers
                for (Class clazz:service.getMappers()) {

                    LOGGER.info("unregistering {}",clazz.getCanonicalName());
                }
                break;
        }



    }


}
