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

package com.stratio.connector.inmemory;

import com.codahale.metrics.Timer;
import com.stratio.connector.inmemory.datastore.InMemoryDatastore;
import com.stratio.connector.inmemory.metadata.MetadataListener;
import com.stratio.crossdata.common.connector.*;
import com.stratio.crossdata.common.data.ClusterName;
import com.stratio.crossdata.common.exceptions.*;
import com.stratio.crossdata.common.metadata.CatalogMetadata;
import com.stratio.crossdata.common.metadata.TableMetadata;
import com.stratio.crossdata.common.security.ICredentials;
import com.stratio.crossdata.connectors.ConnectorApp;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * InMemory connector that demonstrates the internals of a crossdata connector.
 * @see <a href="https://github.com/Stratio/crossdata/_doc/InMemory-Connector-Development-Tutorial.md">InMemory Connector
 * development tutorial</a>
 */
public class InMemoryConnector extends AbstractExtendedConnector {


    /**
     * Class logger.
     */
    private static final Logger LOG = Logger.getLogger(InMemoryConnector.class);
    private static final int DEFAULT_TIMEOUT_IN_MS = 5000;

    /**
     * Map associating the {@link com.stratio.crossdata.common.data.ClusterName}s with
     * the InMemoryDatastores. This type of map usually links with the established connections.
     */
    private final Map<ClusterName, InMemoryDatastore> clusters = new HashMap<>();

    private InMemoryQueryEngine queryEngine;

    private InMemoryStorageEngine storageEngine;

    private InMemoryMetadataEngine metadataEngine;

    private final Timer connectTimer;

    /**
     * Constant defining the required datastore property.
     */
    private static final String DATASTORE_PROPERTY = "TableRowLimit";

    public InMemoryConnector(IConnectorApp connectorApp) {
        super(connectorApp);
        connectTimer = new Timer();
        String timerName = name(InMemoryConnector.class, "connect");
        registerMetric(timerName, connectTimer);
    }

    @Override
    public String getConnectorName() {
        return "InMemoryConnector";
    }

    @Override
    public String[] getDatastoreName() {
        return new String[]{"InMemoryDatastore"};
    }

    @Override
    public void init(IConfiguration configuration) throws InitializationException {
        //The initialization method is called when the connector is launched, currently an
        //empty implementation is passed as it will be a future feature of Crossdata.
        LOG.info("InMemoryConnector launched");
    }

    @Override
    public void connect(ICredentials credentials, ConnectorClusterConfig config) throws ConnectionException {
        //Init Metric
        Timer.Context connectTimerContext = connectTimer.time();

        // Connection
        final ClusterName targetCluster = config.getName();
        Map<String, String> options = config.getClusterOptions();
        LOG.info("clusterOptions: " + config.getClusterOptions().toString() + " connectorOptions: " + config.getConnectorOptions());
        if(!options.isEmpty() && options.get(DATASTORE_PROPERTY) != null){
            //At this step we usually connect to the database. As this is an tutorial implementation,
            //we instantiate the Datastore instead.
            InMemoryDatastore datastore = new InMemoryDatastore(Integer.valueOf(options.get(DATASTORE_PROPERTY)));
            clusters.put(targetCluster, datastore);
        } else {
            long millis = connectTimerContext.stop() / 1000000;
            LOG.info("Connection took " + millis + " milliseconds");
            throw new ConnectionException("Invalid options, expecting TableRowLimit");
        }

        //Try to restore existing schema 2 seconds after the connection
        //TODO waiting for Crossdata version 0.4.0
        /*new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            restoreSchema(targetCluster);
                        }catch(ConnectorException ce){
                            LOG.error("Error fetching existing schema from Crossdata server");
                        }
                    }
                },
                2000
        );
        */

        //End Metric
        long millis = connectTimerContext.stop() / 1000000;
        LOG.info("Connection took " + millis + " milliseconds");
    }

    @Override
    public void close(ClusterName name) throws ConnectionException {
        //This method usually closes the session with the given cluster and removes any relevant data.
        if(clusters.get(name) != null) {
            clusters.remove(name);
        } else {
            throw new ConnectionException("Cluster " + name + "does not exist");
        }
    }

    @Override
    public void shutdown() throws ExecutionException {
        LOG.info("Shutting down InMemoryConnector");
    }

    @Override
    public boolean isConnected(ClusterName name) {
        return clusters.get(name) != null;
    }

    @Override
    public IStorageEngine getStorageEngine() throws UnsupportedException {
        if(storageEngine == null){
            storageEngine = new InMemoryStorageEngine(this);
        }
        return storageEngine;
    }

    @Override
    public IQueryEngine getQueryEngine() throws UnsupportedException {
        if(queryEngine == null){
            queryEngine = new InMemoryQueryEngine(this);
        }
        return queryEngine;
    }

    @Override
    public IMetadataEngine getMetadataEngine() throws UnsupportedException {
        if(metadataEngine == null){
            metadataEngine = new InMemoryMetadataEngine(this);
        }
        return metadataEngine;
    }



    /**
     * Get the datastore associated to a given cluster.
     * @param cluster The cluster name.
     * @return A {@link com.stratio.connector.inmemory.datastore.InMemoryDatastore}.
     */
    protected InMemoryDatastore getDatastore(ClusterName cluster){
        return this.clusters.get(cluster);
    }


   /* private void restoreSchema(ClusterName cluster) throws ConnectorException {
        List<CatalogMetadata> catalogList = getCatalogs(cluster, DEFAULT_TIMEOUT_IN_MS).get();
        if (catalogList != null){
            for (CatalogMetadata catalogMetadata : catalogList) {
                LOG.debug("Restoring catalog: "+catalogMetadata.toString());
                getMetadataEngine().createCatalog(catalogMetadata.getTables().values().iterator().next().getClusterRef(),
                                catalogMetadata);
                for (TableMetadata tableMetadata : catalogMetadata.getTables().values()) {
                    LOG.debug("Restoring table: "+tableMetadata.toString());
                    getMetadataEngine().createTable(tableMetadata.getClusterRef(),tableMetadata);
                }
            }
        }

    }*/


    /**
     * Run an InMemory Connector using a {@link com.stratio.crossdata.connectors.ConnectorApp}.
     * @param args The arguments.
     */
    public static void main(String [] args){
        ConnectorApp connectorApp = new ConnectorApp();
        InMemoryConnector inMemoryConnector = new InMemoryConnector(connectorApp);
        connectorApp.startup(inMemoryConnector);
        MetadataListener metadataListener = new MetadataListener();
        connectorApp.subscribeToMetadataUpdate(metadataListener);

    }
}
