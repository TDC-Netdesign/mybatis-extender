/*
 * Copyright 2018 mnn.
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
package dk.netdesign.mybatis.extender.runtime.migrations;

import dk.netdesign.mybatis.extender.api.MigrationConfiguration;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.migration.BootstrapScript;
import org.apache.ibatis.migration.Change;
import org.apache.ibatis.migration.MigrationException;
import org.apache.ibatis.migration.MigrationLoader;
import org.apache.ibatis.migration.MigrationScript;
import org.apache.ibatis.migration.OnAbortScript;

/**
 *
 * @author mnn
 */
public class OSGiMigrationLoader implements MigrationLoader {

    private MigrationConfiguration migrationConfig;

    public OSGiMigrationLoader(MigrationConfiguration migrationConfig) {
        this.migrationConfig = migrationConfig;
    }

    @Override
    public List<Change> getMigrations() {
        List<Change> changes = new ArrayList<>();
        List<MigrationScript> scripts = migrationConfig.getMigrationScripts();
        for(MigrationScript script : scripts){
            changes.add(parseChangeFromMigrationScript(script));
        }
        return changes;
    }

    @Override
    public Reader getScriptReader(Change change, boolean undo) {
        List<MigrationScript> scripts = migrationConfig.getMigrationScripts();
        for(MigrationScript script : scripts){
            if(script.getId().equals(change.getId())){
                return new StringReader(undo ? script.getDownScript() : script.getUpScript());
            }
        }
        return null;
    }

    @Override
    public Reader getBootstrapReader() {
        BootstrapScript script = migrationConfig.getBootstrapScript();
        if(script != null){
            return new StringReader(script.getScript());
        }else{
            throw new MigrationException("Could not load Bootstrap. Bootstrap script returned by "+MigrationConfiguration.class.getName()+" was null");
        }
    }

    @Override
    public Reader getOnAbortReader() {
        OnAbortScript script = migrationConfig.getOnAbortScript();
        if(script != null){
            return new StringReader(script.getScript());
        }else{
            return null;
        }
    }

    protected Change parseChangeFromMigrationScript(MigrationScript script) {
        Change change = new Change();
        change.setId(script.getId());
        change.setDescription(script.getDescription());
        change.setFilename(script.getClass().getName());
        return change;
    }
    
     

}
