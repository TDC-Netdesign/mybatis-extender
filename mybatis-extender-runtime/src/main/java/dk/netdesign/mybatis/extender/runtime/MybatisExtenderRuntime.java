package dk.netdesign.mybatis.extender.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dk.netdesign.mybatis.extender.api.MybatisConfiguration;
import org.osgi.framework.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Created by nmw on 26-04-2017.
 */
@Component(immediate = true)
public class MybatisExtenderRuntime {

    private static final Logger LOGGER = LogManager.getLogger();

    private BundleContext ctx=null;

    @Activate
    public void activate(BundleContext ctx) throws InvalidSyntaxException {
        System.out.print("Where are my logging!?");
        LOGGER.info("MybatisExtenderRuntime started");
        this.ctx=ctx;
        //dk.netdesign.mybatis.extender.api.MybatisConfiguration

        String filter = "("+Constants.OBJECTCLASS+"=" + MybatisConfiguration.class.getName() + ")";
        MybatisExtenderServiceListener mybatisExtenderServiceListener = new MybatisExtenderServiceListener(ctx);
        ctx.addServiceListener(mybatisExtenderServiceListener, filter);

        ServiceReference<?> references[] = ctx.getServiceReferences((String) null, filter);
        for (int i = 0; references != null && i < references.length; i++) {
            mybatisExtenderServiceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, references[i]));
        }

    }





}
