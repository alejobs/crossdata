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

package com.stratio.crossdata.server.actors

import akka.actor.{ReceiveTimeout, RootActorPath, ActorLogging, Actor, Props, Address}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ClusterMetricsChanged, MemberEvent, MemberExited,
MemberRemoved, UnreachableMember, CurrentClusterState, MemberUp, ClusterDomainEvent}
import com.stratio.crossdata.common.connector.ConnectorClusterConfig
import com.stratio.crossdata.common.data
import com.stratio.crossdata.common.data.{NodeName, ConnectorName, Status}
import com.stratio.crossdata.common.executionplan.{ResultType, ExecutionType, ManagementWorkflow, ExecutionWorkflow}
import com.stratio.crossdata.common.result.{ErrorResult, Result, ConnectResult}
import com.stratio.crossdata.common.utils.StringUtils
import com.stratio.crossdata.communication.{replyConnectorName, getConnectorName,Connect}
import com.stratio.crossdata.core.execution.{ExecutionInfo, ExecutionManager}
import com.stratio.crossdata.core.loadwatcher.LoadWatcherManager
import com.stratio.crossdata.core.metadata.MetadataManager
import org.apache.log4j.Logger

import scala.collection.JavaConversions._
import scala.collection.mutable
import com.stratio.crossdata.common.statements.structures.SelectorHelper
import java.util.{Collections, UUID}

object ConnectorManagerActor {
  def props(cluster:Cluster): Props = Props(new ConnectorManagerActor(cluster))
}

class ConnectorManagerActor(cluster:Cluster) extends Actor with ActorLogging {

  lazy val logger = Logger.getLogger(classOf[ConnectorManagerActor])
  logger.info("Lifting connector manager actor")
  val coordinatorActorRef = context.actorSelection("../../CoordinatorActor")//context.actorSelection("../CoordinatorActor")
  var connectorsAlreadyReset = false

