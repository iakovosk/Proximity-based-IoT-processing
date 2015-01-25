package common
import akka.actor._
//import akka.remote._
import scala.collection.mutable.ArrayBuffer
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask 
import concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global



case object Start
case class Message(msg: String)
case object Ping
case object FetchMasters
case object RegisterAsMaster
case class PingNewMaster(newActor: ActorRef)
case class DeployActor(newMaster: ActorRef)
case class SendActorRef(newMaster: ActorRef)


case class Nodes(latency: Double, cpuClock: Double, battery: Double, actorContext: Double) {
	var late: Double = latency
	var cpu: Double = cpuClock
	var batt: Double = battery
	var actCon: Double = actorContext
}

class PingActor extends Actor {
	def receive = {
	case Ping =>
		var r = new scala.util.Random
		//println("Message sent")
		sender ! r.nextInt(150)
	}
}

class MasterActor extends Actor {

	// create the remote actor
	//val remote = context.actorFor("akka://HelloRemoteSystem@127.0.0.1:5150/user/RemoteActor")
	val x = new ArrayBuffer[(Nodes, ActorRef)](100)
	var slavesThreshold = 10
	def receive = {
	case Start => 
		//println("Started master actor here: " +self)
		val pingActor = context.actorOf(Props[PingActor], name = "PingActor")
	case Ping => //may be used ocassionaly, should refer to the PingActor instead, it will return wrong results if pinged when it is blocked for splitting
		sender ! "OK"
	case Message(msg) => 
		println(s"LocalActor received message: '$msg'")
	case Nodes(latency, cpuClock, battery, actorContext) =>
		val slaveNode = new Nodes(latency, cpuClock, battery, actorContext)
		
		//Check the number of registered nodes and add it or split into two landmarks
		x+= ((slaveNode, sender))
		
		if(x.length>slavesThreshold){
			//deploy a new actor as a master and give it half of the nodes
			//println("Split!")
			var i=0
			val limit = 5000
			implicit val timeout = Timeout(limit)

			//We assume that there won't be more than 1 slave actors per node
			//and also that a slave actor will belong to the master in the same node if it 			exists, so a slave actor registered on this master won't have a master in its device
			
			//We pick a random new node to serve as master
			//println("Message sent")
			val newMaster = if(x(0)._2 != self){ //we do not want to create another master here
				Await.result(context.actorSelection("akka.tcp://LocalSystem@127.0.0.1:5200/user/DNSActor") ? DeployActor(x(0)._2),  timeout.duration).asInstanceOf[ActorRef]
			}else{
				Await.result(context.actorSelection("akka.tcp://LocalSystem@127.0.0.1:5200/user/DNSActor") ? DeployActor(x(1)._2),  timeout.duration).asInstanceOf[ActorRef]
			}
			
			Thread.sleep(500)  //Needed because the remote deployment takes time
			for(a<-0 to x.length){
				//println("Message sent")
			}
			val toRemove = x.map{a => a._2 ? PingNewMaster(newMaster)}
			val newToR = toRemove.map(Await.result(_,timeout.duration))
			val R = x.zip(newToR).filter(_._2==true)
			R.foreach(x-=_._1)
		}
		

	case msg: String =>
		println(s"Master received message '$msg'")
	case _ =>
		println("error")
	}
}


