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

package com.stratio.crossdata.core.coordinator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.testng.annotations.Test;

import com.stratio.crossdata.common.data.ClusterName;
import com.stratio.crossdata.common.data.ConnectorName;
import com.stratio.crossdata.common.data.DataStoreName;
import com.stratio.crossdata.common.executionplan.ExecutionType;
import com.stratio.crossdata.common.executionplan.ManagementWorkflow;
import com.stratio.crossdata.common.executionplan.ResultType;
import com.stratio.crossdata.common.metadata.ClusterAttachedMetadata;
import com.stratio.crossdata.common.metadata.DataStoreMetadata;
import com.stratio.crossdata.communication.ManagementOperation;
import com.stratio.crossdata.core.metadata.MetadataManager;
import com.stratio.crossdata.core.metadata.MetadataManagerTestHelper;

public class CoordinatorTest extends MetadataManagerTestHelper {

    /**
     * Testing an API operation (attaching a cluster to a datastore)
     *
     * @throws Exception
     */
    @Test
    public void testAttachCluster() throws Exception {

        // Create and add a test datastore metadata to the metadatamanager
        DataStoreMetadata datastoreTest = insertDataStore("datastoreTest", "production");

        ManagementWorkflow workflow = new ManagementWorkflow("", null, ExecutionType.ATTACH_CLUSTER,
                ResultType.RESULTS);
        workflow.setClusterName(new ClusterName("clusterTest"));
        workflow.setConnectorName(new ConnectorName("myConnector"));
        workflow.setDatastoreName(new DataStoreName("dataStoreTest"));
        Coordinator coordinator = new Coordinator();
        ManagementOperation op = workflow.createManagementOperationMessage();
        coordinator.executeManagementOperation(op);
        // Check that changes persisted in the MetadataManager ("datastoreTest" datastore)
        datastoreTest = MetadataManager.MANAGER.getDataStore(new DataStoreName("dataStoreTest"));
        Map<ClusterName, ClusterAttachedMetadata> clusterAttachedRefsTest =
                datastoreTest.getClusterAttachedRefs();
        boolean found = false;
        for (ClusterName clusterNameTest : clusterAttachedRefsTest.keySet()) {
            ClusterAttachedMetadata clusterAttachedMetadata =
                    clusterAttachedRefsTest.get(clusterNameTest);
            if (clusterAttachedMetadata.getClusterRef().equals(new ClusterName("clusterTest"))) {
                assertEquals(clusterAttachedMetadata.getDataStoreRef(), new DataStoreName("datastoreTest"),
                        "Wrong attachment for clusterTest");
                found = true;
                break;
            }
        }
        assertTrue(found, "Attachment not found");
    }

    /**
     * Testing an API operation (detaching a cluster to a datastore)
     *
     * @throws Exception
     */
    @Test
    public void testDetachCluster() throws Exception {

        // Create and add a test datastore metadata to the metadatamanager
        DataStoreMetadata datastoreTest = insertDataStore("datastoreTest", "production");

        ManagementWorkflow workflow = new ManagementWorkflow("", null, ExecutionType.DETACH_CLUSTER,
                ResultType.RESULTS);
        workflow.setClusterName(new ClusterName("clusterTest"));
        workflow.setConnectorName(new ConnectorName("myConnector"));
        workflow.setDatastoreName(new DataStoreName("dataStoreTest"));
        Coordinator coordinator = new Coordinator();
        ManagementOperation op = workflow.createManagementOperationMessage();
        coordinator.executeManagementOperation(op);
        // Check that changes persisted in the MetadataManager ("datastoreTest" datastore)
        datastoreTest = MetadataManager.MANAGER.getDataStore(new DataStoreName("dataStoreTest"));
        Map<ClusterName, ClusterAttachedMetadata> clusterAttachedRefsTest =
                datastoreTest.getClusterAttachedRefs();
        boolean found = false;
        for (ClusterName clusterNameTest : clusterAttachedRefsTest.keySet()) {
            ClusterAttachedMetadata clusterAttachedMetadata =
                    clusterAttachedRefsTest.get(clusterNameTest);
            if (clusterAttachedMetadata.getClusterRef().equals(new ClusterName("clusterTest"))) {
                assertEquals(clusterAttachedMetadata.getDataStoreRef(), new DataStoreName("datastoreTest"),
                        "Wrong attachment for clusterTest");
                found = true;
                break;
            }
        }
        assertFalse(found);
    }

