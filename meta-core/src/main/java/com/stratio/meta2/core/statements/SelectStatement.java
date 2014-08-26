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

package com.stratio.meta2.core.statements;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.stratio.meta.common.result.CommandResult;
import com.stratio.meta.common.result.QueryResult;
import com.stratio.meta.common.result.Result;
import com.stratio.meta.common.statements.structures.relationships.Relation;
import com.stratio.meta.common.statements.structures.relationships.RelationCompare;
import com.stratio.meta.common.statements.structures.relationships.RelationIn;
import com.stratio.meta.common.statements.structures.relationships.RelationToken;
import com.stratio.meta.common.statements.structures.selectors.GroupByFunction;
import com.stratio.meta.common.statements.structures.selectors.SelectorFunction;
import com.stratio.meta.common.statements.structures.selectors.SelectorGroupBy;
import com.stratio.meta.common.statements.structures.selectors.SelectorIdentifier;
import com.stratio.meta.common.statements.structures.selectors.SelectorMeta;
import com.stratio.meta.common.statements.structures.window.Window;
import com.stratio.meta.common.statements.structures.window.WindowType;
import com.stratio.meta.common.utils.StringUtils;
import com.stratio.meta.core.engine.EngineConfig;
import com.stratio.meta.core.metadata.CustomIndexMetadata;
import com.stratio.meta.core.metadata.MetadataManager;
import com.stratio.meta.core.structures.GroupBy;
import com.stratio.meta.core.structures.InnerJoin;
import com.stratio.meta.core.structures.Selection;
import com.stratio.meta.core.structures.SelectionClause;
import com.stratio.meta.core.structures.SelectionList;
import com.stratio.meta.core.structures.SelectionSelector;
import com.stratio.meta.core.structures.SelectionSelectors;
import com.stratio.meta2.common.statements.structures.terms.Term;
import com.stratio.meta2.core.structures.OrderDirection;
import com.stratio.meta2.core.structures.Ordering;
import com.stratio.streaming.api.IStratioStreamingAPI;
import com.stratio.streaming.commons.messages.ColumnNameTypeValue;

/**
 * Class that models a {@code SELECT} statement from the META language.
 */
public class SelectStatement extends MetaStatement {

  /**
   * Maximum limit of rows to be retreived in a query.
   */
  private static final int MAX_LIMIT = 10000;

  /**
   * The {@link com.stratio.meta.core.structures.SelectionClause} of the Select statement.
   */
  private SelectionClause selectionClause = null;

  /**
   * The name of the target table.
   */
  private final String tableName;

  /**
   * Whether a time window has been specified in the Select statement.
   */
  private boolean windowInc = false;

  /**
   * The {@link com.stratio.meta.common.statements.structures.window.Window} specified in the Select statement for
   * streaming queries.
   */
  private Window window = null;

  /**
   * Whether a JOIN clause has been specified.
   */
  private boolean joinInc = false;

  /**
   * The {@link com.stratio.meta.core.structures.InnerJoin} clause.
   */
  private InnerJoin join = null;

  /**
   * Whether the Select contains a WHERE clause.
   */
  private boolean whereInc = false;

  /**
   * The list of {@link com.stratio.meta.common.statements.structures.relationships.Relation} found in the WHERE clause.
   */
  private List<Relation> where = null;

  /**
   * Whether an ORDER BY clause has been specified.
   */
  private boolean orderInc = false;

  /**
   * The list of {@link com.stratio.meta2.core.structures.Ordering} clauses.
   */
  private List<Ordering> order = null;

  /**
   * Whether a GROUP BY clause has been specified.
   */
  private boolean groupInc = false;

  /**
   * The {@link com.stratio.meta.core.structures.GroupBy} clause.
   */
  private List<GroupBy> group = null;

  /**
   * Whether a LIMIT clause has been specified.
   */
  private boolean limitInc = false;

  /**
   * The LIMIT in terms of the number of rows to be retrieved in the result of the SELECT statement.
   */
  private int limit = 0;

  /**
   * Flag to disable complex analytic functions such as INNER JOIN.
   */
  private boolean disableAnalytics = false;

  // TODO: We should probably remove this an pass it as parameters.
  /**
   * The {@link com.stratio.meta.core.metadata.MetadataManager} used to retrieve table metadata
   * during the validation process and the statement execution phase.
   */
  private MetadataManager metadata = null;

  /**
   * The {@link com.datastax.driver.core.TableMetadata} associated with the table specified in the
   * FROM of the Select statement.
   */
  private TableMetadata tableMetadataFrom = null;

  /**
   * Map with the collection of {@link com.datastax.driver.core.ColumnMetadata} associated with the
   * tables specified in the FROM and the INNER JOIN parts of the Select statement. A virtual table
   * named {@code any} is used to match unqualified column names.
   */
  private Map<String, Collection<ColumnMetadata>> columns = new HashMap<>();

  private boolean streamMode = false;

  /**
   * Class logger.
   */
  private static final Logger LOG = Logger.getLogger(SelectStatement.class);

  private Map<String, String> fieldsAliasesMap;

  /**
   * Class constructor.
   * 
   * @param tableName The name of the target table.
   */
  public SelectStatement(String tableName) {
    this.command = false;
    if (tableName.contains(".")) {
      String[] ksAndTablename = tableName.split("\\.");
      catalog = ksAndTablename[0];
      this.tableName = ksAndTablename[1];
      catalogInc = true;
    } else {
      this.tableName = tableName;
    }

  }

  /**
   * Class constructor.
   * 
   * @param selectionClause The {@link com.stratio.meta.core.structures.SelectionClause} of the
   *        Select statement.
   * @param tableName The name of the target table.
   */
  public SelectStatement(SelectionClause selectionClause, String tableName) {
    this(tableName);
    this.selectionClause = selectionClause;
    this.selectionClause.addTablename(this.tableName);
  }

  /**
   * Get the keyspace specified in the select statement.
   * 
   * @return The keyspace or null if not specified.
   */
  public String getKeyspace() {
    return catalog;
  }

  /**
   * Set the keyspace specified in the select statement.
   * 
   * @param keyspace The name of the keyspace.
   */
  public void setKeyspace(String keyspace) {
    this.catalogInc = true;
    this.catalog = keyspace;
  }

  /**
   * Get the name of the target table.
   * 
   * @return The table name.
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * Get the {@link com.stratio.meta.core.structures.SelectionClause}.
   * 
   * @return The selection clause.
   */
  public SelectionClause getSelectionClause() {
    return selectionClause;
  }

