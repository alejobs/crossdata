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

import java.util.Map;

import com.stratio.crossdata.common.data.CatalogName;
import com.stratio.crossdata.common.statements.structures.Selector;
import com.stratio.crossdata.common.utils.StringUtils;
import com.stratio.crossdata.core.validator.requirements.ValidationRequirements;
import com.stratio.crossdata.core.validator.requirements.ValidationTypes;

/**
 * Class that models an {@code ALTER CATALOG} statement from the CROSSDATA language.
 */
public class AlterCatalogStatement extends MetadataStatement {

    /**
     * A JSON with the options specified by the user.
     */
    private Map<Selector, Selector> options;

    /**
     * Class constructor.
     *
     * @param catalogName The name of the catalog.
     * @param options     A JSON with the storage options.
     */
    public AlterCatalogStatement(CatalogName catalogName, String options) {
        this.command = false;
        this.catalog = catalogName;
        this.catalogInc = true;
        this.options = StringUtils.convertJsonToOptions(options);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ALTER CATALOG ");
        sb.append(catalog);
        sb.append(" WITH ").append(this.options);
        return sb.toString();
    }

    @Override
    public ValidationRequirements getValidationRequirements() {
        return new ValidationRequirements().add(ValidationTypes.MUST_EXIST_CATALOG)
                .add(ValidationTypes.MUST_EXIST_PROPERTIES);
    }

    public Map<Selector, Selector> getOptions() {
        return options;
    }

    public CatalogName getCatalogName() {
        return catalog;
    }
}
