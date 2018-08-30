package dk.netdesign.mybatis.extender.sample.hsql;

import dk.netdesign.mybatis.extender.api.MigrationCommands;
import dk.netdesign.mybatis.extender.api.MigrationConfiguration;
import org.apache.ibatis.type.TypeHandler;
import dk.netdesign.mybatis.extender.api.MybatisConfiguration;
import dk.netdesign.mybatis.extender.api.ScriptLoadingMigrationsConfiguration;
import dk.netdesign.mybatis.extender.sample.hsql.mappers.SampleAnnotationMapper;
import dk.netdesign.mybatis.extender.sample.hsql.mappers.SampleXMLBasedMapper;
import dk.netdesign.mybatis.extender.sample.hsql.migrations.BootstrapMigration;
import dk.netdesign.mybatis.extender.sample.hsql.migrations.SampleMigrationCommands;
import dk.netdesign.mybatis.extender.sample.hsql.migrations.V001_CreateChangelog;
import dk.netdesign.mybatis.extender.sample.hsql.migrations.V002_MigrationCreateBooks;
import dk.netdesign.mybatis.extender.sample.hsql.migrations.V003_MigrationUniqueKey;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.migration.Change;

/**
 * Created by nmw on 26-04-2017.
 */
@Component(immediate = true, service = MybatisConfiguration.class)
public class SampleMapperConfig extends ScriptLoadingMigrationsConfiguration{

    public SampleMapperConfig() {
        addScript(new BootstrapMigration());
        addScript(new V001_CreateChangelog()).addScript(new V002_MigrationCreateBooks()).addScript(new V003_MigrationUniqueKey());
    }
    
    
    @Override
    public List<Class> getMappers() {
        List<Class> myMappers=new ArrayList<>();
        myMappers.add(SampleAnnotationMapper.class);
        myMappers.add(SampleXMLBasedMapper.class);

        return myMappers;
    }

    @Override
    public Map<Class, TypeHandler> getTypeHandlers() {
        return null;
    }

    @Override
    public String getJDBCUrl() {
        return "jdbc:hsqldb:mem:sample";
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUser() {
        return null;
    }

    @Override
    public Class getDriver() {
        return org.hsqldb.jdbcDriver.class;
    }
    
    @Override
    public Class<? extends MigrationCommands> getMigrationRemoteRegistrationType() {
        return SampleMigrationCommands.class;
    }

    @Override
    public void executeOnRegistration(MigrationCommands migrator) {
        migrator.bootstrap();
        migrator.up();
        List<Change> changes = migrator.status();
        System.out.println(changes);
    }
    
    
    
    
    
    
}
