package org.ops4j.mybatis.extender.sample;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ops4j.mybatis.extender.sample.mappers.SampleMapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Created by nmw on 26-04-2017.
 */
@Component
public class SampleComponentConsumer {


    private SampleMapper sampleMapper;
    private static final Logger LOGGER = LogManager.getLogger();

    @Activate
    public void activate(){

        LOGGER.info("Got my mapper, saying hello {}", getSampleMapper().sayHello());

    }

    public SampleMapper getSampleMapper() {
        return sampleMapper;
    }

    @Reference
    public void setSampleMapper(SampleMapper sampleMapper) {
        this.sampleMapper = sampleMapper;
    }
}
