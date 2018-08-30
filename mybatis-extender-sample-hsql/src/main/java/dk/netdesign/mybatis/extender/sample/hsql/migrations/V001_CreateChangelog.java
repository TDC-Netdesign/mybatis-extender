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
package dk.netdesign.mybatis.extender.sample.hsql.migrations;

import java.math.BigDecimal;
import org.apache.ibatis.migration.MigrationScript;

/**
 *
 * @author mnn
 */
public class V001_CreateChangelog implements MigrationScript {
  public BigDecimal getId() {
    return new BigDecimal(1);
  }

  public String getDescription() {
    return "Create changelog";
  }

  public String getUpScript() {
    return "CREATE TABLE changelog ("
      + "ID INT NOT NULL,"
      + "APPLIED_AT VARCHAR(25) NOT NULL,"
      + "DESCRIPTION VARCHAR(255) NOT NULL,"
      + "PRIMARY KEY (id));"; 
  }

  public String getDownScript() {
    return "DROP TABLE changelog;";
  }
}
