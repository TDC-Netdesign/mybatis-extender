package dk.netdesign.osgidialer.integrationtest;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.extra.RepositoryOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import org.ops4j.pax.exam.CoreOptions;
import shaded.org.apache.http.protocol.HttpContext;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.repositories;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BackendIntegrationTest {

    @Inject
    private BootFinished bootFinished;

    @Configuration
    public Option[] configuration() throws Exception {
        return new Option[]{




                karafDistributionConfiguration().frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                        .type("zip").version("4.1.1"))
                        .unpackDirectory(new File("target/paxexam/unpack/"))
                ,CoreOptions.systemTimeout(360000),
                configureConsole().ignoreLocalConsole(),
                logLevel(LogLevel.DEBUG),


                features(
                        maven().groupId("mybatis-extender")
                                .artifactId("mybatis-extender-feature").type("xml")
                                .classifier("features").version("1.0-SNAPSHOT")
                       ,"mybatis-extender","mybatis-extender-sample","webconsole"),
        }
                ;
    }

    //    @Test
//    public void businessServiceSimpleCall() throws dk.netdesign.common.exception.CiscoAPIException, dk.netdesign.common.exception.FailedSecurityException {
//        businessService.getAllCampaigns();
//        
//    }

    @Category(DontRunOnJenkins.class)
    @Test
    public void dontStopTillYouGetEnough() throws IOException {
        System.in.read();

    }
}
