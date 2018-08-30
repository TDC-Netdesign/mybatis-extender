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

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.migration.Change;
import org.apache.ibatis.migration.MigrationException;

/**
 *
 * @author mnn
 */
public interface MigrationCommands {
    
    public void bootstrap() throws MigrationException;
    public void bootstrap(boolean force) throws MigrationException;
    public void up() throws MigrationException;
    public void down() throws MigrationException;
    public void up(int number) throws MigrationException;
    public void down(int number) throws MigrationException;
    public void version(BigDecimal versionTarget) throws MigrationException;
    public void pending() throws MigrationException;
    public List<Change> status() throws MigrationException;
    
}
