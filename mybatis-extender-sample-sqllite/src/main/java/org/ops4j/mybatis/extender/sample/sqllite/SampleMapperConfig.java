package org.ops4j.mybatis.extender.sample.sqllite;

import org.apache.ibatis.type.TypeHandler;
import org.ops4j.mybatis.extender.api.MybatisConfiguration;
import org.ops4j.mybatis.extender.sample.sqllite.mappers.SampleAnnotationMapper;
import org.ops4j.mybatis.extender.sample.sqllite.mappers.SampleXMLBasedMapper;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by nmw on 26-04-2017.
 */
@Component(immediate = true)
public class SampleMapperConfig implements MybatisConfiguration {
    @Override
    public List<Class> getMappers() {
        List<Class> myMappers = new ArrayList<>();
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
        return "jdbc:sqlite:test.db";
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
        return org.sqlite.JDBC.class;
    }
}
