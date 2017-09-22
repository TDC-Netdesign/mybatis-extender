package org.ops4j.mybatis.extender.test;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.ops4j.mybatis.extender.runtime.MybatisExtenderRuntime;
import org.ops4j.mybatis.extender.sample.hsql.SampleMapperConfig;

/**
 * Created by nmw on 27-04-2017.
 */
public class SimpleTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    @Ignore("Need to fix missing import, and expand testing")
    @Test
    public void testSomething() {

        // register and activate service with configuration
        MybatisExtenderRuntime service1 = context.registerInjectActivateService(new MybatisExtenderRuntime(),
                "prop1", "value1");

        SampleMapperConfig server2= context.registerInjectActivateService(new SampleMapperConfig(),
                "prop1", "value1");

    }
}
