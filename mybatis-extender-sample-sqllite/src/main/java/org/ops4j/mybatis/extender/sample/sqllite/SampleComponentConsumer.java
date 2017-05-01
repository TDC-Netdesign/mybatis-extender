package org.ops4j.mybatis.extender.sample.sqllite;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ops4j.mybatis.extender.sample.sqllite.mappers.SampleAnnotationMapper;
import org.ops4j.mybatis.extender.sample.sqllite.mappers.SampleXMLBasedMapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Created by nmw on 26-04-2017.
 */
@Component
public class SampleComponentConsumer {


    private SampleAnnotationMapper sampleAnnotationMapper;
    private SampleXMLBasedMapper sampleXMLBasedMapper;
    private static final Logger LOGGER = LogManager.getLogger();

    @Activate
    public void activate() {

        LOGGER.info("Got getSampleAnnotationMapper, saying hello {}", getSampleAnnotationMapper().sayHello());

        //Just for fun printing all default tables:

        LOGGER.info("Got getSampleAnnotationMapper, saying hello {}", getSampleAnnotationMapper().sayHello());
        getSampleAnnotationMapper().getAllTables().stream().forEach(table -> LOGGER.info("found table {}", table));


        LOGGER.info("Got getSampleXMLBasedMapper, saying hello {}", getSampleXMLBasedMapper().sayHello());
        getSampleXMLBasedMapper().getAllTables().stream().forEach(table -> LOGGER.info("found table {}", table));


    }

    public SampleAnnotationMapper getSampleAnnotationMapper() {
        return sampleAnnotationMapper;
    }

    @Reference
    public void setSampleAnnotationMapper(SampleAnnotationMapper sampleAnnotationMapper) {
        this.sampleAnnotationMapper = sampleAnnotationMapper;
    }

    @Reference
    public void setSampleXMLBasedMapper(SampleXMLBasedMapper sampleXMLBasedMapper) {
        this.sampleXMLBasedMapper = sampleXMLBasedMapper;
    }

    public SampleXMLBasedMapper getSampleXMLBasedMapper() {
        return sampleXMLBasedMapper;
    }
}
