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

import com.stratio.crossdata.core.validator.requirements.ValidationRequirements;
import com.stratio.crossdata.core.validator.requirements.ValidationTypes;

/**
 * Class that models a {@code DROP CLUSTER} statement from the CROSSDATA language. In order to remove
 * an active cluster from the system, the user is required to delete first the existing tables.
 */
public class DetachClusterStatement extends MetadataStatement {

    /**
     * CLUSTER name given by the user.
     */
    private final String clusterName;

    /**
     * Default constructor.
     *
     * @param clusterName The name of the cluster to be removed.
     */
    public DetachClusterStatement(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DETACH CLUSTER ");
        sb.append(clusterName);
        return sb.toString();
    }

    @Override
    public ValidationRequirements getValidationRequirements() {
        return new ValidationRequirements().add(ValidationTypes.MUST_EXIST_CLUSTER);
    }

    public String getClusterName() {
        return clusterName;
    }
}