    /*@Test
    public void testAttachConnector() throws Exception {

        // Create and add a test datastore metadata to the metadatamanager
        DataStoreMetadata datastoreTest = insertDataStore("datastoreTest", "preproduction");

        // Create and add a test cluster metadata to the metadatamanager
        ClusterName clusterName = new ClusterName("clusterTest");
        DataStoreName dataStoreRef = new DataStoreName("dataStoreTest");
        Map<Selector, Selector> options = new HashMap<>();
        Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefs = new HashMap<>();
        ClusterMetadata clusterTest =
                new ClusterMetadata(clusterName, dataStoreRef, options, connectorAttachedRefs);
        MetadataManager.MANAGER.createClusterAndAttach(clusterTest, false);

        // Create and add a test connector metadata to the metadatamanager
        ConnectorName connectorName = new ConnectorName("connectorTest");
        String connectorVersion = "0.2.0";
        Set<DataStoreName> dataStoreRefs = new HashSet<>();
        com.stratio.com.stratio.crossdata.common.api.generated.connectorOld.RequiredPropertiesType connectorRequiredProperties =
                null;
        com.stratio.com.stratio.crossdata.common.api.generated.connectorOld.OptionalPropertiesType connectorOptionalProperties =
                null;
        SupportedOperationsType supportedOperations = null;
        ConnectorMetadata connectorTest =
                new ConnectorMetadata(connectorName, connectorVersion, dataStoreRefs,
                        connectorRequiredProperties, connectorOptionalProperties, supportedOperations);
        MetadataManager.MANAGER.createConnector(connectorTest, false);

        // Add information about the connector attachment to the metadatamanager
        BaseQuery baseQuery =
                new BaseQuery(UUID.randomUUID().toString(),
                        "ATTACH CONNECTOR cassandra_connector TO cassandra_prod WITH OPTIONS {}",
                        new CatalogName("test"));

        ConnectorName connectorRef = new ConnectorName("connectorTest");
        ClusterName clusterRef = new ClusterName("clusterTest");
        Map<Selector, Selector> properties = new HashMap<>();
        ConnectorAttachedMetadata connectorAttachedMetadata =
                new ConnectorAttachedMetadata(connectorRef, clusterRef, properties);

        AttachConnectorStatement attachConnectorStatement =
                new AttachConnectorStatement(new ConnectorName("connectorTest"), new ClusterName("clusterTest"), "{}");

        MetadataParsedQuery metadataParsedQuery =
                new MetadataParsedQuery(baseQuery, attachConnectorStatement);

        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);

        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);

        Coordinator coordinator = new Coordinator();
        coordinator.coordinate(plannedQuery);

        // Check that changes persisted in the MetadataManager ("clusterTest" cluster)
        clusterTest = MetadataManager.MANAGER.getCluster(new ClusterName("clusterTest"));

        Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefsTest =
                clusterTest.getConnectorAttachedRefs();

        boolean found = false;

        for (ConnectorName connectorNameTest : connectorAttachedRefsTest.keySet()) {
            ConnectorAttachedMetadata connectorAttachedMetadataTest =
                    connectorAttachedRefsTest.get(connectorNameTest);
            if (connectorAttachedMetadataTest.getClusterRef().equals(new ClusterName("clusterTest"))) {
                assertEquals(connectorAttachedMetadata.getClusterRef(), new ClusterName("clusterTest"),
                        "Wrong attachment for connectorTest");
                found = true;
                break;
            }
        }
        assertTrue(found, "Attachment not found");
    }

    // CREATE CATALOG
    @Test
    public void testCreateCatalogCheckName() throws Exception {
        CatalogName catalogName = new CatalogName("testCatalog");

        BaseQuery baseQuery =
                new BaseQuery(UUID.randomUUID().toString(), "CREATE CATALOG testCatalog", catalogName);

        CreateCatalogStatement createCatalogStatement =
                new CreateCatalogStatement(catalogName, false, "{}");

        MetadataParsedQuery metadataParsedQuery =
                new MetadataParsedQuery(baseQuery, createCatalogStatement);

        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);

        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);

        Coordinator coordinator = new Coordinator();
        if (coordinator.coordinate(plannedQuery) instanceof MetadataInProgressQuery) {
            assertTrue(true);
            coordinator.persist(plannedQuery);
        } else {
            fail("Coordinator.coordinate not creating new MetadataInProgressQuery");
        }

        assertEquals(MetadataManager.MANAGER.getCatalog(catalogName).getName(), catalogName);
    }

    @Test
    public void testCreateCatalogCheckNameNotEquals() throws Exception {
        CatalogName catalogName = new CatalogName("testCatalog2");

        BaseQuery baseQuery =
                new BaseQuery(UUID.randomUUID().toString(), "CREATE CATALOG testCatalog2", catalogName);

        CreateCatalogStatement createCatalogStatement =
                new CreateCatalogStatement(catalogName, false, "{}");

        MetadataParsedQuery metadataParsedQuery =
                new MetadataParsedQuery(baseQuery, createCatalogStatement);

        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);

        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);

        Coordinator coordinator = new Coordinator();
        if (coordinator.coordinate(plannedQuery) instanceof MetadataInProgressQuery) {
            assertTrue(true);
            coordinator.persist(plannedQuery);
        } else {
            fail("Coordinator.coordinate not creating new MetadataInProgressQuery");
        }

        assertNotEquals(MetadataManager.MANAGER.getCatalog(catalogName).getName(), new CatalogName(
                "notSameName"));
    }

    // CREATE TABLE
    @Test
    public void testCreateTable() throws Exception {

        String datastore = "datastoreTest";
        String cluster = "clusterTest";
        String catalog = "catalogTest4";
        String tableString = "tableTest";

        createTestDatastoreAndPersist(datastore, "0.2.0");
        createTestClusterAndPersist(cluster, datastore);
        createTestsCatalogAndPersist(catalog);

        // Create and add test table to the metadatamanager
        TableName tableName = new TableName(catalog, tableString);
        TableMetadata table =
                new TableMetadata(tableName, new HashMap<Selector, Selector>(),
                        new HashMap<ColumnName, ColumnMetadata>(), new HashMap<IndexName, IndexMetadata>(),
                        new ClusterName(cluster), new ArrayList<ColumnName>(), new ArrayList<ColumnName>());

        BaseQuery baseQuery =
                new BaseQuery(UUID.randomUUID().toString(), "CREATE TABLE testTable", new CatalogName(
                        catalog));

        Map<ColumnName, ColumnType> columns = new HashMap<ColumnName, ColumnType>();
        for (Entry<ColumnName, ColumnMetadata> c : table.getColumns().entrySet()) {
            columns.put(c.getKey(), c.getValue().getColumnType());
        }

        CreateTableStatement createTableStatement =
                new CreateTableStatement(tableName, table.getClusterRef(), columns, table.getPrimaryKey(),
                        table.getPartitionKey());
        createTableStatement.setTableMetadata(table);

        MetadataParsedQuery metadataParsedQuery =
                new MetadataParsedQuery(baseQuery, createTableStatement);

        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);

        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);

        Coordinator coordinator = new Coordinator();
        if (coordinator.coordinate(plannedQuery) instanceof MetadataInProgressQuery) {
            assertTrue(true);
            coordinator.persist(plannedQuery);
        } else {
            fail("Coordinator.coordinate not creating new MetadataInProgressQuery");
        }

        assertEquals(MetadataManager.MANAGER.getTable(tableName), table);
    }

    //CREATE INDEX
    @Test
    public void testCreateIndex() throws Exception {

        String datastore = "datastoreTest";
        String cluster = "clusterTest";
        String catalog = "catalogTest5";
        String table = "tableTest";

        createTestDatastoreAndPersist(datastore, "0.2.0");
        createTestClusterAndPersist(cluster, datastore);
        createTestsCatalogAndPersist(catalog);
        createTestTableAndPersist(cluster, catalog, table);

        BaseQuery baseQuery =
                new BaseQuery(UUID.randomUUID().toString(), "CREATE INDEX testIndex ON testTable",
                        new CatalogName(catalog));

        CreateIndexStatement createIndexStatement = new CreateIndexStatement();
        createIndexStatement.setTableName(new TableName("catalogTest5", "tableTest"));
        new MetadataParsedQuery(baseQuery, createIndexStatement);

        MetadataParsedQuery metadataParsedQuery =
                new MetadataParsedQuery(baseQuery, createIndexStatement);

        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);

        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);

        Coordinator coordinator = new Coordinator();
        if (coordinator.coordinate(plannedQuery) instanceof MetadataInProgressQuery) {
            assertTrue(true);
            coordinator.persist(plannedQuery);
        } else {
            fail("Coordinator.coordinate not creating new MetadataInProgressQuery");
        }
    }

    //INSERT INTO
    @Test
    public void testInsertInto() throws Exception {
        String catalog = "catalogTest6";
        String table = "tableTest";

        BaseQuery baseQuery =
                new BaseQuery(UUID.randomUUID().toString(),
                        "INSERT INTO catalogTest6.tableTest ('col1', 'col2') VALUES (1, 2)",
                        new CatalogName(catalog));

        InsertIntoStatement insert = new InsertIntoStatement(new TableName(catalog, table), new ArrayList<ColumnName>(),
                new SelectStatement(new TableName(catalog, table)), false);

        StorageParsedQuery metadataParsedQuery =
                new StorageParsedQuery(baseQuery, insert);

        StorageValidatedQuery metadataValidatedQuery = new StorageValidatedQuery(metadataParsedQuery);

        StoragePlannedQuery plannedQuery = new StoragePlannedQuery(metadataValidatedQuery, null);

        Coordinator coordinator = new Coordinator();
        if (coordinator.coordinate(plannedQuery) instanceof StorageInProgressQuery) {
            assertTrue(true);
            coordinator.persist(plannedQuery);
        } else {
            fail("Coordinator.coordinate not creating new StorageInProgressQuery");
        }
    }

    private void createTestDatastoreAndPersist(String name, String version) {
        // Create and add a test datastore metadata to the metadatamanager
        DataStoreName dataStoreName = new DataStoreName(name);
        String dataStoreVersion = version;
        RequiredPropertiesType requiredProperties = null;
        OptionalPropertiesType othersProperties = null;
        DataStoreMetadata datastoreTest =
                new DataStoreMetadata(dataStoreName, dataStoreVersion, requiredProperties, othersProperties);
        MetadataManager.MANAGER.createDataStore(datastoreTest, false);
    }

    private void createTestClusterAndPersist(String cluster, String datastore) {
        // Create and add a test cluster metadata to the metadatamanager
        ClusterName clusterName = new ClusterName(cluster);
        DataStoreName dataStoreRef = new DataStoreName(datastore);
        Map<Selector, Selector> options = new HashMap<>();
        Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefs = new HashMap<>();
        ClusterMetadata clusterTest =
                new ClusterMetadata(clusterName, dataStoreRef, options, connectorAttachedRefs);
        MetadataManager.MANAGER.createClusterAndAttach(clusterTest, false);
    }

    private void createTestsCatalogAndPersist(String catalog) {
        // Create and add test catalog to the metadatamanager
        CatalogName catalogName = new CatalogName(catalog);
        CatalogMetadata catalogMetadata =
                new CatalogMetadata(catalogName, new HashMap<Selector, Selector>(),
                        new HashMap<TableName, TableMetadata>());
        MetadataManager.MANAGER.createCatalog(catalogMetadata);
    }

    private void createTestTableAndPersist(String clusterName, String catalogName,
            String tableNameString) {
        // Create and add test table to the metadatamanager
        TableName tableName = new TableName(catalogName, tableNameString);
        TableMetadata table =
                new TableMetadata(tableName, new HashMap<Selector, Selector>(),
                        new HashMap<ColumnName, ColumnMetadata>(), new HashMap<IndexName, IndexMetadata>(),
                        new ClusterName(clusterName), new ArrayList<ColumnName>(), new ArrayList<ColumnName>());

        Map<ColumnName, ColumnType> columns = new HashMap<ColumnName, ColumnType>();
        for (Entry<ColumnName, ColumnMetadata> c : table.getColumns().entrySet()) {
            columns.put(c.getKey(), c.getValue().getColumnType());
        }
        MetadataManager.MANAGER.createTable(table);
    }*/

