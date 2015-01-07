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

package com.stratio.crossdata.core.statements;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import com.stratio.crossdata.common.data.CatalogName;
import com.stratio.crossdata.common.data.ClusterName;
import com.stratio.crossdata.common.data.ColumnName;
import com.stratio.crossdata.common.data.TableName;
import com.stratio.crossdata.common.metadata.ColumnType;
import com.stratio.crossdata.common.metadata.structures.TableType;
import com.stratio.crossdata.common.statements.structures.Selector;
import com.stratio.crossdata.common.utils.StringUtils;
import com.stratio.crossdata.core.validator.requirements.ValidationRequirements;
import com.stratio.crossdata.core.validator.requirements.ValidationTypes;

/**
 * Class that models a {@code CREATE TABLE} statement of the CROSSDATA language.
 */
public class CreateTableStatement extends MetadataStatement implements ITableStatement {

    private TableType tableType = TableType.DATABASE;

    /**
     * The name of the target table.
     */
    private TableName tableName;

    private ClusterName clusterName;

    /**
     * A map with the name of the columns in the table and the associated data type.
     */
    private LinkedHashMap<ColumnName, ColumnType> columnsWithType = new LinkedHashMap<>();

    /**
     * The list of columns that are part of the primary key.
     */
    private LinkedList<ColumnName> primaryKey = new LinkedList<>();

    /**
     * The list of columns that are part of the partition key.
     */
    private LinkedList<ColumnName> partitionKey = new LinkedList<>();

    /**
     * The list of columns that are part of the clustering key.
     */
    private LinkedList<ColumnName> clusterKey = new LinkedList<>();

    /**
     * The list of {@link com.stratio.crossdata.core.structures.Property} of the table.
     */
    private Map<Selector, Selector> properties = new LinkedHashMap<>();

    /**
     * Whether the table should be created only if not exists.
     */
    private boolean ifNotExists;

    /**
     * Class constructor.
     *
     * @param tableType    TABLE type {@link com.stratio.crossdata.common.metadata.structures.TableType}.
     * @param tableName    The name of the table.
     * @param columns      A map with the name of the columns in the table and the associated data type.
     * @param partitionKey The list of columns that are part of the primary key.
     * @param clusterKey   The list of columns that are part of the clustering key.
     */
    public CreateTableStatement(TableType tableType, TableName tableName, ClusterName clusterName,
            LinkedHashMap<ColumnName, ColumnType> columns,
            LinkedList<ColumnName> partitionKey, LinkedList<ColumnName> clusterKey) {
        this.command = false;
        this.tableType = tableType;
        this.tableName = tableName;
        this.clusterName = clusterName;
        this.columnsWithType = columns;
        this.partitionKey = partitionKey;
        this.clusterKey = clusterKey;
        if (partitionKey != null) {
            this.primaryKey.addAll(partitionKey);
        }
        if (clusterKey != null) {
            this.primaryKey.addAll(clusterKey);
        }
    }

    /**
     * Class constructor.
     *
     * @param tableName    The name of the table.
     * @param columns      A map with the name of the columns in the table and the associated data type.
     * @param partitionKey The list of columns that are part of the primary key.
     * @param clusterKey   The list of columns that are part of the clustering key.
     */
    public CreateTableStatement(TableName tableName, ClusterName clusterName,
            LinkedHashMap<ColumnName, ColumnType> columns,
            LinkedList<ColumnName> partitionKey, LinkedList<ColumnName> clusterKey) {
        this(TableType.DATABASE, tableName, clusterName, columns, partitionKey, clusterKey);
    }

    public LinkedList<ColumnName> getPartitionKey() {
        return partitionKey;
    }

    public LinkedList<ColumnName> getClusterKey() {
        return clusterKey;
    }

    public TableType getTableType() {
        return tableType;
    }

    public LinkedHashMap<ColumnName, ColumnType> getColumnsWithTypes() {
        return columnsWithType;
    }

    public TableName getTableName() {
        return tableName;
    }

    public void setTableName(TableName tableName) {
        this.tableName = tableName;
    }

    public ClusterName getClusterName() {
        return clusterName;
    }

    /**
     * Set the catalog specified in the create table statement.
     *
     * @param catalog The name of the catalog.
     */
    public void setCatalog(CatalogName catalog) {
        this.catalog = catalog;
    }

    /**
     * Set the list of {@link com.stratio.crossdata.core.structures.Property}.
     *
     * @param properties The list.
     */
    public void setProperties(String properties) {
        this.properties = StringUtils.convertJsonToOptions(properties);
    }

    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }

    /**
     * The list of properties of the table.
     */
    public Map<Selector, Selector> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CREATE ");
        if (tableType != TableType.DATABASE) {
            sb.append(tableType);
        }
        sb.append("TABLE ");
        if (ifNotExists) {
            sb.append("IF NOT EXISTS ");
        }
        sb.append(tableName.getQualifiedName());
        sb.append(" ON CLUSTER ").append(clusterName);

        sb.append("(");
        sb.append(columnsWithType.toString().replace("{", "").replace("}", ""));
        sb.append(", PRIMARY KEY((").append(partitionKey.toString().replace("[", "").replace("]", "")).append(")");
        if (!clusterKey.isEmpty()) {
            sb.append(", ").append(clusterKey.toString().replace("[", "").replace("]", ""));
        }
        sb.append("))");

        if (hasProperties()) {
            sb.append(" WITH ").append(properties);
        }
        return sb.toString();
    }

    private boolean hasProperties() {
        return ((properties != null) && (!properties.isEmpty()));
    }

    public ValidationRequirements getValidationRequirements() {
        return new ValidationRequirements().add(ValidationTypes.MUST_EXIST_CATALOG)
                .add(ValidationTypes.MUST_EXIST_CLUSTER)
                .add(ValidationTypes.MUST_NOT_EXIST_TABLE);
    }

    @Override
    public CatalogName getEffectiveCatalog() {
        CatalogName effective;
        if (tableName != null) {
            effective = tableName.getCatalogName();
        } else {
            effective = catalog;
        }
        if (sessionCatalog != null) {
            effective = sessionCatalog;
        }
        return effective;
    }

    public boolean isIfNotExists() {
        return ifNotExists;
    }
}