  /**
   * Set the {@link com.stratio.meta.core.structures.SelectionClause} for selecting columns.
   * 
   * @param selectionClause selection clause.
   */
  public void setSelectionClause(SelectionClause selectionClause) {
    this.selectionClause = selectionClause;
  }

  /**
   * Set the {@link com.stratio.meta.common.statements.structures.window.Window} for streaming queries.
   * 
   * @param window The window.
   */
  public void setWindow(Window window) {
    this.windowInc = true;
    this.window = window;
  }

  /**
   * Get the Join clause.
   * 
   * @return The Join or null if not set.
   */
  public InnerJoin getJoin() {
    return join;
  }

  /**
   * Set the {@link com.stratio.meta.core.structures.InnerJoin} clause.
   * 
   * @param join The join clause.
   */
  public void setJoin(InnerJoin join) {
    this.joinInc = true;
    this.join = join;
  }

  /**
   * Get the list of {@link Relation} in the where clause.
   * 
   * @return The list of relations.
   */
  public List<Relation> getWhere() {
    return where;
  }

  /**
   * Set the list of {@link Relation} in the where clause.
   * 
   * @param where The list of relations.
   */
  public void setWhere(List<Relation> where) {
    this.whereInc = true;
    this.where = where;
  }

  /**
   * Set the {@link Ordering} in the ORDER BY clause.
   * 
   * @param order The order.
   */
  public void setOrder(List<Ordering> order) {
    this.orderInc = true;
    this.order = order;
  }

  /**
   * Return ORDER BY clause.
   * 
   * @return list of {@link com.stratio.meta2.core.structures.Ordering}.
   */
  public List<Ordering> getOrder() {
    return order;
  }

  /**
   * Check if ORDER BY clause is included.
   * 
   * @return {@code true} if is included.
   */
  public boolean isOrderInc() {
    return orderInc;
  }

  /**
   * Set the {@link com.stratio.meta.core.structures.GroupBy} clause.
   * 
   * @param group The group by.
   */
  public void setGroup(List<GroupBy> group) {
    this.groupInc = true;
    this.group = group;
  }

  /**
   * Return GROUP BY clause.
   * 
   * @return list of {@link com.stratio.meta.core.structures.GroupBy}.
   */
  public List<GroupBy> getGroup() {
    return group;
  }

  /**
   * Check if GROUP BY clause is included.
   * 
   * @return {@code true} if is included.
   */
  public boolean isGroupInc() {
    return groupInc;
  }

  /**
   * Check if a WHERE clause is included.
   * 
   * @return Whether it is included.
   */
  public boolean isWhereInc() {
    return whereInc;
  }

  /**
   * Set the LIMIT of the query.
   * 
   * @param limit The maximum number of rows to be returned.
   */
  public void setLimit(int limit) {
    this.limitInc = true;
    if (limit <= MAX_LIMIT) {
      this.limit = limit;
    } else {
      this.limit = MAX_LIMIT;
    }
  }

  public int getLimit() {
    return limit;
  }

  public Window getWindow() {
    return window;
  }

  public MetadataManager getMetadata() {
    return metadata;
  }

  /**
   * Disable the analytics mode.
   * 
   * @param disableAnalytics Whether analytics are enable (default) or not.
   */
  public void setDisableAnalytics(boolean disableAnalytics) {
    this.disableAnalytics = disableAnalytics;
  }

  /**
   * Add a {@link com.stratio.meta.core.structures.SelectionSelector} to the
   * {@link com.stratio.meta.core.structures.SelectionClause}.
   * 
   * @param selSelector The new selector.
   */
  public void addSelection(SelectionSelector selSelector) {
    if (selectionClause == null) {
      SelectionSelectors selSelectors = new SelectionSelectors();
      selectionClause = new SelectionList(selSelectors);
    }
    SelectionList selList = (SelectionList) selectionClause;
    SelectionSelectors selSelectors = (SelectionSelectors) selList.getSelection();
    selSelectors.addSelectionSelector(selSelector);
  }

  public Map<String, String> getFieldsAliasesMap() {
    return fieldsAliasesMap;
  }

  public void setFieldsAliasesMap(Map<String, String> fieldsAliasesMap) {
    this.fieldsAliasesMap = fieldsAliasesMap;
  }

  /**
   * Creates a String representing the Statement with META syntax.
   * 
   * @return String
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SELECT ");
    if (selectionClause != null) {
      sb.append(selectionClause.toString());
    }
    sb.append(" FROM ");
    if (catalogInc) {
      sb.append(catalog).append(".");
    }
    sb.append(tableName);
    if (windowInc) {
      sb.append(" WITH WINDOW ").append(window.toString());
    }
    if (joinInc) {
      sb.append(" INNER JOIN ").append(join.toString());
    }
    if (whereInc) {
      sb.append(" WHERE ");
      sb.append(StringUtils.stringList(where, " AND "));
    }
    if (orderInc) {
      sb.append(" ORDER BY ").append(StringUtils.stringList(order, ", "));
    }
    if (groupInc) {
      sb.append(" GROUP BY ").append(StringUtils.stringList(group, ", "));
    }
    if (limitInc) {
      sb.append(" LIMIT ").append(limit);
    }
    if (disableAnalytics) {
      sb.append(" DISABLE ANALYTICS");
    }

    return sb.toString().replace("  ", " ");
  }

  /** {@inheritDoc} */
  @Override
  public Result validate(MetadataManager metadata, EngineConfig config) {
    // Validate FROM keyspace
    Result result =
        validateKeyspaceAndTable(metadata, sessionCatalog, catalogInc, catalog, tableName);

    if ((!result.hasError()) && (result instanceof CommandResult)
        && ("streaming".equalsIgnoreCase(((CommandResult) result).getResult().toString()))) {
      streamMode = true;
    }

    if (!streamMode && windowInc) {
      result =
          Result
              .createValidationErrorResult("Window option can only be applied to ephemeral tables.");
    }

    if (streamMode && !windowInc) {
      result = Result.createValidationErrorResult("Window is mandatory for ephemeral tables.");
    }

    if (!result.hasError() && joinInc) {
      result =
          validateKeyspaceAndTable(metadata, sessionCatalog, join.isKeyspaceInc(),
              join.getKeyspace(), join.getTablename());
    }

    String effectiveKs1 = getEffectiveCatalog();
    String effectiveKs2 = null;
    if (joinInc) {
      SelectStatement secondSelect = new SelectStatement("");
      if (join.getKeyspace() != null) {
        secondSelect.setKeyspace(join.getKeyspace());
      }
      secondSelect.setSessionCatalog(this.sessionCatalog);
      effectiveKs2 = secondSelect.getEffectiveCatalog();
    }

    TableMetadata tableMetadataJoin = null;

    com.stratio.meta.common.metadata.structures.TableMetadata streamingMetadata = null;
    if (!result.hasError()) {
      // Cache Metadata manager and table metadata for the getDriverStatement.
      this.metadata = metadata;
      if (streamMode) {
        streamingMetadata = metadata.convertStreamingToMeta(getEffectiveCatalog(), tableName);
      } else {
        tableMetadataFrom = metadata.getTableMetadata(effectiveKs1, tableName);
      }
      if (joinInc) {
        tableMetadataJoin = metadata.getTableMetadata(effectiveKs2, join.getTablename());
      }

      if (streamMode) {
        result = validateSelectionColumns(streamingMetadata, tableMetadataJoin);
      } else {
        result = validateSelectionColumns(tableMetadataFrom, tableMetadataJoin);
      }

      if (!result.hasError()) {
        result = validateOptions();
      }
    }

    if (!result.hasError() && joinInc) {
      if (streamMode) {
        result = validateJoinClause(streamingMetadata, tableMetadataJoin);
      } else {
        result = validateJoinClause(tableMetadataFrom, tableMetadataJoin);
      }
    }

    if (!result.hasError() && whereInc) {
      if (streamMode) {
        result =
            Result
                .createValidationErrorResult("Where clauses in ephemeral tables are not supported yet.");
      } else {
        result = validateWhereClause(tableMetadataFrom);
      }

    }

    /*
     * if(!result.hasError() && windowInc){ result = validateWindow(config); }
     */

    return result;
  }

