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

import static org.testng.Assert.fail;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import com.stratio.crossdata.common.data.ClusterName;
import com.stratio.crossdata.common.exceptions.PlanningException;
import com.stratio.crossdata.common.logicalplan.LogicalWorkflow;
import com.stratio.crossdata.common.logicalplan.Project;
import com.stratio.crossdata.common.metadata.ColumnType;
import com.stratio.crossdata.common.metadata.Operations;
import com.stratio.crossdata.common.metadata.TableMetadata;
import com.stratio.crossdata.common.statements.structures.window.TimeUnit;
import com.stratio.crossdata.common.statements.structures.window.WindowType;

/**
 * Planner test concerning Logical workflow creation.
 */
public class PlannerLogicalWorkflowTest extends PlannerBaseTest {

    /**
     * Class logger.
     */
    private static final Logger LOG = Logger.getLogger(PlannerLogicalWorkflowTest.class);

    @Test
    public void selectSingleColumn() {
        String inputText = "SELECT demo.t1.a FROM demo.t1;";
        String[] expectedColumns = { "demo.t1.a" };

        String[] columnsT1 = { "a" };
        ColumnType[] columnTypes1 = { ColumnType.INT };
        String[] partitionKeys1 = { "a" };
        String[] clusteringKeys1 = { };
        TableMetadata t1 = defineTable(new ClusterName("c"), "demo", "t1", columnsT1, columnTypes1,
                partitionKeys1, clusteringKeys1);

        LogicalWorkflow workflow = null;
        try {
            workflow = getWorkflow(inputText, "selectSingleColumn", t1);
        } catch (PlanningException e) {
            fail("LogicalWorkflow couldn't be calculated");
        }
        assertNumberInitialSteps(workflow, 1);
        assertColumnsInProject(workflow, "demo.t1", expectedColumns);
        assertSelect(workflow);
    }

    @Test
    public void selectMultipleColumns() {
        String inputText = "SELECT demo.t1.a, demo.t1.b, demo.t1.c FROM demo.t1;";
        String[] expectedColumns = { "demo.t1.a", "demo.t1.b", "demo.t1.c" };

        String[] columnsT1 = { "a", "b", "c" };
        ColumnType[] columnTypes1 = { ColumnType.INT, ColumnType.INT, ColumnType.INT };
        String[] partitionKeys1 = { "a" };
        String[] clusteringKeys1 = { };
        TableMetadata t1 = defineTable(new ClusterName("c"), "demo", "t1", columnsT1, columnTypes1,
                partitionKeys1, clusteringKeys1);

        LogicalWorkflow workflow = null;
        try {
            workflow = getWorkflow(inputText, "selectSingleColumn", t1);
        } catch (PlanningException e) {
            fail("LogicalWorkflow couldn't be calculated");
        }
        assertColumnsInProject(workflow, "demo.t1", expectedColumns);
        assertSelect(workflow);
    }

    @Test
    public void selectMultipleColumnsWithAlias() {
        String inputText = "SELECT demo.t1.a AS a, demo.t1.b AS b, demo.t1.c AS c FROM demo.t1;";
        String[] expectedColumns = { "demo.t1.a", "demo.t1.b", "demo.t1.c" };

        String[] columnsT1 = { "a", "b", "c" };
        ColumnType[] columnTypes1 = { ColumnType.INT, ColumnType.INT, ColumnType.INT };
        String[] partitionKeys1 = { "a" };
        String[] clusteringKeys1 = { };
        TableMetadata t1 = defineTable(new ClusterName("c"), "demo", "t1", columnsT1, columnTypes1,
                partitionKeys1, clusteringKeys1);

        LogicalWorkflow workflow = null;
        try {
            workflow = getWorkflow(inputText, "selectSingleColumn", t1);
        } catch (PlanningException e) {
            fail("LogicalWorkflow couldn't be calculated");
        }
        assertColumnsInProject(workflow, "demo.t1", expectedColumns);
        assertSelect(workflow);
    }

    @Test
    public void selectJoinMultipleColumns() {
        //TODO update on clause when fullyqualifed names are supported in the JOIN.
        String inputText = "SELECT demo.t1.a, demo.t1.b, demo.t2.c, demo.t2.d FROM demo.t1" +
                " INNER JOIN demo.t2 ON demo.t1.aa = demo.t2.aa;";

        String[] columnsT1 = { "a", "b", "aa" };
        ColumnType[] columnTypes1 = { ColumnType.INT, ColumnType.INT, ColumnType.TEXT };
        String[] partitionKeys1 = { "a" };
        String[] clusteringKeys1 = { };
        TableMetadata t1 = defineTable(new ClusterName("c"), "demo", "t1", columnsT1, columnTypes1,
                partitionKeys1, clusteringKeys1);

        String[] columnsT2 = { "c", "d", "aa" };
        ColumnType[] columnTypes2 = { ColumnType.INT, ColumnType.INT, ColumnType.TEXT };
        String[] partitionKeys2 = { "c" };
        String[] clusteringKeys2 = { };
        TableMetadata t2 = defineTable(new ClusterName("c"), "demo", "t2", columnsT2, columnTypes2,
                partitionKeys2, clusteringKeys2);

        String[] expectedColumnsT1 = { "demo.t1.a", "demo.t1.b", "demo.t1.aa" };
        String[] expectedColumnsT2 = { "demo.t2.c", "demo.t2.d" };

        LogicalWorkflow workflow = null;
        try {
            workflow = getWorkflow(inputText, "selectSingleColumn", t1, t2);
        } catch (PlanningException e) {
            fail("LogicalWorkflow couldn't be calculated");
        }
        assertNumberInitialSteps(workflow, 2);
        assertColumnsInProject(workflow, "demo.t1", expectedColumnsT1);
        assertColumnsInProject(workflow, "demo.t2", expectedColumnsT2);

        assertJoin(workflow, "demo.t1", "demo.t2", "demo.t1.aa = demo.t2.aa");
        assertSelect(workflow);
    }

