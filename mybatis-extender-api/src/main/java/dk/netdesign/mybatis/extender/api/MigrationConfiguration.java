/*
 * Copyright 2018 Martin Nielsen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.netdesign.mybatis.extender.api;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.migration.BootstrapScript;
import org.apache.ibatis.migration.MigrationScript;
import org.apache.ibatis.migration.OnAbortScript;
import org.apache.ibatis.migration.options.DatabaseOperationOption;

/**
 * Use this interface to receive Migrations support. When exposing a service with this interface, a {@link MigrationCommands} service is created. This interface
 * can be used as a command interface to call Migration Commands on the database defined by the {@link MybatisConfiguration#getJDBCUrl()} method.
 * In the case that more than a single MigrationConfiguration is registered, it is recommended to extend the {@link MigrationCommands} interface and overriding
 * {@link #getMigrationRemoteRegistrationType()}. This will tell the MybatisExtender to use the extended interface to expose the commands to that
 * interface instead.
 * If the Migration workflow is relatively simple, the{@link #executeOnRegistration(dk.netdesign.mybatis.extender.api.MigrationCommands) } can be used.
 * This method is called when the MigrationConfiguration service is registered, which means that simple tasks such as always calling 
 * {@link MigrationCommands#up()} can be handled that way instead of consuming the returned {@link MigrationCommands} service.
 */

public interface MigrationConfiguration extends MybatisConfiguration{
    
    /**
     * Returns the {@link BootstrapScript} used to first define the Database
     * @return The {@link BootstrapScript} for the database
     */
    public BootstrapScript getBootstrapScript();
    
    /**
     * Returns the {@link OnAbortScript} script used to abort operations in case of errors
     * @return The {@link OnAbortScript} available. Can be null.
     */
    public OnAbortScript getOnAbortScript();
    
    /**
     * Returns the MigrationScripts available to the database defined by this {@link MigrationConfiguration}
     * @return Every {@link MigrationScript} available in no particular order
     */
    public List<MigrationScript> getMigrationScripts();
    
//    /**
//     * Specifies the {@link ClassLoader} which should be used to load the SQL migrations placed in the {@link #getMigrationPackageName() migrations Package}.
//     * If this is not overridden, the ClassLoader from this object (The one implementing {@link MigrationConfiguration} will be used.
//     * @return The ClassLoader to use for loading the Migrations.
//     */
//    public default ClassLoader getClassLoader(){
//        return getClass().getClassLoader();
//    }
    
    /**
     * Specifies the type the Mybatis-Extender will expose the Migration commands on. If {@link MigrationCommands} is returned, the commands will be exposed on
     * that interface only. If a subclass of {@link MigrationCommands} is returned instead, the Migration commands will be exposed on both that interface, as
     * well as {@link MigrationCommands}.
     * @return The type that should be used to expose the commands provided by Migrations
     */
    public default Class<? extends MigrationCommands> getMigrationRemoteRegistrationType(){
        return MigrationCommands.class;
    };
    
    /**
     * Specifies the {@link DatabaseOperationOption} to use for all Migrations commands called on this database 
     * @return The options, or null for default options
     */
    public default DatabaseOperationOption getDatabaseOptions(){
        return null;
    }
    
    /**
     * This method can be to call MigrationCommands immediately when this Object is registered as a service. Use this method if the Migrations workflow is
     * sufficiently simple that it can be handled when the application is started.
     * @param migrator The Commands interface provided by the Mybatis-Extender to call Commands on the Migrations service.
     */
    public default void executeOnRegistration(MigrationCommands migrator){};
    
    public default Collection<Exception> getExceptionHolder(){return null;};
    
}
