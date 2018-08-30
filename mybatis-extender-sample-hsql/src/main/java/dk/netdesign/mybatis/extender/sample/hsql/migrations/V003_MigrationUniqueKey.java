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
public class V003_MigrationUniqueKey implements MigrationScript{

    @Override
    public BigDecimal getId() {
        return new BigDecimal(3);
    }

    @Override
    public String getDescription() {
        return "Add unique constraint";
    }

    @Override
    public String getUpScript() {
        return "ALTER TABLE books ADD CONSTRAINT UQ_book_title UNIQUE (title);";
    }

    @Override
    public String getDownScript() {
        return "ALTER TABLE books REMOVE CONSTRAINT UQ_book_title;";
    }
    
}