  private Result validateWindow(EngineConfig config) {
    Result result = QueryResult.createSuccessQueryResult();
    if (WindowType.TEMPORAL.equals(window.getType())) {
      long windowMillis = window.getDurationInMilliseconds();
      if (windowMillis % config.getStreamingDuration() != 0) {
        result =
            Result.createValidationErrorResult("Window time must be multiple of "
                + config.getStreamingDuration() + " milliseconds.");
      }
    } else {
      result = Result.createValidationErrorResult("This type of window is not supported yet.");
    }
    return result;
  }

  /**
   * Validate the supported select options.
   * 
   * @return A {@link com.stratio.meta.common.result.Result} with the validation result.
   */
  private Result validateOptions() {
    Result result = QueryResult.createSuccessQueryResult();

    if (groupInc) {
      result = validateGroupByClause();
    }

    if (orderInc) {
      result = validateOrderByClause();
    }

    return result;
  }

  private boolean checkSelectorExists(SelectorIdentifier selector) {
    return !findColumn(selector.getTable(), selector.getField()).hasError();
  }

  /**
   * Validate the JOIN clause.
   * 
   * @param tableFrom The table in the FROM clause.
   * @param tableJoin The table in the JOIN clause.
   * @return Whether the specified table names and fields are valid.
   */
  // TODO validateJoinClause
  private Result validateJoinClause(TableMetadata tableFrom, TableMetadata tableJoin) {
    Result result = QueryResult.createSuccessQueryResult();
    if (joinInc) {
      if (!checkSelectorExists(join.getLeftField())) {
        result =
            Result.createValidationErrorResult("Join selector " + join.getLeftField().toString()
                + " table or column name not found");
      }
      if (!checkSelectorExists(join.getRightField())) {
        result =
            Result.createValidationErrorResult("Join selector " + join.getRightField().toString()
                + " table or column name not found");
      }
    }

    return result;
  }

  private Result validateJoinClause(
      com.stratio.meta.common.metadata.structures.TableMetadata streamingMetadata,
      TableMetadata tableMetadataJoin) {
    Result result = QueryResult.createSuccessQueryResult();
    if (joinInc) {

      SelectorIdentifier leftField = join.getLeftField();
      SelectorIdentifier rightField = join.getRightField();

      boolean streamingLeft = false;
      boolean batchLeft = false;
      if (leftField.getTable().equalsIgnoreCase(streamingMetadata.getTableName())) {
        if (streamingMetadata.getColumn(leftField.getField()) == null) {
          result =
              Result.createValidationErrorResult("Ephemeral table '"
                  + streamingMetadata.getTableName() + "' doesn't contain the field '"
                  + leftField.getField() + "'.");
        } else {
          streamingLeft = true;
        }
      } else if (leftField.getTable().equalsIgnoreCase(tableMetadataJoin.getName())) {
        if (tableMetadataJoin.getColumn(leftField.getField()) == null) {
          result =
              Result.createValidationErrorResult("Table '" + tableMetadataJoin.getName()
                  + "' doesn't contain the field '" + leftField.getField() + "'.");
        } else {
          batchLeft = true;
        }
      } else {
        result =
            Result.createValidationErrorResult("Table '" + leftField.getTable()
                + "' doesn't match any of the incoming tables.");
      }

      if (!result.hasError()) {
        if (streamingLeft) {
          if (tableMetadataJoin.getColumn(rightField.getField()) == null) {
            result =
                Result.createValidationErrorResult("Table '" + tableMetadataJoin.getName()
                    + "' doesn't contain the field '" + rightField.getField() + "'.");
          }
        } else if (batchLeft) {
          if (streamingMetadata.getColumn(rightField.getField()) == null) {
            result =
                Result.createValidationErrorResult("Ephemeral table '"
                    + streamingMetadata.getTableName() + "' doesn't contain the field '"
                    + rightField.getField() + "'.");
          }
        }
      }

    }

    return result;
  }

  /**
   * Validate a relation found in a where clause.
   * 
   * @param targetTable The target table.
   * @param column The name of the column.
   * @param terms The terms.
   * @param rc Relation of Comparator type.
   * @return Whether the relation is valid.
   */
  private Result validateWhereSingleColumnRelation(String targetTable, String column,
      List<Term<?>> terms, Relation rc) {
    Result result = QueryResult.createSuccessQueryResult();

    String operator = rc.getOperator();

    ColumnMetadata cm = findColumnMetadata(targetTable, column);
    if (cm != null) {
      Iterator<Term<?>> termsIt = terms.iterator();
      Class<?> columnType = cm.getType().asJavaClass();
      while (!result.hasError() && termsIt.hasNext()) {
        Term<?> term = termsIt.next();
        if (!columnType.equals(term.getTermClass())) {
          result =
              Result.createValidationErrorResult("Column [" + column + "] of type [" + columnType
                  + "] does not accept " + term.getTermClass() + " values (" + term.toString()
                  + ")");
        }
      }

      if (Boolean.class.equals(columnType)) {
        boolean supported = true;
        switch (operator) {
          case ">":
          case "<":
          case ">=":
          case "<=":
          case "in":
          case "between":
            supported = false;
            break;
          default:
            break;
        }
        if (!supported) {
          result =
              Result.createValidationErrorResult("Operand " + operator + " not supported for"
                  + " column " + column + ".");
        }
      }
    } else {
      result =
          Result.createValidationErrorResult("Column " + column + " not found in " + targetTable
              + " table.");
    }

    return result;
  }

