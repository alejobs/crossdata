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

package com.stratio.crossdata.core.planner;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.stratio.crossdata.common.data.ColumnName;
import com.stratio.crossdata.common.data.TableName;
import com.stratio.crossdata.common.metadata.TableMetadata;
import com.stratio.crossdata.common.statements.structures.ColumnSelector;
import com.stratio.crossdata.common.statements.structures.Relation;
import com.stratio.crossdata.common.statements.structures.Selector;
import com.stratio.crossdata.core.query.SelectParsedQuery;
import com.stratio.crossdata.core.query.SelectValidatedQuery;
import com.stratio.crossdata.core.statements.SelectStatement;
import com.stratio.crossdata.core.structures.InnerJoin;

/**
 * Query wrapper to return non-processed list of tables, columns, etc.
 */
public class SelectValidatedQueryWrapper extends SelectValidatedQuery {

    /**
     * Class logger.
     */
    private static final Logger LOG = Logger.getLogger(SelectValidatedQueryWrapper.class);

    private SelectStatement stmt = null;

    private List<TableMetadata> tableMetadataList = new ArrayList<>();

    public SelectValidatedQueryWrapper(SelectStatement stmt, SelectParsedQuery parsedQuery) {
        super(parsedQuery);
        this.stmt = stmt;
    }

    public void addTableMetadata(TableMetadata tm) {
        LOG.info("Adding " + tm.getName().getQualifiedName());
        tableMetadataList.add(tm);
    }

    @Override
    public List<TableName> getTables() {
        List<TableName> tableNames = new ArrayList<>();
        tableNames.add(stmt.getTableName());
        InnerJoin join = stmt.getJoin();
        if (join != null) {
            tableNames.add(join.getTablename());
        }
        return tableNames;
    }

    @Override
    public List<ColumnName> getColumns() {
        List<ColumnName> columnNames = new ArrayList<>();
        for (Selector s : stmt.getSelectExpression().getSelectorList()) {
            columnNames.addAll(getSelectorColumns(s));
        }
        InnerJoin join = stmt.getJoin();
        if (join != null) {
            for (Relation r : join.getRelations()) {
                columnNames.addAll(getRelationColumns(r));
            }
        }
        return columnNames;
    }

    private List<ColumnName> getSelectorColumns(Selector r) {
        List<ColumnName> result = new ArrayList<>();
        if (ColumnSelector.class.isInstance(r)) {
            result.add(ColumnSelector.class.cast(r).getName());
        }
        return result;
    }

    private List<ColumnName> getRelationColumns(Relation r) {
        List<ColumnName> result = new ArrayList<>();
        result.addAll(getSelectorColumns(r.getLeftTerm()));
        return result;
    }

    @Override
    public InnerJoin getJoin() {
        return stmt.getJoin();
    }

    @Override
    public List<Relation> getRelations() {
        return stmt.getWhere();
    }

    @Override
    public List<TableMetadata> getTableMetadata() {
        return tableMetadataList;
    }
}
