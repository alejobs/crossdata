/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.stratio.crossdata.common.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.stratio.crossdata.common.metadata.ColumnMetadata;
import com.stratio.crossdata.common.statements.structures.Selector;

public class AlterOptions implements Serializable {

    private static final long serialVersionUID = -2447041205006360889L;

    /**
     * Type of alter.
     */
    private AlterOperation option;

    private ColumnMetadata columnMetadata;

    private Map<Selector, Selector> properties = new HashMap<>();

    public AlterOptions(AlterOperation option,
            Map<Selector, Selector> properties,
            ColumnMetadata columnMetadata) {
        this.option = option;
        this.columnMetadata = columnMetadata;
        this.properties = properties;
    }

    public AlterOperation getOption() {
        return option;
    }

    public void setOption(AlterOperation option) {
        this.option = option;
    }

    public ColumnMetadata getColumnMetadata() {
        return columnMetadata;
    }

    public void setColumnMetadata(ColumnMetadata columnMetadata) {
        this.columnMetadata = columnMetadata;
    }

    public Map<Selector, Selector> getProperties() {
        return properties;
    }

    public void setProperties(Map<Selector, Selector> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AlterOptions that = (AlterOptions) o;

        if (columnMetadata != null ? !columnMetadata.equals(that.columnMetadata) : that.columnMetadata != null) {
            return false;
        }
        if (option != that.option) {
            return false;
        }
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = option != null ? option.hashCode() : 0;
        result = 31 * result + (columnMetadata != null ? columnMetadata.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
