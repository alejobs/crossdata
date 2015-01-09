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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.stratio.crossdata.common.connector.ConnectorClusterConfig;
import com.stratio.crossdata.common.connector.IConfiguration;
import com.stratio.crossdata.common.connector.IConnector;
import com.stratio.crossdata.common.data.ClusterName;
import com.stratio.crossdata.common.exceptions.ConnectionException;
import com.stratio.crossdata.common.exceptions.InitializationException;
import com.stratio.crossdata.common.security.ICredentials;

/**
 * Main connector class tests.
 */
public class InMemoryConnectorTest {

    @Test
    public void createConnector(){
        IConnector connector = new InMemoryConnector(null);
        assertEquals(connector.getConnectorName(), "InMemoryConnector", "Invalid connector name");
        assertEquals(connector.getDatastoreName()[0], "InMemoryDatastore", "Invalid datastore name");
        try {
            connector.init(new IConfiguration() {
                @Override public int hashCode() {
                    return super.hashCode();
                }
            });
        } catch (InitializationException e) {
            fail("Failed to init the connector", e);
        }

        Map<String,String> clusterOptions = new HashMap<>();
        Map<String,String> connectorOptions = new HashMap<>();
        clusterOptions.put("TableRowLimit", "10");
        ClusterName clusterName = new ClusterName("cluster");
        ConnectorClusterConfig config = new ConnectorClusterConfig(clusterName, connectorOptions, clusterOptions);

        try {
            connector.connect(new ICredentials() {
                @Override public int hashCode() {
                    return super.hashCode();
                }
            }, config);
        } catch (ConnectionException e) {
            fail("Cannot connect to inmemory cluster", e);
        }
    }
}