  /**
   * Validate that the where clause is valid by checking that columns exists on the target table and
   * that the comparisons are semantically valid.
   * 
   * @return A {@link com.stratio.meta.common.result.Result} with the validation result.
   */
  private Result validateWhereClause(TableMetadata tableMetadata) {
    // TODO: Check that the MATCH operator is only used in Lucene mapped columns.
    Result result = QueryResult.createSuccessQueryResult();
    Iterator<Relation> relations = where.iterator();
    while (!result.hasError() && relations.hasNext()) {
      Relation relation = relations.next();
      //TODO Uncomment
      //relation.updateTermClass(tableMetadata);
      if (Relation.TYPE_COMPARE == relation.getType() || Relation.TYPE_IN == relation.getType()
          || Relation.TYPE_BETWEEN == relation.getType()) {
        // Check comparison, =, >, <, etc.
        // RelationCompare rc = RelationCompare.class.cast(relation);
        String column = relation.getIdentifiers().get(0).toString();
        // Determine the target table the column belongs to.
        String targetTable = "any";
        if (column.contains(".")) {
          String[] tableAndColumn = column.split("\\.");
          targetTable = tableAndColumn[0];
          column = tableAndColumn[1];
        }

        // Check terms types
        result =
            validateWhereSingleColumnRelation(targetTable, column, relation.getTerms(), relation);
        if ("match".equalsIgnoreCase(relation.getOperator()) && joinInc) {
          result =
              Result
                  .createValidationErrorResult("Select statements with 'Inner Join' don't support MATCH operator.");
        }
      } else if (Relation.TYPE_TOKEN == relation.getType()) {
        // TODO: Check TOKEN relation
        result = Result.createValidationErrorResult("TOKEN function not supported.");
      }
    }

    return result;
  }

  /**
   * Validate whether the group by clause is valid or not by checking columns exist on the target
   * table and comparisons are semantically correct.
   * 
   * @return A {@link com.stratio.meta.common.result.Result} with the validation result.
   */
  private Result validateGroupByClause() {

    Result result = QueryResult.createSuccessQueryResult();

    List<String> selectionCols = this.getSelectionClause().getIds();

    for (GroupBy groupByCol : this.group) {
      String col = groupByCol.toString();
      if (!selectionCols.contains(col)) {
        this.getSelectionClause().getIds().add(col);
      }
    }
    return result;
  }

  /**
   * Validate whether the group by clause is valid or not by checking columns exist on the target
   * table and comparisons are semantically correct.
   * 
   * @return A {@link com.stratio.meta.common.result.Result} with the validation result.
   */
  private Result validateOrderByClause() {

    Result result = QueryResult.createSuccessQueryResult();

    for (Ordering orderField : order) {

      String field = orderField.getSelectorIdentifier().toString();

      String targetTable = "any";
      String columnName = field;
      if (field.contains(".")) {
        targetTable = field.substring(0, field.indexOf("."));
        columnName = field.substring(field.indexOf(".") + 1);
      }

      Result columnResult = findColumn(targetTable, columnName);
      if (columnResult.hasError()) {
        result = columnResult;
      }
    }

    return result;
  }

  /**
   * Find a column in the selected tables.
   * 
   * @param table The target table of the column.
   * @param column The name of the column.
   * @return A {@link com.stratio.meta.common.result.Result}.
   */
  private Result findColumn(String table, String column) {

    Result result = QueryResult.createSuccessQueryResult();
    boolean found = false;

    if (columns.get(table) != null) {

      Iterator<ColumnMetadata> it = columns.get(table).iterator();
      while (!found && it.hasNext()) {
        ColumnMetadata cm = it.next();
        if (cm.getName().equals(column)) {
          found = true;
        }
      }
      if (!found) {
        result =
            Result.createValidationErrorResult("Column " + column + " does not " + "exist in "
                + table + " table.");
      }

    } else {
      result =
          Result.createValidationErrorResult("Column " + column + " refers to table " + table
              + " that has not been specified on query.");
    }
    return result;
  }

  /**
   * Find a column in the selected tables.
   * 
   * @param table The target table of the column.
   * @param column The name of the column.
   * @return A {@link com.datastax.driver.core.ColumnMetadata} or null if not found.
   */
  private ColumnMetadata findColumnMetadata(String table, String column) {

    ColumnMetadata result = null;
    boolean found = false;

    if (columns.get(table) != null) {
      Iterator<ColumnMetadata> it = columns.get(table).iterator();
      while (!found && it.hasNext()) {
        ColumnMetadata cm = it.next();
        if (cm.getName().equals(column)) {
          found = true;
          result = cm;
        }
      }
    }
    return result;
  }

