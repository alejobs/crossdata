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

package com.stratio.crossdata.core.query;

import com.stratio.crossdata.common.data.CatalogName;
import com.stratio.crossdata.common.result.QueryStatus;
import com.stratio.crossdata.core.statements.CrossdataStatement;

/**
 * Interface IParsedQuery.
 */
public interface IParsedQuery {

    /**
     * Get the query to parse.
     *
     * @return String
     */
    String getQuery();

    /**
     * Get the queryId of a query.
     *
     * @return Query identification.
     */
    String getQueryId();

    /**
     * Get the status of a query.
     *
     * @return String
     */
    QueryStatus getStatus();

    /**
     * Get the Catalog of a query-
     *
     * @return com.stratio.crossdata.common.data.CatalogName
     */
    CatalogName getDefaultCatalog();

    /**
     * Get the statement of a query.
     *
     * @return com.stratio.crossdata.core.statements.CrossdataStatement
     */
    CrossdataStatement getStatement();

}
