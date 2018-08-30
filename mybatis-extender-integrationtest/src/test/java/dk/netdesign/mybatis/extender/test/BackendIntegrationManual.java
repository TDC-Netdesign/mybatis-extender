package dk.netdesign.mybatis.extender.test;

import org.apache.karaf.features.BootFinished;
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
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BackendIntegrationManual {

    @Inject
    private BootFinished bootFinished;

    @Configuration
    public Option[] configuration() throws Exception {
        return new Option[]{



                KarafDistributionOption.debugConfiguration("5005", false),
                karafDistributionConfiguration().frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                        .type("zip").version("4.0.9"))
                        .unpackDirectory(new File("target/paxexam/unpack/"))
                ,CoreOptions.systemTimeout(360000),
                configureConsole().ignoreLocalConsole(),
                logLevel(LogLevel.INFO),


                features(
                        maven().groupId("dk.netdesign")
                                .artifactId("mybatis-extender-feature").type("xml")
                                .classifier("features").versionAsInProject()
                       ,"mybatis-extender","mybatis-extender-sample","webconsole"),
        }
                ;
    }


    @Test
    public void dontStopTillYouGetEnough() throws IOException {
        System.out.println("ready and awaiting command!");
        System.in.read();

    }
}
