package remote

import akka.actor._
import common._
import akka.remote._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.language.postfixOps
import akka.pattern.ask
import akka.util.Timeout



import com.typesafe.config.ConfigFactory





object HelloRemote extends App  {

	val customConfig = new ArrayBuffer[com.typesafe.config.Config](150)
	var incrPort = 0
	for( incrPort <- 5250 to 9540 by 5){
		customConfig+= ConfigFactory.parseString("""
	akka {
loglevel = "DEBUG"
	//log-config-on-start = off
	//stdout-loglevel = "OFF"
loglevel = "OFF"
	//
	actor {
lifecycle = off
		provider = "akka.remote.RemoteActorRefProvider"
	}
	log-dead-letters = off
	log-dead-letters-during-shutdown = off
	remote {
	log-remote-lifecycle-events = off
		enabled-transports = ["akka.remote.netty.tcp"]
		netty.tcp {
		hostname = "127.0.0.1"
		port = """ + incrPort.toString + """
		}
	}
	}
	""")
	}

	val systems = new ArrayBuffer[ActorSystem](500)
	var a = 0;
	val actors = new ArrayBuffer[ActorRef](1000)
	for(a<-0 to 399){
		systems+= ActorSystem("HelloRemoteSystem"+a.toString, ConfigFactory.load(customConfig(a)))
		actors+= systems(a).actorOf(Props[RemoteActor], name = "RemoteActor"+a.toString)
		Thread.sleep(200)
		actors(a) ! Start

	}

}


object myFunctions{

	def getClosestMaster(masters : ArrayBuffer[ActorRef], context: ActorContext) : (ActorRef, Int) = {
		var closestMaster=0
		var minLatency=1
		val latencyThreshold = 5000
		implicit val timeout = Timeout(latencyThreshold)
		for(incrPort <- 1 to masters.length){
			println("Mes")
		}
		val f = Future.firstCompletedOf(Seq.tabulate(masters.length){ a =>
			Future.apply{ val timestampBefore: Long = System.currentTimeMillis; /*println("Actually pinging: " + masters(a).path+"/PingActor");*/ val fakeLatency = Await.result(context.actorSelection(masters(a).path+"/PingActor") ? Ping,  timeout.duration).asInstanceOf[Int]; val timestampAfter: 			Long = System.currentTimeMillis;val latency = timestampAfter-timestampBefore + fakeLatency;  (a,latency)}   
		}).map { i =>
			closestMaster=i._1;minLatency=i._2.toInt
		}
		Await.result(f, timeout.duration)
		return(masters(closestMaster), minLatency)
	}
}

class RemoteActor extends Actor {
	var masters = new ArrayBuffer[ActorRef](4) //or maybe we only keep one master, if we do not care about failure prevention
	var closestMaster :ActorRef =_
	var minLatency :Long =_
	val latencyThreshold = 5000

	implicit val timeout = Timeout(5000)
	def receive = {
	case Start =>
		//Ask master ips from the DNSActor
		val DNSActor = context.actorSelection("akka.tcp://LocalSystem@127.0.0.1:5200/user/DNSActor")

		masters = Await.result(DNSActor ? FetchMasters,  timeout.duration).asInstanceOf[ArrayBuffer[ActorRef]]

		val reply = myFunctions.getClosestMaster(masters, context)
		closestMaster = reply._1
		minLatency = reply._2
		//We call a function that will collect our information 
		val y = new Nodes(5, 5, 5, 5)


		//We register to the closest landmark/master:
		//If the minimum latency rate is over a threshold, then this node becomes a master
		if(minLatency>latencyThreshold){
			val DNSActor = context.actorSelection("akka.tcp://LocalSystem@127.0.0.1:5200/user/DNSActor")
			//DNSActor ! RegisterAsMaster
			//it now becomes master of itself, pings itself also
			//closestMaster context.actorSelection("../MasterActor"), 0)
			closestMaster = Await.result(DNSActor ? RegisterAsMaster,  timeout.duration).asInstanceOf[ActorRef]
			println("Mes")
			closestMaster ! y
			Thread.sleep(500) //Needed because remote deployment takes time
			val timestampBefore: Long = System.currentTimeMillis
			//println("Message sent")
			val fakeLatency = Await.result(context.actorSelection(closestMaster.path+"/PingActor") ? Ping, timeout.duration).asInstanceOf[Int]
			val timestampAfter: Long = System.currentTimeMillis
			val minLatency = timestampAfter-timestampBefore + fakeLatency;	
		}else{
			//Register to the closest master
			//println("Message sent")
			closestMaster ! y //Maybe we could use JSON objects instead of Nodes? more IoT-like
		}

	case PingNewMaster(newMaster) =>
		if(newMaster != closestMaster){
			val timestampBefore: Long = System.currentTimeMillis
			val fakeLatency = Await.result(context.actorSelection(newMaster.path+"/PingActor") ? Ping, timeout.duration).asInstanceOf[Int] //generalize this!
			val timestampAfter: Long = System.currentTimeMillis
			val latency = timestampAfter-timestampBefore+fakeLatency;	
			//println("new latency: " + latency)
			//println("old latency: " + minLatency)
			if(latency<minLatency){
				val reply = (newMaster, latency)	
				closestMaster = reply._1
				minLatency = reply._2

				//println("Moving to the new actor")
				//Sending verification to former master and nodes information to the new master
				//println("Message sent")
				//println("Message sent")
				sender ! true
				newMaster ! new Nodes(5, 5, 5, 5)
				
			}else{
				//println("Not moving to the new actor")
				//println("Message sent")
				sender ! false
			}
		}else{
			//println("Message sent")
			sender ! false
		}
	case Message(msg) =>

		println(s"RemoteActor received message '$msg'")
	case msg: String =>
		println(s"RemoteActor received message '$msg'")
	case _ => 
		println("RemoteActor got something unexpected.")
		
	}
}

