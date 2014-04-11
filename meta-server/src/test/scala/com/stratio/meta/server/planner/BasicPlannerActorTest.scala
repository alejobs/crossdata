package com.stratio.meta.server.planner

import com.stratio.meta.core.engine.Engine
import akka.actor.ActorSystem
import com.stratio.meta.server.actors.{PlannerActor,  ExecutorActor}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuiteLike
import com.stratio.meta.common.result.Result
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import org.testng.Assert._
import scala.util.Success
import com.stratio.meta.server.utilities._
import scala.collection.mutable
import com.stratio.meta.server.config.BeforeAndAfterCassandra


/**
 * Created by aalcocer on 4/8/14.
 * To generate unit test of proxy actor
 */
class BasicPlannerActorTest extends TestKit(ActorSystem("TestKitUsageExectutorActorSpec",ConfigFactory.parseString(TestKitUsageSpec.config)))
with DefaultTimeout with FunSuiteLike  with  BeforeAndAfterCassandra
{

  lazy val engine:Engine =  createEngine.create()


  lazy val executorRef = system.actorOf(ExecutorActor.props(engine.getExecutor),"TestExecutorActor")
  lazy val plannerRef = system.actorOf(PlannerActor.props(executorRef,engine.getPlanner),"TestPlanerActor")
  lazy val plannerRefTest= system.actorOf(PlannerActor.props(testActor,engine.getPlanner),"TestPlanerActorTest")


  override def beforeCassandraFinish() {
    shutdown(system)
  }


  test("executor resend to executor message 1"){
    within(2000 millis){

      val query="create KEYSPACE ks_demo1 WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      val stmt = engine.getParser.parseStatement(query)
      stmt.setTargetKeyspace("ks_demo1")
      val stmt1=engine.getValidator.validateQuery(stmt)
      plannerRefTest ! stmt1
      expectMsg(engine.getPlanner.planQuery(stmt1))

    }
  }
  test("executor resend to executor message 2"){
    within(2000 millis){

      val query="create KEYSPACE ks_demo1 WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      val stmt = engine.getParser.parseStatement(query)
      stmt.setTargetKeyspace("ks_demo1")
      val stmt1=engine.getValidator.validateQuery(stmt)
      stmt1.setError()
      plannerRefTest ! stmt1
      expectNoMsg()
    }
  }
  test("executor resend to executor message 3"){
    within(2000 millis){

      val query="create KEYSPACE ks_demo1 WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      val stmt = engine.getParser.parseStatement(query)
      stmt.setTargetKeyspace("ks_demo1")
      val stmt1=engine.getValidator.validateQuery(stmt)
      stmt1.setError()
      stmt1.setErrorMessage("it is a test of error")
      var complete:Boolean=true
      val futureExecutorResponse=plannerRefTest.ask(stmt1)(2 second)
      try{
        val result = Await.result(futureExecutorResponse, 1 seconds)
      }catch{
        case ex:Exception => {
          println("\n\n\n"+ex.getMessage+"\n\n\n")
          complete=false
        }
      }
      if (complete&&futureExecutorResponse.isCompleted){
        val value_response= futureExecutorResponse.value.get

        value_response match{
        case Success(value:Result)=>
          if (value.hasError){
            assertEquals(value.getErrorMessage,"it is a test of error")
          }

        }

      }
    }
  }

  val querying= new queryString


  test ("executor Test"){

    within(3000 millis){

      plannerRef ! 1
      expectNoMsg

    }
  }
  test ("QueryActor create KS"){

    within(3000 millis){

      val msg= "create KEYSPACE ks_demo WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"sucess" )
    }
  }
  test ("QueryActor create KS yet"){

    within(3000 millis){

      val msg="create KEYSPACE ks_demo WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"Keyspace ks_demo already exists" )
    }
  }

  test ("QueryActor use KS"){

    within(3000 millis){

      val msg="use ks_demo ;"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"sucess" )
    }
  }

  test ("QueryActor use KS yet"){

    within(3000 millis){

      val msg="use ks_demo ;"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"sucess" )
    }
  }


  test ("QueryActor use KS not create"){

    within(3000 millis){

      val msg="use ks_demo_not ;"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"Keyspace 'ks_demo_not' does not exist" )
    }
  }
  test ("QueryActor insert into table not create yet without error"){

    within(3000 millis){

      val msg="insert into demo (field1, field2) values ('test1','text2');"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"unconfigured columnfamily demo" )
    }
  }
  test ("QueryActor select without table"){

    within(3000 millis){

      val msg="select * from demo ;"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"unconfigured columnfamily demo")
    }
  }


  test ("QueryActor create table not create yet"){

    within(3000 millis){

      val msg="create TABLE demo (field1 text PRIMARY KEY , field2 text);"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"sucess" )
    }
  }

  test ("QueryActor create table  create yet"){

    within(3000 millis){

      val msg="create TABLE demo (field1 text PRIMARY KEY , field2 text);"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"Table ks_demo.demo already exists" )
    }
  }

  test ("QueryActor insert into table  create yet without error"){

    within(3000 millis){

      val msg="insert into demo (field1, field2) values ('test1','text2');"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"sucess" )
    }
  }
  test ("QueryActor select"){

    within(3000 millis){

      val msg="select * from demo ;"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),mutable.MutableList("test1", "text2").toString() )
    }
  }
  test ("QueryActor drop table "){

    within(3000 millis){

      val msg="drop table demo ;"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"sucess" )
    }
  }
  test ("QueryActor drop KS "){

    within(3000 millis){

      val msg="drop keyspace ks_demo ;"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"sucess" )
    }
  }
  test ("QueryActor drop KS  not exit"){

    within(3000 millis){

      val msg="drop keyspace ks_demo ;"
      assertEquals(querying.proccess(msg,plannerRef,engine,2),"Cannot drop non existing keyspace 'ks_demo'." )
    }
  }
}