    //    @Test
    //    public void testAttachConnector() throws Exception {
    //
    //        // Create and add a test datastore metadata to the metadatamanager
    //        DataStoreMetadata datastoreTest = insertDataStore("datastoreTest", "preproduction");
    //
    //        // Create and add a test cluster metadata to the metadatamanager
    //        ClusterName clusterName = new ClusterName("clusterTest");
    //        DataStoreName dataStoreRef = new DataStoreName("dataStoreTest");
    //        Map<Selector, Selector> options = new HashMap<>();
    //        Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefs = new HashMap<>();
    //        ClusterMetadata clusterTest =
    //                new ClusterMetadata(clusterName, dataStoreRef, options, connectorAttachedRefs);
    //        MetadataManager.MANAGER.createClusterAndAttach(clusterTest, false);
    //
    //        // Create and add a test connector metadata to the metadatamanager
    //        ConnectorName connectorName = new ConnectorName("connectorTest");
    //        String connectorVersion = "0.2.0";
    //        Set<DataStoreName> dataStoreRefs = new HashSet<>();
    //        com.stratio.com.stratio.crossdata.common.api.generated.connector.RequiredPropertiesType connectorRequiredProperties =
    //                null;
    //        com.stratio.com.stratio.crossdata.common.api.generated.connector.OptionalPropertiesType connectorOptionalProperties =
    //                null;
    //        SupportedOperationsType supportedOperations = null;
    //        ConnectorMetadata connectorTest =
    //                new ConnectorMetadata(connectorName, connectorVersion, dataStoreRefs,
    //                        connectorRequiredProperties, connectorOptionalProperties, supportedOperations);
    //        MetadataManager.MANAGER.createConnector(connectorTest, false);
    //
    //        // Add information about the connector attachment to the metadatamanager
    //        BaseQuery baseQuery =
    //                new BaseQuery(UUID.randomUUID().toString(),
    //                        "ATTACH CONNECTOR cassandra_connector TO cassandra_prod WITH OPTIONS {}",
    //                        new CatalogName("test"));
    //
    //        ConnectorName connectorRef = new ConnectorName("connectorTest");
    //        ClusterName clusterRef = new ClusterName("clusterTest");
    //        Map<Selector, Selector> properties = new HashMap<>();
    //        ConnectorAttachedMetadata connectorAttachedMetadata =
    //                new ConnectorAttachedMetadata(connectorRef, clusterRef, properties);
    //
    //        AttachConnectorStatement attachConnectorStatement =
    //                new AttachConnectorStatement(new ConnectorName("connectorTest"), new ClusterName("clusterTest"), "{}");
    //
    //        MetadataParsedQuery metadataParsedQuery =
    //                new MetadataParsedQuery(baseQuery, attachConnectorStatement);
    //
    //        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);
    //
    //        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);
    //
    //        Coordinator coordinator = new Coordinator();
    //        coordinator.coordinate(plannedQuery);
    //
    //        // Check that changes persisted in the MetadataManager ("clusterTest" cluster)
    //        clusterTest = MetadataManager.MANAGER.getCluster(new ClusterName("clusterTest"));
    //
    //        Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefsTest =
    //                clusterTest.getConnectorAttachedRefs();
    //
    //        boolean found = false;
    //
    //        for (ConnectorName connectorNameTest : connectorAttachedRefsTest.keySet()) {
    //            ConnectorAttachedMetadata connectorAttachedMetadataTest =
    //                    connectorAttachedRefsTest.get(connectorNameTest);
    //            if (connectorAttachedMetadataTest.getClusterRef().equals(new ClusterName("clusterTest"))) {
    //                assertEquals(connectorAttachedMetadata.getClusterRef(), new ClusterName("clusterTest"),
    //                        "Wrong attachment for connectorTest");
    //                found = true;
    //                break;
    //            }
    //        }
    //        assertTrue(found, "Attachment not found");
    //    }
    //
    //    // CREATE CATALOG
    //    @Test
    //    public void testCreateCatalogCheckName() throws Exception {
    //        CatalogName catalogName = new CatalogName("testCatalog");
    //
    //        BaseQuery baseQuery =
    //                new BaseQuery(UUID.randomUUID().toString(), "CREATE CATALOG testCatalog", catalogName);
    //
    //        CreateCatalogStatement createCatalogStatement =
    //                new CreateCatalogStatement(catalogName, false, "{}");
    //
    //        MetadataParsedQuery metadataParsedQuery =
    //                new MetadataParsedQuery(baseQuery, createCatalogStatement);
    //
    //        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);
    //
    //        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);
    //
    //        Coordinator coordinator = new Coordinator();
    //        if (coordinator.coordinate(plannedQuery) instanceof MetadataInProgressQuery) {
    //            assertTrue(true);
    //            coordinator.persist(plannedQuery);
    //        } else {
    //            fail("Coordinator.coordinate not creating new MetadataInProgressQuery");
    //        }
    //
    //        assertEquals(MetadataManager.MANAGER.getCatalog(catalogName).getName(), catalogName);
    //    }
    //
    //    @Test
    //    public void testCreateCatalogCheckNameNotEquals() throws Exception {
    //        CatalogName catalogName = new CatalogName("testCatalog2");
    //
    //        BaseQuery baseQuery =
    //                new BaseQuery(UUID.randomUUID().toString(), "CREATE CATALOG testCatalog2", catalogName);
    //
    //        CreateCatalogStatement createCatalogStatement =
    //                new CreateCatalogStatement(catalogName, false, "{}");
    //
    //        MetadataParsedQuery metadataParsedQuery =
    //                new MetadataParsedQuery(baseQuery, createCatalogStatement);
    //
    //        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);
    //
    //        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);
    //
    //        Coordinator coordinator = new Coordinator();
    //        if (coordinator.coordinate(plannedQuery) instanceof MetadataInProgressQuery) {
    //            assertTrue(true);
    //            coordinator.persist(plannedQuery);
    //        } else {
    //            fail("Coordinator.coordinate not creating new MetadataInProgressQuery");
    //        }
    //
    //        assertNotEquals(MetadataManager.MANAGER.getCatalog(catalogName).getName(), new CatalogName(
    //                "notSameName"));
    //    }
    //
    //    // CREATE TABLE
    //    @Test
    //    public void testCreateTable() throws Exception {
    //
    //        String datastore = "datastoreTest";
    //        String cluster = "clusterTest";
    //        String catalog = "catalogTest4";
    //        String tableString = "tableTest";
    //
    //        createTestDatastoreAndPersist(datastore, "0.2.0");
    //        createTestClusterAndPersist(cluster, datastore);
    //        createTestsCatalogAndPersist(catalog);
    //
    //        // Create and add test table to the metadatamanager
    //        TableName tableName = new TableName(catalog, tableString);
    //        TableMetadata table =
    //                new TableMetadata(tableName, new HashMap<Selector, Selector>(),
    //                        new HashMap<ColumnName, ColumnMetadata>(), new HashMap<IndexName, IndexMetadata>(),
    //                        new ClusterName(cluster), new ArrayList<ColumnName>(), new ArrayList<ColumnName>());
    //
    //        BaseQuery baseQuery =
    //                new BaseQuery(UUID.randomUUID().toString(), "CREATE TABLE testTable", new CatalogName(
    //                        catalog));
    //
    //        Map<ColumnName, ColumnType> columns = new HashMap<ColumnName, ColumnType>();
    //        for (Entry<ColumnName, ColumnMetadata> c : table.getColumns().entrySet()) {
    //            columns.put(c.getKey(), c.getValue().getColumnType());
    //        }
    //
    //        CreateTableStatement createTableStatement =
    //                new CreateTableStatement(tableName, table.getClusterRef(), columns, table.getPrimaryKey(),
    //                        table.getPartitionKey());
    //        createTableStatement.setTableMetadata(table);
    //
    //        MetadataParsedQuery metadataParsedQuery =
    //                new MetadataParsedQuery(baseQuery, createTableStatement);
    //
    //        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);
    //
    //        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);
    //
    //        Coordinator coordinator = new Coordinator();
    //        if (coordinator.coordinate(plannedQuery) instanceof MetadataInProgressQuery) {
    //            assertTrue(true);
    //            coordinator.persist(plannedQuery);
    //        } else {
    //            fail("Coordinator.coordinate not creating new MetadataInProgressQuery");
    //        }
    //
    //        assertEquals(MetadataManager.MANAGER.getTable(tableName), table);
    //    }
    //
    //    //CREATE INDEX
    //    @Test
    //    public void testCreateIndex() throws Exception {
    //
    //        String datastore = "datastoreTest";
    //        String cluster = "clusterTest";
    //        String catalog = "catalogTest5";
    //        String table = "tableTest";
    //
    //        createTestDatastoreAndPersist(datastore, "0.2.0");
    //        createTestClusterAndPersist(cluster, datastore);
    //        createTestsCatalogAndPersist(catalog);
    //        createTestTableAndPersist(cluster, catalog, table);
    //
    //        BaseQuery baseQuery =
    //                new BaseQuery(UUID.randomUUID().toString(), "CREATE INDEX testIndex ON testTable",
    //                        new CatalogName(catalog));
    //
    //        CreateIndexStatement createIndexStatement = new CreateIndexStatement();
    //        createIndexStatement.setTableName(new TableName("catalogTest5", "tableTest"));
    //        new MetadataParsedQuery(baseQuery, createIndexStatement);
    //
    //        MetadataParsedQuery metadataParsedQuery =
    //                new MetadataParsedQuery(baseQuery, createIndexStatement);
    //
    //        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery(metadataParsedQuery);
    //
    //        MetadataPlannedQuery plannedQuery = new MetadataPlannedQuery(metadataValidatedQuery, null);
    //
    //        Coordinator coordinator = new Coordinator();
    //        if (coordinator.coordinate(plannedQuery) instanceof MetadataInProgressQuery) {
    //            assertTrue(true);
    //            coordinator.persist(plannedQuery);
    //        } else {
    //            fail("Coordinator.coordinate not creating new MetadataInProgressQuery");
    //        }
    //    }
    //
    //    //INSERT INTO
    //    @Test
    //    public void testInsertInto() throws Exception {
    //        String catalog = "catalogTest6";
    //        String table = "tableTest";
    //
    //        BaseQuery baseQuery =
    //                new BaseQuery(UUID.randomUUID().toString(),
    //                        "INSERT INTO catalogTest6.tableTest ('col1', 'col2') VALUES (1, 2)",
    //                        new CatalogName(catalog));
    //
    //        InsertIntoStatement insert = new InsertIntoStatement(new TableName(catalog, table), new ArrayList<ColumnName>(),
    //                new SelectStatement(new TableName(catalog, table)), false);
    //
    //        StorageParsedQuery metadataParsedQuery =
    //                new StorageParsedQuery(baseQuery, insert);
    //
    //        StorageValidatedQuery metadataValidatedQuery = new StorageValidatedQuery(metadataParsedQuery);
    //
    //        StoragePlannedQuery plannedQuery = new StoragePlannedQuery(metadataValidatedQuery, null);
    //
    //        Coordinator coordinator = new Coordinator();
    //        if (coordinator.coordinate(plannedQuery) instanceof StorageInProgressQuery) {
    //            assertTrue(true);
    //            coordinator.persist(plannedQuery);
    //        } else {
    //            fail("Coordinator.coordinate not creating new StorageInProgressQuery");
    //        }
    //    }
    //
    //    private void createTestDatastoreAndPersist(String name, String version) {
    //        // Create and add a test datastore metadata to the metadatamanager
    //        DataStoreName dataStoreName = new DataStoreName(name);
    //        String dataStoreVersion = version;
    //        RequiredPropertiesType requiredProperties = null;
    //        OptionalPropertiesType othersProperties = null;
    //        DataStoreMetadata datastoreTest =
    //                new DataStoreMetadata(dataStoreName, dataStoreVersion, requiredProperties, othersProperties);
    //        MetadataManager.MANAGER.createDataStore(datastoreTest, false);
    //    }
    //
    //    private void createTestClusterAndPersist(String cluster, String datastore) {
    //        // Create and add a test cluster metadata to the metadatamanager
    //        ClusterName clusterName = new ClusterName(cluster);
    //        DataStoreName dataStoreRef = new DataStoreName(datastore);
    //        Map<Selector, Selector> options = new HashMap<>();
    //        Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefs = new HashMap<>();
    //        ClusterMetadata clusterTest =
    //                new ClusterMetadata(clusterName, dataStoreRef, options, connectorAttachedRefs);
    //        MetadataManager.MANAGER.createClusterAndAttach(clusterTest, false);
    //    }
    //
    //    private void createTestsCatalogAndPersist(String catalog) {
    //        // Create and add test catalog to the metadatamanager
    //        CatalogName catalogName = new CatalogName(catalog);
    //        CatalogMetadata catalogMetadata =
    //                new CatalogMetadata(catalogName, new HashMap<Selector, Selector>(),
    //                        new HashMap<TableName, TableMetadata>());
    //        MetadataManager.MANAGER.createCatalog(catalogMetadata);
    //    }
    //
    //    private void createTestTableAndPersist(String clusterName, String catalogName,
    //            String tableNameString) {
    //        // Create and add test table to the metadatamanager
    //        TableName tableName = new TableName(catalogName, tableNameString);
    //        TableMetadata table =
    //                new TableMetadata(tableName, new HashMap<Selector, Selector>(),
    //                        new HashMap<ColumnName, ColumnMetadata>(), new HashMap<IndexName, IndexMetadata>(),
    //                        new ClusterName(clusterName), new ArrayList<ColumnName>(), new ArrayList<ColumnName>());
    //
    //        Map<ColumnName, ColumnType> columns = new HashMap<ColumnName, ColumnType>();
    //        for (Entry<ColumnName, ColumnMetadata> c : table.getColumns().entrySet()) {
    //            columns.put(c.getKey(), c.getValue().getColumnType());
    //        }
    //        MetadataManager.MANAGER.createTable(table);
    //    }
    //
}
