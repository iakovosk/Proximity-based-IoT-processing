package local

import akka.actor._
import akka.remote._
import common._
import concurrent.{Await, Future}
import scala.collection.mutable.ArrayBuffer 

import com.typesafe.config.ConfigFactory



object Local extends App {
	val system = ActorSystem("LocalSystem")
	val DNSActor = system.actorOf(Props[DNSActor], name = "DNSActor")
	DNSActor ! Start
}

class DNSActor extends Actor {

	val customConfig= ConfigFactory.parseString("""
	akka {
	log-config-on-start = off
	//loglevel = "DEBUG"
	actor {
		provider = "akka.remote.RemoteActorRefProvider"
	}
	log-dead-letters = off
	log-dead-letters-during-shutdown = off
	remote {
	log-remote-lifecycle-events = off
		enabled-transports = ["akka.remote.netty.tcp"]
		netty.tcp {
		hostname = "127.0.0.1"
		port = "5262"
		}
	}
	}
	""")

	val customConfig2= ConfigFactory.parseString("""
	akka {
	log-config-on-start = off
	//loglevel = "DEBUG"
	actor {
		provider = "akka.remote.RemoteActorRefProvider"
	}
	log-dead-letters = off
	log-dead-letters-during-shutdown = off
	remote {
	log-remote-lifecycle-events = off
		enabled-transports = ["akka.remote.netty.tcp"]
		netty.tcp {
		hostname = "127.0.0.1"
		port = "5263"
		}
	}
	}
	""")


	var incr = 0
	val masters = new ArrayBuffer[ActorRef](150)
	def receive = {
		//in case of Start it initiates the first master
	case Start =>

		val systems= ActorSystem("LocalSystem0", ConfigFactory.load(customConfig))

		val masterActor = (context.actorOf(Props[MasterActor].withDeploy( Deploy(scope = RemoteScope(AddressFromURIString("akka.tcp://LocalSystem0@127.0.0.1:5262")))), name = "MasterActor"))
		//println(masterActor)
		masterActor ! Start 
		//masterActor2 ! Start 
		//println(masterActor)
		masters+=(masterActor)
		println(masters(0))
		//	masters+=(masterActor2)
		incr = 1 //2
	case FetchMasters =>
		//println("Message sent")
		sender ! masters
	case DeployActor(newMaster) =>

		//println("Deploying a new master here: " + newMaster)
		incr+=1

		val address = AddressFromURIString(newMaster.path.root.toString)
		//println("Remotely deploying a master actor in this address: " + address)	
		val newMasterRef = (context.actorOf(Props[MasterActor].withDeploy( Deploy(scope = RemoteScope(address))), name = "MasterActor"+incr.toString))
		//println("Message sent")
		//println(newMasterRef)
		newMasterRef ! Start 
		masters+=newMasterRef
		
		//sends the new actor's actorRef to the sender so that it can send the nodes
		//println("Message sent")
		//println("Which is: " + newMasterRef)
		sender ! newMasterRef
		
	case RegisterAsMaster =>
		println("Threshold over the limit!")
		//Deploy a master actor at that specific node
		incr+=1
		val address = AddressFromURIString(sender.path.root.toString)	
		masters+=(context.actorOf(Props[MasterActor].withDeploy( Deploy(scope = RemoteScope(address))), name = "MasterActor"+incr.toString))	
		//println("Message sent")
		sender ! masters(incr-1)

	}
}


