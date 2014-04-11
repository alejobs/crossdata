package com.stratio.meta.server.parser

import com.stratio.meta.core.engine.Engine
import akka.actor.ActorSystem
import com.stratio.meta.server.actors._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuiteLike
import scala.concurrent.duration._
import org.testng.Assert._
import com.stratio.meta.server.utilities._
import scala.collection.mutable
import com.stratio.meta.server.config.BeforeAndAfterCassandra


/**
 * Created by aalcocer on 4/8/14.
 * To generate unit test of proxy actor
 */
class BasicParserActorTest extends TestKit(ActorSystem("TestKitUsageExectutorActorSpec",ConfigFactory.parseString(TestKitUsageSpec.config)))
with DefaultTimeout with FunSuiteLike with BeforeAndAfterCassandra
{

  lazy val engine:Engine =  createEngine.create()


  lazy val executorRef = system.actorOf(ExecutorActor.props(engine.getExecutor),"TestExecutorActor")
  lazy val plannerRef = system.actorOf(PlannerActor.props(executorRef,engine.getPlanner),"TestPlanerActor")
  lazy val validatorRef = system.actorOf(ValidatorActor.props(plannerRef,engine.getValidator),"TestValidatorActor")
  lazy val parserRef = system.actorOf(ParserActor.props(validatorRef,engine.getParser),"TestParserActor")
  lazy val parserRefTest= system.actorOf(ParserActor.props(testActor,engine.getParser),"TestParserActorTest")

  lazy val process2=new queryCaseElse
  lazy val myCommandResult=process2.queryelse(parserRef)


  override def beforeCassandraFinish() {
    shutdown(system)
  }


  test ("ServerActor Test send nothing"){

    within(2000 millis){

      assertEquals(myCommandResult.getErrorMessage,"Not recognized object")
    }
  }

  val querying= new queryString


  test ("parser Test"){

    within(3000 millis){

      validatorRef ! 1
      expectNoMsg()

    }
  }
  test ("parserActor create KS"){

    within(3000 millis){

      val msg= "create KEYSPACE ks_demo WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"sucess" )

    }
  }
  test ("parserActor create KS yet"){

    within(3000 millis){

      val msg="create KEYSPACE ks_demo WITH replication = {class: SimpleStrategy, replication_factor: 1};"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"Keyspace ks_demo already exists" )
    }
  }

  test ("parserActor use KS"){

    within(3000 millis){

      val msg="use ks_demo ;"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"sucess" )
    }
  }

  test ("parserActor use KS yet"){

    within(3000 millis){

      val msg="use ks_demo ;"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"sucess" )
    }
  }


  test ("parserActor use KS not create"){

    within(3000 millis){

      val msg="use ks_demo_not ;"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"Keyspace 'ks_demo_not' does not exist" )
    }
  }
  test ("parserActor insert into table not create yet without error"){

    within(3000 millis){

      val msg="insert into demo (field1, field2) values ('test1','text2');"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"unconfigured columnfamily demo" )
    }
  }
  test ("parserActor select without table"){

    within(3000 millis){

      val msg="select * from demo ;"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"unconfigured columnfamily demo")
    }
  }


  test ("parserActor create table not create yet"){

    within(3000 millis){

      val msg="create TABLE demo (field1 text PRIMARY KEY , field2 text);"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"sucess" )
    }
  }

  test ("parserActor create table  create yet"){

    within(3000 millis){

      val msg="create TABLE demo (field1 text PRIMARY KEY , field2 text);"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"Table ks_demo.demo already exists" )
    }
  }

  test ("parserActor insert into table  create yet without error"){

    within(3000 millis){

      val msg="insert into demo (field1, field2) values ('test1','text2');"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"sucess" )
    }
  }
  test ("parserActor select"){

    within(3000 millis){

      val msg="select * from demo ;"
      assertEquals(querying.proccess(msg,parserRef,engine,4),mutable.MutableList("test1", "text2").toString() )
    }
  }
  test ("parserActor drop table "){

    within(3000 millis){

      val msg="drop table demo ;"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"sucess" )
    }
  }
  test ("parserActor drop KS "){

    within(3000 millis){

      val msg="drop keyspace ks_demo ;"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"sucess" )
    }
  }
  test ("parserActor drop KS  not exit"){

    within(3000 millis){

      val msg="drop keyspace ks_demo ;"
      assertEquals(querying.proccess(msg,parserRef,engine,4),"Cannot drop non existing keyspace 'ks_demo'." )
    }
  }


}


