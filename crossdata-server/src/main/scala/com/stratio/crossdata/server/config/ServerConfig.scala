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

package com.stratio.crossdata.server.config

import java.io.File

import com.stratio.crossdata.core.engine.EngineConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j.Logger

object ServerConfig {
  val SERVER_BASIC_CONFIG = "server-reference.conf"
  val PARENT_CONFIG_NAME = "crossdata-server"


  val SERVER_CLUSTER_NAME_KEY = "config.cluster.name"
  val SERVER_ACTOR_NAME_KEY = "config.cluster.actor"
  val SERVER_USER_CONFIG_FILE = "external.config.filename"
  val SERVER_USER_CONFIG_RESOURCE = "external.config.resource"

  //val SERVER_ACTOR_NUM= "config.akka.number.server-actor"
}

trait ServerConfig extends GridConfig with NumberActorConfig {

  lazy val logger: Logger = ???
  lazy val engineConfig: EngineConfig = {
    val result = new EngineConfig()
    result.setGridListenAddress(gridListenAddress)
    result.setGridContactHosts(gridContactHosts)
    result.setGridPort(gridPort)
    result.setGridMinInitialMembers(gridMinInitialMembers)
    result.setGridJoinTimeout(gridJoinTimeout)
    result.setGridPersistencePath(gridPersistencePath)
    result
  }
  lazy val clusterName = config.getString(ServerConfig.SERVER_CLUSTER_NAME_KEY)
  lazy val actorName = config.getString(ServerConfig.SERVER_ACTOR_NAME_KEY)
  override val config: Config = {

    var defaultConfig = ConfigFactory.load(ServerConfig.SERVER_BASIC_CONFIG).getConfig(ServerConfig.PARENT_CONFIG_NAME)
    val configFile = defaultConfig.getString(ServerConfig.SERVER_USER_CONFIG_FILE)
    val configResource = defaultConfig.getString(ServerConfig.SERVER_USER_CONFIG_RESOURCE)

    if (configResource != "") {
      val resource = ServerConfig.getClass.getClassLoader.getResource(configResource)
      if (resource != null) {
        val userConfig = ConfigFactory.parseResources(configResource).getConfig(ServerConfig.PARENT_CONFIG_NAME)
        defaultConfig = userConfig.withFallback(defaultConfig)
      } else {
        logger.warn("User resource (" + configResource + ") haven't been found")
        val file = new File(configResource)
        if (file.exists()) {
          val userConfig = ConfigFactory.parseFile(file).getConfig(ServerConfig.PARENT_CONFIG_NAME)
          defaultConfig = userConfig.withFallback(defaultConfig)
        } else {
          logger.warn("User file (" + configResource + ") haven't been found in classpath")
        }
      }
    }

    if (configFile != "") {
      val file = new File(configFile)
      if (file.exists()) {
        val userConfig = ConfigFactory.parseFile(file).getConfig(ServerConfig.PARENT_CONFIG_NAME)
        defaultConfig = userConfig.withFallback(defaultConfig)
      } else {
        logger.warn("User file (" + configFile + ") haven't been found")
      }
    }

    ConfigFactory.load(defaultConfig)
  }

  //lazy val num_actors_server_actor:Int  = config.getString(ServerConfig.SERVER_ACTOR_NUM).toInt
}

