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
package dk.netdesign.mybatis.extender.api;

import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.migration.BootstrapScript;
import org.apache.ibatis.migration.MigrationScript;
import org.apache.ibatis.migration.OnAbortScript;
import org.apache.ibatis.migration.SimpleScript;

/**
 *
 * @author mnn
 */
public abstract class ScriptLoadingMigrationsConfiguration implements MigrationConfiguration{
    BootstrapScript bootstrap = null;
    OnAbortScript onAbort = null;
    List<MigrationScript> migrations = new ArrayList<>();
    
    @Override
    public BootstrapScript getBootstrapScript() {
        return bootstrap;
    }

    @Override
    public OnAbortScript getOnAbortScript() {
        return onAbort;
    }

    @Override
    public List<MigrationScript> getMigrationScripts() {
        return migrations;
    }
    
    public ScriptLoadingMigrationsConfiguration addScript(BootstrapScript script){
        if(bootstrap != null){
            throw new RuntimeException("Only one bootstrap script can be defined. Currently contains:\n"+bootstrap);
        }
        bootstrap = script;
        return this;
    }
    
    public ScriptLoadingMigrationsConfiguration addScript(OnAbortScript script){
        if(onAbort != null){
            throw new RuntimeException("Only one onAbort script can be defined. Currently contains:\n"+onAbort);
        }
        onAbort = script;
        return this;
    }
    
    public ScriptLoadingMigrationsConfiguration addScript(MigrationScript script){
        for(MigrationScript existingScript : migrations){
            if(script.getId().equals(existingScript.getId())){
                throw new RuntimeException("Cannot add script with id "+script.getId()+". Currently contains script with that id:\n"+script);
            }
        }
        migrations.add(script);
        return this;
    }
    
}