  /**
   * Validate that the columns specified in the select are valid by checking that the selection
   * columns exists in the table.
   * 
   * @param tableFrom The {@link com.datastax.driver.core.TableMetadata} associated with the FROM
   *        table.
   * @param tableJoin The {@link com.datastax.driver.core.TableMetadata} associated with the JOIN
   *        table.
   * @return A {@link com.stratio.meta.common.result.Result} with the validation result.
   */
  private Result validateSelectionColumns(TableMetadata tableFrom, TableMetadata tableJoin) {
    Result result = QueryResult.createSuccessQueryResult();

    if (streamMode && (selectionClause instanceof SelectionList)
        && (((SelectionList) selectionClause).getTypeSelection() == Selection.TYPE_SELECTOR)) {
      List<String> colNames =
          metadata.getStreamingColumnNames(getEffectiveCatalog() + "_" + tableName);
      SelectionList selectionList = (SelectionList) selectionClause;
      SelectionSelectors selectionSelectors = (SelectionSelectors) selectionList.getSelection();
      selectionSelectors.getSelectors();

      for (SelectionSelector selectionSelector : selectionSelectors.getSelectors()) {
        SelectorIdentifier selectorIdentifier =
            (SelectorIdentifier) selectionSelector.getSelector();
        String colName = selectorIdentifier.getField();
        if (!colNames.contains(colName.toLowerCase())) {
          return Result.createValidationErrorResult("Column '" + colName
              + "' not found in ephemeral table '" + getEffectiveCatalog() + "." + tableName
              + "'.");
        }
      }
    }

    if (streamMode) {
      return result;
    }

    // Create a HashMap with the columns
    Collection<ColumnMetadata> allColumns = new ArrayList<>();
    columns.put(tableFrom.getName(), tableFrom.getColumns());
    allColumns.addAll(tableFrom.getColumns());
    if (joinInc) {
      // TODO: Check that what happens if two columns from t1 and t2 have the same name.
      columns.put(tableJoin.getName(), tableJoin.getColumns());
      allColumns.addAll(tableJoin.getColumns());
    }
    columns.put("any", allColumns);

    Result columnResult = null;

    boolean check = false;
    SelectionList sl = null;
    if (selectionClause.getType() == SelectionClause.TYPE_SELECTION) {
      sl = SelectionList.class.cast(selectionClause);
      // Check columns only if an asterisk is not selected.
      if (sl.getSelection().getType() == Selection.TYPE_SELECTOR) {
        check = true;
      }
    }

    if (!check) {
      return result;
    }

    SelectionSelectors ss = SelectionSelectors.class.cast(sl.getSelection());
    for (SelectionSelector selector : ss.getSelectors()) {
      if (selector.getSelector() instanceof SelectorIdentifier) {
        SelectorIdentifier si = SelectorIdentifier.class.cast(selector.getSelector());

        columnResult = findColumn(si.getTable(), si.getField());
        if (columnResult.hasError()) {
          result = columnResult;
        }
      } else if (selector.getSelector() instanceof SelectorGroupBy) {

        if (groupInc) {
          SelectorGroupBy selectorMeta = (SelectorGroupBy) selector.getSelector();

          if (!selectorMeta.getGbFunction().equals(GroupByFunction.COUNT)) {
            // Checking column in the group by aggregation function
            if (selectorMeta.getParam().getType() == SelectorMeta.TYPE_IDENT) {
              SelectorIdentifier subselectorIdentifier =
                  (SelectorIdentifier) selectorMeta.getParam();

              columnResult =
                  findColumn(subselectorIdentifier.getTable(), subselectorIdentifier.getField());
              if (columnResult.hasError()) {
                result = columnResult;
              }
            } else {
              result =
                  Result
                      .createValidationErrorResult("Nested functions on selected fields not supported.");
            }
          }
        }
      } else {
        result =
            Result.createValidationErrorResult("Functions type on selected fields not supported.");
      }
    }

    return result;
  }

  private Result validateSelectionColumns(
      com.stratio.meta.common.metadata.structures.TableMetadata streamingMetadata,
      TableMetadata tableJoin) {
    Result result = QueryResult.createSuccessQueryResult();

    if ((selectionClause instanceof SelectionList)
        && (((SelectionList) selectionClause).getTypeSelection() == Selection.TYPE_SELECTOR)) {
      SelectionList selectionList = (SelectionList) selectionClause;
      SelectionSelectors selectionSelectors = (SelectionSelectors) selectionList.getSelection();
      selectionSelectors.getSelectors();

      for (SelectionSelector selectionSelector : selectionSelectors.getSelectors()) {
        SelectorIdentifier selectorIdentifier =
            (SelectorIdentifier) selectionSelector.getSelector();
        String tableName = selectorIdentifier.getTable();
        String colName = selectorIdentifier.getField();

        result = findColumn(streamingMetadata, tableJoin, colName);
      }
    }

    return result;
  }

  private Result findColumn(
      com.stratio.meta.common.metadata.structures.TableMetadata streamingMetadata,
      TableMetadata tableJoin, String colName) {
    Result result = QueryResult.createSuccessQueryResult();
    if (tableName.equalsIgnoreCase(streamingMetadata.getTableName())) {
      if (streamingMetadata.getColumn(colName) == null) {
        result =
            Result.createValidationErrorResult("Field '" + colName
                + "' not found in ephemeral table '" + tableName + "'.");
      }
    } else if (tableName.equalsIgnoreCase(tableJoin.getName())) {
      if (tableJoin.getColumn(colName) == null) {
        result =
            Result.createValidationErrorResult("Field '" + colName + "' not found in table '"
                + tableName + "'.");
      }
    } else {
      result =
          Result.createValidationErrorResult("Table '" + tableName
              + "' doesn't match to any incoming tables.");
    }
    return result;
  }

  /**
   * Get the processed where clause to be sent to Cassandra related with lucene indexes.
   * 
   * @param metadata The {@link com.stratio.meta.core.metadata.MetadataManager} that provides the
   *        required information.
   * @param tableMetadata The associated {@link com.datastax.driver.core.TableMetadata}.
   * @return A String array with the column name and the lucene query, or null if no index is found.
   */
  public String[] getLuceneWhereClause(MetadataManager metadata, TableMetadata tableMetadata) {
    String[] result = null;
    CustomIndexMetadata luceneIndex = metadata.getLuceneIndex(tableMetadata);
    int addedClauses = 0;
    if (luceneIndex != null) {
      // TODO: Check in the validator that the query uses AND with the lucene mapped columns.
      StringBuilder sb = new StringBuilder("{filter:{type:\"boolean\",must:[");

      // Iterate throughout the relations of the where clause looking for MATCH.
      for (Relation relation : where) {
        if (Relation.TYPE_COMPARE == relation.getType()
            && "MATCH".equalsIgnoreCase(relation.getOperator())) {
          RelationCompare rc = RelationCompare.class.cast(relation);
          // String column = rc.getIdentifiers().get(0).toString();
          String column = rc.getIdentifiers().get(0).getField();
          String value = rc.getTerms().get(0).toString();
          // Generate query for column
          String[] processedQuery = processLuceneQueryType(value);
          sb.append("{type:\"");
          sb.append(processedQuery[0]);
          sb.append("\",field:\"");
          sb.append(column);
          sb.append("\",value:\"");
          sb.append(processedQuery[1]);
          sb.append("\"},");
          addedClauses++;
        }
      }
      sb.replace(sb.length() - 1, sb.length(), "");
      sb.append("]}}");
      if (addedClauses > 0) {
        result = new String[] {luceneIndex.getIndexName(), sb.toString()};
      }
    }
    return result;
  }