  log.info("Lifting connector manager actor")

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[ClusterDomainEvent])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def receive : Receive= {

    /**
     * A new actor connects to the cluster. If the new actor is a connector, we requests its name.
     */
    //TODO Check that new actors are recognized and their information stored in the MetadataManager
    case mu: MemberUp => {
      logger.info("Member is Up: " + mu.toString + mu.member.getRoles)
      val it = mu.member.getRoles.iterator()
      while (it.hasNext()) {
        val role = it.next()
        role match {
          case "connector" => {

            val connectorActorRef = context.actorSelection(RootActorPath(mu.member.address) / "user" / "ConnectorActor")
            val nodeName = new NodeName(mu.member.address.toString)
            if(MetadataManager.MANAGER.checkGetConnectorName(nodeName)){
              logger.debug("Asking its name to the connector " + mu.member.address)
              connectorActorRef ! getConnectorName()
            }
          }
          case _ => {
            logger.debug(mu.member.address + " has the role: " + role)
          }
        }
      }
    }

    /**
     * CONNECTOR answers its name.
     */
    case msg: replyConnectorName => {
      logger.info("Connector Name " + msg.name + " received from " + sender)
      val actorRefUri = StringUtils.getAkkaActorRefUri(sender, false)
      logger.info("Registering connector from: " + actorRefUri)

      if(actorRefUri != null){
        val connectorName = new ConnectorName(msg.name)
        ExecutionManager.MANAGER.createEntry(actorRefUri, connectorName, true)

        MetadataManager.MANAGER.addConnectorRef(connectorName, actorRefUri)

        val connectorMetadata = MetadataManager.MANAGER.getConnector(connectorName)
        val clusterProps = connectorMetadata.getClusterProperties

        if((clusterProps != null) && (!clusterProps.isEmpty)){

          for(clusterProp <- clusterProps.entrySet()){
            val clusterName = clusterProp.getKey
            val clusterMetadata = MetadataManager.MANAGER.getCluster(clusterName)
            val optsCluster = MetadataManager.MANAGER.getCluster(clusterName).getOptions;

            val optsConnector = clusterMetadata.getConnectorAttachedRefs.get(connectorName).getProperties

            val clusterConfig = new ConnectorClusterConfig(clusterName,
              SelectorHelper.convertSelectorMapToStringMap(optsConnector),
              SelectorHelper.convertSelectorMapToStringMap(optsCluster))

            clusterConfig.setDataStoreName(clusterMetadata.getDataStoreRef)

            val reconnectQueryUUID = UUID.randomUUID().toString
            val executionInfo = new ExecutionInfo
            executionInfo.setRemoveOnSuccess(true)
            executionInfo.setUpdateOnSuccess(true)
            val executionWorkflow = new ManagementWorkflow(reconnectQueryUUID, Collections.emptySet[String](), ExecutionType.ATTACH_CONNECTOR,ResultType.RESULTS)
            executionWorkflow.setClusterName(clusterName)
            executionInfo.setWorkflow(executionWorkflow)
            ExecutionManager.MANAGER.createEntry(reconnectQueryUUID, executionInfo)

            sender ! new Connect(reconnectQueryUUID, null, clusterConfig)
          }
        }
        MetadataManager.MANAGER.setConnectorStatus(connectorName, Status.ONLINE)
        MetadataManager.MANAGER.setNodeStatus(new NodeName(sender.path.address.toString), Status.ONLINE)
      } else {
        logger.error("Actor reference of the sender can't be null")
      }

    }

    case c: ConnectResult => {
      logger.info("Connect result from " + sender + " => " + c.getSessionId)
      coordinatorActorRef forward c
    }

    case er: ErrorResult => {
      coordinatorActorRef ! er
    }

    //Pass the message to the connectorActor to extract the member in the cluster
    case state: CurrentClusterState => {
      logger.info("Current members: " + state.members.mkString(", "))
      if(!connectorsAlreadyReset){
        var foundServers = mutable.HashSet[Address]()
        val members = state.getMembers
        for(member <- members){
          logger.info("Address: " + member.address + ", roles: " + member.getRoles )
          if(member.getRoles.contains("server")){
            foundServers += member.address
            logger.info("New server added. Size: " + foundServers.size)
          }
        }
        if(foundServers.size == 1){
          logger.info("Resetting Connectors status")
          val connectors = MetadataManager.MANAGER.getConnectorNames(data.Status.ONLINE)
          MetadataManager.MANAGER.setConnectorStatus(connectors, data.Status.OFFLINE)
          val nodes = MetadataManager.MANAGER.getNodeNames(data.Status.ONLINE)
          MetadataManager.MANAGER.setNodeStatus(nodes, data.Status.OFFLINE)
          connectorsAlreadyReset = true
        }
      }
    }

    case member: UnreachableMember => {
      logger.info("Member detected as unreachable: " + member)
      //TODO Process UnreachableMember
    }

    case member: MemberRemoved => {
      logger.info("Member is Removed: " + member.member.address)
      logger.info("Member info: " + member.toString)
      val actorRefUri = StringUtils.getAkkaActorRefUri(member.member.address, false)+"/user/ConnectorActor/"
      if(ExecutionManager.MANAGER.exists(actorRefUri)){
        val connectorName = ExecutionManager.MANAGER.getValue(actorRefUri)
        logger.info("Removing Connector: " + connectorName)
        MetadataManager.MANAGER.removeActorRefFromConnector(connectorName.asInstanceOf[ConnectorName], actorRefUri)
        MetadataManager.MANAGER.setNodeStatus(new NodeName(member.member.address.toString), Status.OFFLINE)
        ExecutionManager.MANAGER.deleteEntry(actorRefUri)
      } else {
        logger.warn(actorRefUri + " not found in the Execution Manager")
      }
      // example of member.member.address = akka.tcp://CrossdataServerCluster@127.0.0.1:13421
      val lwmkey=member.member.address.toString.split("@")(1).split(":")(0)
      if(LoadWatcherManager.MANAGER.exists(lwmkey)){
        LoadWatcherManager.MANAGER.deleteEntry(lwmkey)
      }
    }

    case member: MemberExited => {
      logger.info("Member is exiting: " + member.member.address)
      val actorRefUri = StringUtils.getAkkaActorRefUri(sender, false)
      val connectorName = ExecutionManager.MANAGER.getValue(actorRefUri)
      MetadataManager.MANAGER.removeActorRefFromConnector(connectorName.asInstanceOf[ConnectorName], actorRefUri)
      MetadataManager.MANAGER.setNodeStatus(new NodeName(member.member.address.toString), Status.OFFLINE)
    }

    case _: MemberEvent => {
      logger.info("Receiving anything else")
      //TODO Process MemberEvent
    }

    case clusterMetricsChanged: ClusterMetricsChanged => {

    }

    case clusterDomainEvent: ClusterDomainEvent => {
      logger.debug("ClusterDomainEvent: " + clusterDomainEvent)
      //TODO Process ClusterDomainEvent
    }

    case ReceiveTimeout => {
      logger.info("ReceiveTimeout")
      //TODO Process ReceiveTimeout
    }

    case unknown: Any=> {
      sender ! Result.createUnsupportedOperationErrorResult("Not recognized object")
      logger.error("Unknown event: " + unknown)
    }
  }

}
