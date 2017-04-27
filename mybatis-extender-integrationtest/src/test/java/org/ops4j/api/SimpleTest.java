package org.ops4j.api;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.ops4j.mybatis.extender.sample.SampleMapperConfig;
import org.ops4j.mybatis.extender.sample.mappers.SampleMapper;

/**
 * Created by nmw on 27-04-2017.
 */
public class SimpleTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    @Test
    public void testSomething() {

        // register and activate service with configuration
        MybatisExtenderRuntime service1 = context.registerInjectActivateService(new MybatisExtenderRuntime(),
                "prop1", "value1");

        SampleMapperConfig server2= context.registerInjectActivateService(new SampleMapperConfig(),
                "prop1", "value1");

    }
}