  /**
   * Process a query pattern to determine the type of Lucene query. The supported types of queries
   * are: <li>
   * <ul>
   * Wildcard: The query contains * or ?.
   * </ul>
   * <ul>
   * Fuzzy: The query ends with ~ and a number.
   * </ul>
   * <ul>
   * Regex: The query contains [ or ].
   * </ul>
   * <ul>
   * Match: Default query, supporting escaped symbols: *, ?, [, ], etc.
   * </ul>
   * </li>
   * 
   * @param query The user query.
   * @return An array with the type of query and the processed query.
   */
  protected String[] processLuceneQueryType(String query) {
    String[] result = {"", ""};
    Pattern escaped = Pattern.compile(".*\\\\\\*.*|.*\\\\\\?.*|.*\\\\\\[.*|.*\\\\\\].*");
    Pattern wildcard = Pattern.compile(".*\\*.*|.*\\?.*");
    Pattern regex = Pattern.compile(".*\\].*|.*\\[.*");
    Pattern fuzzy = Pattern.compile(".*~\\d+");
    if (escaped.matcher(query).matches()) {
      result[0] = "match";
      result[1] =
          query.replace("\\*", "*").replace("\\?", "?").replace("\\]", "]").replace("\\[", "[");
    } else if (regex.matcher(query).matches()) {
      result[0] = "regex";
      result[1] = query;
    } else if (fuzzy.matcher(query).matches()) {
      result[0] = "fuzzy";
      result[1] = query;
    } else if (wildcard.matcher(query).matches()) {
      result[0] = "wildcard";
      result[1] = query;
    } else {
      result[0] = "match";
      result[1] = query;
    }
    // C* Query builder doubles the ' character.
    result[1] = result[1].replaceAll("^'", "").replaceAll("'$", "");
    return result;
  }

  /**
   * Creates a String representing the Statement with CQL syntax.
   * 
   * @return CQL string
   */
  @Override
  public String translateToCQL() {
    StringBuilder sb = new StringBuilder(this.toString());

    if (sb.toString().contains("TOKEN(")) {
      int currentLength = 0;
      int newLength = sb.toString().length();
      while (newLength != currentLength) {
        currentLength = newLength;
        sb = new StringBuilder(sb.toString().replaceAll("(.*)" // $1
            + "(=|<|>|<=|>=|<>|LIKE)" // $2
            + "(\\s?)" // $3
            + "(TOKEN\\()" // $4
            + "([^'][^\\)]+)" // $5
            + "(\\).*)", // $6
            "$1$2$3$4'$5'$6"));
        sb = new StringBuilder(sb.toString().replaceAll("(.*TOKEN\\(')" // $1
            + "([^,]+)" // $2
            + "(,)" // $3
            + "(\\s*)" // $4
            + "([^']+)" // $5
            + "(')" // $6
            + "(\\).*)", // $7
            "$1$2'$3$4'$5$6$7"));
        sb = new StringBuilder(sb.toString().replaceAll("(.*TOKEN\\(')" // $1
            + "(.+)" // $2
            + "([^'])" // $3
            + "(,)" // $4
            + "(\\s*)" // $5
            + "([^']+)" // $6
            + "(')" // $7
            + "(\\).*)", // $8
            "$1$2$3'$4$5'$6$7$8"));
        sb = new StringBuilder(sb.toString().replaceAll("(.*TOKEN\\(')" // $1
            + "(.+)" // $2
            + "([^'])" // $3
            + "(,)" // $4
            + "(\\s*)" // $5
            + "([^']+)" // $6
            + "(')" // $7
            + "([^TOKEN]+)" // $8
            + "('\\).*)", // $9
            "$1$2$3'$4$5'$6$7$8$9"));
        newLength = sb.toString().length();
      }
    }

    return sb.toString();
  }

  @Override
  public String translateToSiddhi(IStratioStreamingAPI stratioStreamingAPI, String streamName,
      String outgoing) {
    StringBuilder querySb = new StringBuilder("from ");
    querySb.append(streamName);
    if (windowInc) {
      querySb.append("#window.timeBatch( ").append(getWindow().toString().toLowerCase())
          .append(" )");
    }

    List<String> ids = new ArrayList<>();
    boolean asterisk = false;
    SelectionClause selectionClause = getSelectionClause();
    if (selectionClause.getType() == SelectionClause.TYPE_SELECTION) {
      SelectionList selectionList = (SelectionList) selectionClause;
      Selection selection = selectionList.getSelection();
      if (selection.getType() == Selection.TYPE_ASTERISK) {
        asterisk = true;
      }
    }
    if (asterisk) {
      List<ColumnNameTypeValue> cols = null;
      try {
        cols = stratioStreamingAPI.columnsFromStream(streamName);
      } catch (Exception e) {
        LOG.error(e);
      }
      for (ColumnNameTypeValue ctv : cols) {
        ids.add(ctv.getColumn());
      }
    } else {
      ids = getSelectionClause().getFields();
    }

    String idsStr = Arrays.toString(ids.toArray()).replace("[", "").replace("]", "");
    querySb.append(" select ").append(idsStr).append(" insert into ");
    querySb.append(outgoing);
    return querySb.toString();
  }

  /**
   * Get the driver representation of the fields found in the selection clause.
   * 
   * @param selSelectors The selectors.
   * @param selection The current Select.Selection.
   * @return A {@link com.datastax.driver.core.querybuilder.Select.Selection}.
   */
  private Select.Selection getDriverBuilderSelection(SelectionSelectors selSelectors,
      Select.Selection selection) {
    Select.Selection result = selection;
    for (SelectionSelector selSelector : selSelectors.getSelectors()) {
      SelectorMeta selectorMeta = selSelector.getSelector();
      if (selectorMeta.getType() == SelectorMeta.TYPE_IDENT) {
        SelectorIdentifier selIdent = (SelectorIdentifier) selectorMeta;
        if (selSelector.isAliasInc()) {
          result = result.column(selIdent.getField()).as(selSelector.getAlias());
        } else {
          result = result.column(selIdent.getField());
        }
      } else if (selectorMeta.getType() == SelectorMeta.TYPE_FUNCTION) {
        SelectorFunction selFunction = (SelectorFunction) selectorMeta;
        List<SelectorMeta> params = selFunction.getParams();
        Object[] innerFunction = new Object[params.size()];
        int pos = 0;
        for (SelectorMeta selMeta : params) {
          innerFunction[pos] = QueryBuilder.raw(selMeta.toString());
          pos++;
        }
        result = result.fcall(selFunction.getName(), innerFunction);
      }
    }
    return result;
  }

