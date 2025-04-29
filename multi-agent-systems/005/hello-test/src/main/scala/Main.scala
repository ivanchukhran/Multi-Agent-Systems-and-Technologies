import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props

class HelloActor(name: String) extends Actor {
  def receive = {
    case "hello" => println(s"hello from $name")
    case _       => println(s"huh? said $name")
  }
}

object Main extends App {
  val system = ActorSystem("HelloSystem")
  // default Actor constructor
  val helloActor = system.actorOf(Props(new HelloActor("Alice")), name = "helloactor")
  helloActor ! "hello"
  helloActor ! "buenos dias"
}
