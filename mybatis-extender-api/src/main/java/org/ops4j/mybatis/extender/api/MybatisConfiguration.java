package org.ops4j.mybatis.extender.api;

import org.apache.ibatis.type.TypeHandler;

import java.util.List;
import java.util.Map;

/**
 * Created by nino on 09-04-2017.
 */
public interface MybatisConfiguration {

    /**
     *
     * @return the list of mappers for this configuration
     */
    public List<Class> getMappers();

    /**
     *
     * @return Map key class which are handled by typehandler
     */
    public Map<Class,TypeHandler> getTypeHandlers();

    /**
     *
     * @return a valid jdbc url string
     */
    public String getJDBCUrl();

    /**
     *
     * @return get password for JDBC
     */
    public String getPassword();

    /**
     *
     * @return user for JDBC
     */
    public String getUser();

    /**
     * driver used for jdbc connection, will be loaded in to the bundles context.
     * @return
     */
    public Class getDriver();
}

