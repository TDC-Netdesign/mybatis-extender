package dk.netdesign.mybatis.extender.runtime;

import dk.netdesign.mybatis.extender.api.MigrationConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dk.netdesign.mybatis.extender.api.MybatisConfiguration;
import java.io.IOException;
import org.osgi.framework.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Created by nmw on 26-04-2017.
 */
@Component(immediate = true)
public class MybatisExtenderRuntime {

    private static final Logger LOGGER = LogManager.getLogger();
    private MybatisExtenderServiceListener mybatisExtenderServiceListener;

    @Activate
    public void activate(BundleContext ctx) throws InvalidSyntaxException {
        LOGGER.info(MybatisExtenderRuntime.class.getSimpleName()+" started");
        //dk.netdesign.mybatis.extender.api.MybatisConfiguration

        String filter = "(|("+Constants.OBJECTCLASS+"=" + MybatisConfiguration.class.getName() + ")("+Constants.OBJECTCLASS+"=" + MigrationConfiguration.class.getName() + "))";
        mybatisExtenderServiceListener = new MybatisExtenderServiceListener(ctx);
        ctx.addServiceListener(mybatisExtenderServiceListener, filter);

        ServiceReference<?> references[] = ctx.getServiceReferences((String) null, filter);
        for (int i = 0; references != null && i < references.length; i++) {
            mybatisExtenderServiceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, references[i]));            
        }

    }
    
    @Deactivate
    public void deactivate(BundleContext ctx) throws IOException{
        LOGGER.info(MybatisExtenderRuntime.class.getSimpleName()+" closed");
        if(mybatisExtenderServiceListener != null){
            mybatisExtenderServiceListener.close();
        }
        
    }





}