    @Test
    public void selectJoinMultipleColumnsWithWhere() {
        //TODO update on clause when fullyqualifed names are supported in the JOIN.
        String inputText = "SELECT demo.t1.a, demo.t1.b, demo.t2.c, demo.t2.d FROM demo.t1 INNER JOIN demo.t2" +
                " ON demo.t1.aa = demo.t2.aa WHERE demo.t1.b > 10 AND demo.t2.d < 10;";

        String[] columnsT1 = { "a", "b", "aa" };
        ColumnType[] columnTypes1 = { ColumnType.INT, ColumnType.INT, ColumnType.TEXT };
        String[] partitionKeys1 = { "a" };
        String[] clusteringKeys1 = { };
        TableMetadata t1 = defineTable(new ClusterName("c"), "demo", "t1", columnsT1, columnTypes1,
                partitionKeys1, clusteringKeys1);

        String[] columnsT2 = { "c", "d", "aa" };
        ColumnType[] columnTypes2 = { ColumnType.INT, ColumnType.INT, ColumnType.TEXT };
        String[] partitionKeys2 = { "c" };
        String[] clusteringKeys2 = { };
        TableMetadata t2 = defineTable(new ClusterName("c"), "demo", "t2", columnsT2, columnTypes2,
                partitionKeys2, clusteringKeys2);

        String[] expectedColumnsT1 = { "demo.t1.a", "demo.t1.b", "demo.t1.aa" };
        String[] expectedColumnsT2 = { "demo.t2.c", "demo.t2.d" };

        LogicalWorkflow workflow = null;
        try {
            workflow = getWorkflow(inputText, "selectSingleColumn", t1, t2);
        } catch (PlanningException e) {
            fail("LogicalWorkflow couldn't be calculated");
        }
        assertNumberInitialSteps(workflow, 2);
        Project project1 = assertColumnsInProject(workflow, "demo.t1", expectedColumnsT1);
        Project project2 = assertColumnsInProject(workflow, "demo.t2", expectedColumnsT2);
        assertJoin(workflow, "demo.t1", "demo.t2", "demo.t1.aa = demo.t2.aa");
        assertFilterInPath(project1, Operations.FILTER_NON_INDEXED_GT);
        assertFilterInPath(project2, Operations.FILTER_NON_INDEXED_LT);

        assertSelect(workflow);

    }

    @Test
    public void selectBasicWhere() {
        String inputText = "SELECT demo.t1.a, demo.t1.b, demo.t1.c FROM demo.t1 WHERE demo.t1.a = 3;";
        String[] columns1 = { "a", "b", "c" };
        ColumnType[] columnTypes1 = { ColumnType.INT, ColumnType.INT, ColumnType.INT };
        String[] partitionKeys1 = { "a" };
        String[] clusteringKeys1 = { };
        TableMetadata t1 = defineTable(new ClusterName("c"), "demo", "t1", columns1, columnTypes1,
                partitionKeys1, clusteringKeys1);

        String[] expectedColumns = { "demo.t1.a", "demo.t1.b", "demo.t1.c" };
        LogicalWorkflow workflow = null;
        try {
            workflow = getWorkflow(inputText, "selectBasicWhere", t1);
        } catch (PlanningException e) {
            fail("LogicalWorkflow couldn't be calculated");
        }
        Project project1 = assertColumnsInProject(workflow, "demo.t1", expectedColumns);
        assertFilterInPath(project1, Operations.FILTER_PK_EQ);
        assertSelect(workflow);
    }

    @Test
    public void selectBasicWhereWindow() {
        String inputText = "SELECT demo.t1.a, demo.t1.b, demo.t1.c FROM demo.t1" +
                " WITH WINDOW 5 SECONDS WHERE demo.t1.a = 3;";

        String[] columns1 = { "a", "b", "c" };
        ColumnType[] columnTypes1 = { ColumnType.INT, ColumnType.INT, ColumnType.INT };
        String[] partitionKeys1 = { "a" };
        String[] clusteringKeys1 = { };
        TableMetadata t1 = defineTable(new ClusterName("c"), "demo", "t1", columns1, columnTypes1,
                partitionKeys1, clusteringKeys1);

        String[] expectedColumns = { "demo.t1.a", "demo.t1.b", "demo.t1.c" };
        LogicalWorkflow workflow = null;
        try {
            workflow = getWorkflow(inputText, "selectBasicWhere", t1);
        } catch (PlanningException e) {
            fail("LogicalWorkflow couldn't be calculated");
        }
        Project project1 = assertColumnsInProject(workflow, "demo.t1", expectedColumns);
        assertFilterInPath(project1, Operations.FILTER_PK_EQ);
        assertWindow(workflow, WindowType.TEMPORAL, -1, 5, TimeUnit.SECONDS);
        assertSelect(workflow);
    }

}