  /**
   * Get the driver builder object with the selection clause.
   * 
   * @return A {@link com.datastax.driver.core.querybuilder.Select.Builder}.
   */
  private Select.Builder getDriverBuilder() {
    Select.Builder builder;
    if (selectionClause.getType() == SelectionClause.TYPE_COUNT) {
      builder = QueryBuilder.select().countAll();
    } else {
      // Selection type
      SelectionList selList = (SelectionList) selectionClause;
      if (selList.getSelection().getType() != Selection.TYPE_ASTERISK) {
        Select.Selection selection = QueryBuilder.select();
        if (selList.isDistinct()) {
          selection = selection.distinct();
        }
        // Select the required columns.
        SelectionSelectors selSelectors = (SelectionSelectors) selList.getSelection();
        builder = getDriverBuilderSelection(selSelectors, selection);
      } else {
        builder = QueryBuilder.select().all();
      }
    }
    return builder;
  }

  /**
   * Cast an input value to the class associated with the comparison column.
   * 
   * @param columnName The name of the column.
   * @param value The initial value.
   * @return A casted object.
   */
  private Object getWhereCastValue(String columnName, Object value) {
    Object result = null;
    Class<?> clazz = tableMetadataFrom.getColumn(columnName).getType().asJavaClass();

    if (String.class.equals(clazz)) {
      result = String.class.cast(value);
    } else if (UUID.class.equals(clazz)) {
      result = UUID.fromString(String.class.cast(value));
    } else if (Date.class.equals(clazz)) {
      // TODO getWhereCastValue with date
      result = null;
    } else {
      try {
        if (value.getClass().equals(clazz)) {
          result = clazz.getConstructor(String.class).newInstance(value.toString());
        } else {
          Method m = clazz.getMethod("valueOf", value.getClass());
          result = m.invoke(value);
        }
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException
          | NoSuchMethodException e) {
        LOG.error("Cannot parse input value", e);
      }
    }
    return result;
  }

  /**
   * Get the driver clause associated with a compare relation.
   * 
   * @param metaRelation The {@link com.stratio.meta.common.statements.structures.relationships.RelationCompare} clause.
   * @return A {@link com.datastax.driver.core.querybuilder.Clause}.
   */
  private Clause getRelationCompareClause(Relation metaRelation) {
    Clause clause = null;
    RelationCompare relCompare = (RelationCompare) metaRelation;
    String field = relCompare.getIdentifiers().get(0).getField();
    Object value = relCompare.getTerms().get(0).getTermValue();
    value = getWhereCastValue(field, value);
    switch (relCompare.getOperator().toUpperCase()) {
      case "=":
        clause = QueryBuilder.eq(field, value);
        break;
      case ">":
        clause = QueryBuilder.gt(field, value);
        break;
      case ">=":
        clause = QueryBuilder.gte(field, value);
        break;
      case "<":
        clause = QueryBuilder.lt(field, value);
        break;
      case "<=":
        clause = QueryBuilder.lte(field, value);
        break;
      case "MATCH":
        // Processed as LuceneIndex
        break;
      default:
        LOG.error("Unsupported operator: " + relCompare.getOperator());
        break;
    }
    return clause;
  }

  /**
   * Get the driver clause associated with an in relation.
   * 
   * @param metaRelation The {@link com.stratio.meta.common.statements.structures.relationships.RelationIn} clause.
   * @return A {@link com.datastax.driver.core.querybuilder.Clause}.
   */
  private Clause getRelationInClause(Relation metaRelation) {
    Clause clause = null;
    RelationIn relIn = (RelationIn) metaRelation;
    List<Term<?>> terms = relIn.getTerms();
    String field = relIn.getIdentifiers().get(0).getField();
    Object[] values = new Object[relIn.numberOfTerms()];
    int nTerm = 0;
    for (Term<?> term : terms) {
      values[nTerm] = getWhereCastValue(field, term.getTermValue());
      nTerm++;
    }
    clause = QueryBuilder.in(relIn.getIdentifiers().get(0).toString(), values);
    return clause;
  }

  /**
   * Get the driver clause associated with an token relation.
   * 
   * @param metaRelation The {@link com.stratio.meta.common.statements.structures.relationships.RelationToken} clause.
   * @return A {@link com.datastax.driver.core.querybuilder.Clause}.
   */
  private Clause getRelationTokenClause(Relation metaRelation) {
    Clause clause = null;
    RelationToken relToken = (RelationToken) metaRelation;

    List<String> names = new ArrayList<>();
    for (SelectorIdentifier identifier : relToken.getIdentifiers()) {
      names.add(identifier.toString());
    }

    if (!relToken.isRightSideTokenType()) {
      Object value = relToken.getTerms().get(0).getTermValue();
      switch (relToken.getOperator()) {
        case "=":
          clause =
              QueryBuilder.eq(QueryBuilder.token(names.toArray(new String[names.size()])), value);
          break;
        case ">":
          clause =
              QueryBuilder.gt(QueryBuilder.token(names.toArray(new String[names.size()])), value);
          break;
        case ">=":
          clause =
              QueryBuilder.gte(QueryBuilder.token(names.toArray(new String[names.size()])), value);
          break;
        case "<":
          clause =
              QueryBuilder.lt(QueryBuilder.token(names.toArray(new String[names.size()])), value);
          break;
        case "<=":
          clause =
              QueryBuilder.lte(QueryBuilder.token(names.toArray(new String[names.size()])), value);
          break;
        default:
          LOG.error("Unsupported operator " + relToken.getOperator());
          break;
      }
    } else {
      return null;
    }
    return clause;
  }

  /**
   * Get the driver where clause.
   * 
   * @param sel The current Select.
   * @return A {@link com.datastax.driver.core.querybuilder.Select.Where}.
   */
  private Where getDriverWhere(Select sel) {
    Where whereStmt = null;
    String[] luceneWhere = getLuceneWhereClause(metadata, tableMetadataFrom);
    if (luceneWhere != null) {
      Clause lc = QueryBuilder.eq(luceneWhere[0], luceneWhere[1]);
      whereStmt = sel.where(lc);
    }
    for (Relation metaRelation : this.where) {
      Clause clause = null;
      switch (metaRelation.getType()) {
        case Relation.TYPE_COMPARE:
          clause = getRelationCompareClause(metaRelation);
          break;
        case Relation.TYPE_IN:
          clause = getRelationInClause(metaRelation);
          break;
        case Relation.TYPE_TOKEN:
          clause = getRelationTokenClause(metaRelation);
          break;
        default:
          LOG.error("Unsupported relation type: " + metaRelation.getType());
          break;
      }
      if (clause != null) {
        if (whereStmt == null) {
          whereStmt = sel.where(clause);
        } else {
          whereStmt = whereStmt.and(clause);
        }
      }
    }
    return whereStmt;
  }

