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

package com.stratio.crossdata.connectors

import java.util

import com.codahale.metrics._
import akka.agent.Agent
import akka.actor._
import akka.pattern.ask
import akka.routing.DefaultResizer
import com.stratio.crossdata.common.annotation.Experimental
import com.stratio.crossdata.common.data._
import com.stratio.crossdata.common.metadata.{UpdatableMetadata, CatalogMetadata, TableMetadata}
import com.stratio.crossdata.common.utils.{Metrics, StringUtils}
import com.stratio.crossdata.connectors.config.ConnectConfig
import com.stratio.crossdata.common.connector._
import org.apache.log4j.Logger
import scala.collection.mutable.{ListMap, Set}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.collection.JavaConversions._
import com.stratio.crossdata.communication.GetCatalogs
import akka.routing.RoundRobinPool
import com.stratio.crossdata.communication.GetTableMetadata
import com.stratio.crossdata.communication.Shutdown


object ConnectorApp extends App {
  args.length==2
}

class ConnectorApp extends ConnectConfig with IConnectorApp {

  lazy val system = ActorSystem(clusterName, config)
  val connectedServers: Set[String] = Set()
  override lazy val logger = Logger.getLogger(classOf[ConnectorApp])

  var actorClusterNode: Option[ActorRef] = None

  var metricName: String = "connector"

  val metadataMapAgent = Agent(new ObservableMap[Name, UpdatableMetadata])(system.dispatcher)
  val runningJobsAgent =   Agent(new ListMap[String, ActorRef]())(system.dispatcher)

  logger.info("Connector Name: " + connectorName)

  if((connectorName.isEmpty) || (connectorName.equalsIgnoreCase("XconnectorX"))){
    logger.error("##########################################################################################");
    logger.error("# ERROR ##################################################################################");
    logger.error("##########################################################################################");
    logger.error("# USING DEFAULT CONNECTOR NAME: XconnectorX                                              #")
    logger.error("# CHANGE PARAMETER crossdata-connector.config.connector.name FROM THE CONFIGURATION FILE #")
    logger.error("##########################################################################################");
  }

  def stop():Unit = {
    actorClusterNode.get ! Shutdown()
    system.shutdown()
    Metrics.getRegistry.getNames.foreach(Metrics.getRegistry.remove(_))

  }

  def startup(connector: IConnector): ActorSelection = {

    val resizer = DefaultResizer(lowerBound = 2, upperBound = 15)
    val connectorManagerActorRef = system.actorOf(
      RoundRobinPool(num_connector_actor, Some(resizer))
        .props(Props(classOf[ConnectorActor], connector.getConnectorName, connector, connectedServers, metadataMapAgent, runningJobsAgent)),
      "ConnectorActor")

    actorClusterNode = Some(connectorManagerActorRef)
    connector.init(new IConfiguration {})
    val actorSelection: ActorSelection = system.actorSelection(
      StringUtils.getAkkaActorRefUri(actorClusterNode.get.toString(), false))

    metricName = MetricRegistry.name(connector.getConnectorName, "connection", "status")
    Metrics.getRegistry.register(metricName,
      new Gauge[Boolean] {
        override def getValue: Boolean = {
          var status: Boolean = true
          if (connectedServers.isEmpty) {
            status = false
          }
          status
        }
      })
    actorSelection
  }

  override def getTableMetadata(clusterName: ClusterName, tableName: TableName, timeout: Int): Option[TableMetadata] = {
    /*TODO: for querying actor internal state, only messages should be used.
      i.e.{{{
        import scala.concurrent.duration._
        val timeout: akka.util.Timeout = 2.seconds
        val response: Option[TableMetadata] = 
          actorClusterNode.map(actor => Await.result((actor ? GetTableMetadata).mapTo[TableMetadata],timeout))
        response.getOrElse(throw new IllegalStateException("Actor cluster node is not initialized"))
      }}}
    */
    val future = actorClusterNode.get.?(GetTableMetadata(clusterName,tableName ))(timeout)
    Try(Await.result(future.mapTo[TableMetadata],Duration.fromNanos(timeout*1000000L))).map{ Some (_)}.recover{
      case e: Exception => logger.debug("Error fetching the catalog metadata from the ObservableMap: "+e.getMessage); None
    }.get

  }

  /*
  TODO Review 0.4.0
  override def getCatalogMetadata(catalogName: CatalogName, timeout: Int): Option[CatalogMetadata] ={
    val future = actorClusterNode.get.?(GetCatalogMetadata(catalogName))(timeout)
    Try(Await.result(future.mapTo[CatalogMetadata],Duration.fromNanos(timeout*1000000L))).map{ Some (_)}.recover{
      case e: Exception => logger.debug("Error fetching the catalog metadata from the ObservableMap: "+e.getMessage); None
    }.get

  }
 */

  /*
  /**
   * Recover the list of catalogs associated to the specified cluster.
   * @param cluster the cluster name.
   * @param timeout the timeout in ms.
   * @return The list of catalog metadata or null if the list is not ready after waiting the specified time.
   */
  @Experimental
  override def getCatalogs(cluster: ClusterName,timeout: Int = 10000): Option[util.List[CatalogMetadata]] ={
    val future = actorClusterNode.get.ask(GetCatalogs(cluster))(timeout)
    Try(Await.result(future.mapTo[util.List[CatalogMetadata]],Duration.fromNanos(timeout*1000000L))).map{ Some (_)}.recover {
      case e: Exception => logger.debug("Error fetching the catalogs from the ObservableMap: "+e.getMessage); None
    }.get
  }
 */

  override def getConnectionStatus(): ConnectionStatus = {
    var status: ConnectionStatus = ConnectionStatus.CONNECTED
    if (connectedServers.isEmpty){
      status = ConnectionStatus.DISCONNECTED
    }
    status
  }

  override def subscribeToMetadataUpdate(mapListener: IMetadataListener) ={
    //actorClusterNode.get.asInstanceOf[ConnectorActor].subscribeToMetadataUpdate(mapListener)
    actorClusterNode.get ! mapListener
  }

  override def registerMetric(name: String, metric: Metric): Metric = {
    Metrics.getRegistry.register(name, metric)
  }
}
