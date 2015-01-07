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

package com.stratio.crossdata.core.normalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.stratio.crossdata.common.data.CatalogName;
import com.stratio.crossdata.common.data.ColumnName;
import com.stratio.crossdata.common.data.TableName;
import com.stratio.crossdata.common.metadata.TableMetadata;
import com.stratio.crossdata.common.statements.structures.OrderByClause;
import com.stratio.crossdata.common.statements.structures.Relation;
import com.stratio.crossdata.common.statements.structures.Selector;
import com.stratio.crossdata.core.metadata.MetadataManager;
import com.stratio.crossdata.core.structures.GroupByClause;
import com.stratio.crossdata.core.structures.InnerJoin;

public class NormalizedFields {
    private Set<ColumnName> columnNames = new HashSet<>();
    private Set<TableName> tableNames = new HashSet<>();
    private Set<CatalogName> catalogNames = new HashSet<>();
    // It can include functions, column names, asterisks...
    private List<Selector> selectors = new ArrayList<>();
    // Join relations
    private InnerJoin join;
    private List<Relation> where = new ArrayList<>();
    private List<OrderByClause> orderByClauseClauses = new ArrayList<>();
    private GroupByClause groupByClause = new GroupByClause();

    private List<TableMetadata> tablesMetadata = new ArrayList<>();

    /**
     * Map of tables alias associating alias with TableNames.
     */
    private Map<String, TableName> tableAlias = new HashMap<>();

    /**
     * Map of column alias associating alias with ColumnNames.
     */
    private Map<String, ColumnName> columnNameAlias = new HashMap<>();

    private Map<String, String> signatures = new HashMap<>();

    public NormalizedFields() {
    }

    public void addColumnName(ColumnName columnName, String alias) {
        this.columnNames.add(columnName);
        if (alias != null) {
            this.columnNameAlias.put(alias, columnName);
        }
    }

    public Set<ColumnName> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(Set<ColumnName> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Add a table name to the list. If an alias is found the alias mapping is also stored.
     *
     * @param tableName A {@link com.stratio.crossdata.common.data.TableName}.
     */
    public void addTableName(TableName tableName) {
        this.tableNames.add(tableName);
        if (tableName.getAlias() != null) {
            tableAlias.put(tableName.getAlias(), tableName);
        }
    }

    public Set<TableName> getTableNames() {
        return tableNames;
    }

    public void setTableNames(Set<TableName> tableNames) {
        this.tableNames = tableNames;
    }

    public Set<CatalogName> getCatalogNames() {
        return catalogNames;
    }

    public void setCatalogNames(Set<CatalogName> catalogNames) {
        this.catalogNames = catalogNames;
    }

    public List<Selector> getSelectors() {
        return selectors;
    }

    public void setSelectors(List<Selector> selectors) {
        this.selectors = selectors;
    }

    public InnerJoin getJoin() {
        return join;
    }

    public void setJoin(InnerJoin join) {
        this.join = join;
    }

    public List<Relation> getWhere() {
        return where;
    }

    public void setWhere(List<Relation> where) {
        this.where = where;
    }

    public List<OrderByClause> getOrderByClauseClauses() {
        return orderByClauseClauses;
    }

    public void setOrderByClauseClauses(List<OrderByClause> orderByClauseClauses) {
        this.orderByClauseClauses = orderByClauseClauses;
    }

    public GroupByClause getGroupByClause() {
        return groupByClause;
    }

    public void setGroupByClause(GroupByClause groupByClause) {
        this.groupByClause = groupByClause;
    }

    public List<TableMetadata> getTablesMetadata() {
        //recover all Metadata about a tableName
        for (TableName tableName : tableNames) {
            tablesMetadata.add(MetadataManager.MANAGER.getTable(tableName));
        }
        return tablesMetadata;
    }

    /**
     * Get whether an alias for a given table name exists.
     *
     * @param name The alias name.
     * @return Whether it exists or not.
     */
    public boolean existTableAlias(final String name) {
        return tableAlias.containsKey(name);
    }

    /**
     * Get whether an alias for a given column name exists.
     *
     * @param name The alias name.
     * @return Whether it exists or not.
     */
    public boolean existColumnAlias(final String name) {
        return columnNameAlias.containsKey(name);
    }

    /**
     * Get the table name associated with an alias.
     *
     * @param alias The alias.
     * @return A {@link com.stratio.crossdata.common.data.TableName} or null if not exists.
     */
    public TableName getTableName(final String alias) {
        return tableAlias.get(alias);
    }

    /**
     * Get the column name associated with an alias.
     *
     * @param alias The alias.
     * @return A {@link com.stratio.crossdata.common.data.ColumnName} or null if not exists.
     */
    public ColumnName getColumnName(final String alias) {
        return columnNameAlias.get(alias);
    }

    public Map<String, String> getSignatures() {
        return signatures;
    }

    public void addSignature(String functionName, String signature) {
        signatures.put(functionName, signature);
    }
}