  @Override
  public Statement getDriverStatement() {
    Select.Builder builder = getDriverBuilder();

    Select sel;
    if (this.catalogInc) {
      sel = builder.from(this.catalog, this.tableName);
    } else {
      sel = builder.from(this.tableName);
    }

    if (this.limitInc) {
      sel.limit(this.limit);
    }

    if (this.orderInc) {
      com.datastax.driver.core.querybuilder.Ordering[] orderings =
          new com.datastax.driver.core.querybuilder.Ordering[order.size()];
      int nOrdering = 0;
      for (Ordering metaOrdering : this.order) {
        if (metaOrdering.isDirInc() && (metaOrdering.getOrderDir() == OrderDirection.DESC)) {
          orderings[nOrdering] = QueryBuilder.desc(metaOrdering.getSelectorIdentifier().toString());
        } else {
          orderings[nOrdering] = QueryBuilder.asc(metaOrdering.getSelectorIdentifier().toString());
        }
        nOrdering++;
      }
      sel.orderBy(orderings);
    }

    Where whereStmt = null;
    if (this.whereInc) {
      whereStmt = getDriverWhere(sel);
    } else {
      whereStmt = sel.where();
    }
    LOG.trace("Executing: " + whereStmt.toString());

    return whereStmt;
  }

  public void addTablenameToIds() {
    selectionClause.addTablename(tableName);
  }

  private void replaceAliasesInSelect(Map<String, String> tablesAliasesMap) {

    if (this.selectionClause instanceof SelectionList
        && ((SelectionList) this.selectionClause).getSelection() instanceof SelectionSelectors) {
      List<SelectionSelector> selectors =
          ((SelectionSelectors) ((SelectionList) this.selectionClause).getSelection())
              .getSelectors();

      for (SelectionSelector selector : selectors) {
        SelectorIdentifier identifier = null;
        if (selector.getSelector() instanceof SelectorIdentifier) {
          identifier = (SelectorIdentifier) selector.getSelector();
        } else if (selector.getSelector() instanceof SelectorGroupBy) {
          identifier = (SelectorIdentifier) ((SelectorGroupBy) selector.getSelector()).getParam();
        }

        if (identifier != null) {
          String table = tablesAliasesMap.get(identifier.getTable());
          if (table != null) {
            identifier.setTable(table);
          }
        }
      }
    }
  }

  private void replaceAliasesInWhere(Map<String, String> fieldsAliasesMap,
      Map<String, String> tablesAliasesMap) {

    if (this.where != null) {
      for (Relation whereCol : this.where) {
        for (SelectorIdentifier id : whereCol.getIdentifiers()) {
          String table = tablesAliasesMap.get(id.getTable());
          if (table != null) {
            id.setTable(table);
          }

          String identifier = fieldsAliasesMap.get(id.toString());
          if (identifier != null) {
            id.setIdentifier(identifier);
          }
        }
      }
    }
  }

  private void replaceAliasesInGroupBy(Map<String, String> fieldsAliasesMap,
      Map<String, String> tablesAliasesMap) {

    if (this.group != null) {
      for (GroupBy groupByCol : this.group) {
        SelectorIdentifier selectorIdentifier = groupByCol.getSelectorIdentifier();

        String table = tablesAliasesMap.get(selectorIdentifier.getTable());
        if (table != null) {
          selectorIdentifier.setTable(table);
        }

        String identifier = fieldsAliasesMap.get(selectorIdentifier.toString());
        if (identifier != null) {
          selectorIdentifier.setIdentifier(identifier);
        }
      }
    }
  }

  private void replaceAliasesInOrderBy(Map<String, String> fieldsAliasesMap,
      Map<String, String> tablesAliasesMap) {

    if (this.order != null) {
      for (Ordering orderBycol : this.order) {
        SelectorIdentifier selectorIdentifier = orderBycol.getSelectorIdentifier();

        String table = tablesAliasesMap.get(selectorIdentifier.getTable());
        if (table != null) {
          selectorIdentifier.setTable(table);
        }

        String identifier = fieldsAliasesMap.get(selectorIdentifier.toString());
        if (identifier != null) {
          selectorIdentifier.setIdentifier(identifier);
        }
      }
    }
  }

  private void replaceAliasesInJoin(Map<String, String> tablesAliasesMap) {

    if (this.join != null) {
      String leftTable = this.join.getLeftField().getTable();
      String tableName = tablesAliasesMap.get(leftTable);
      if (tableName != null) {
        this.join.getLeftField().setTable(tableName);
      }

      String rightTable = this.join.getRightField().getTable();
      tableName = tablesAliasesMap.get(rightTable);
      if (tableName != null) {
        this.join.getRightField().setTable(tableName);
      }
    }
  }

  public void replaceAliasesWithName(Map<String, String> fieldsAliasesMap,
      Map<String, String> tablesAliasesMap) {

    Iterator<Entry<String, String>> entriesIt = tablesAliasesMap.entrySet().iterator();
    while (entriesIt.hasNext()) {
      Entry<String, String> entry = entriesIt.next();
      if (entry.getValue().contains(".")) {
        tablesAliasesMap.put(entry.getKey(), entry.getValue().split("\\.")[1]);
      }
    }

    this.setFieldsAliasesMap(fieldsAliasesMap);

    // Replacing alias in SELECT clause
    replaceAliasesInSelect(tablesAliasesMap);

    // Replacing alias in WHERE clause
    replaceAliasesInWhere(fieldsAliasesMap, tablesAliasesMap);

    // Replacing alias in GROUP BY clause
    replaceAliasesInGroupBy(fieldsAliasesMap, tablesAliasesMap);

    // Replacing alias in ORDER BY clause
    replaceAliasesInOrderBy(fieldsAliasesMap, tablesAliasesMap);

    // Replacing alias in JOIN clause
    replaceAliasesInJoin(tablesAliasesMap);

  }

  public void updateTableNames() {

    // Adding table name to the identifiers in WHERE clause
    if (this.where != null) {
      for (Relation whereCol : this.where) {
        for (SelectorIdentifier identifier : whereCol.getIdentifiers()) {
          identifier.addTablename(this.tableName);
        }
      }
    }

    // Adding table name to the identifiers in GROUP BY clause
    if (this.group != null) {
      for (GroupBy groupByCol : this.group) {
        groupByCol.getSelectorIdentifier().addTablename(this.tableName);
      }
    }

    // Adding table name to the identifiers in ORDER BY clause
    if (this.order != null) {
      for (Ordering orderByCol : this.order) {
        orderByCol.getSelectorIdentifier().addTablename(this.tableName);
      }
    }
  }

}