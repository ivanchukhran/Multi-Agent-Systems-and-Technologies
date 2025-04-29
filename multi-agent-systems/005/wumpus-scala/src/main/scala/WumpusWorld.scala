// import akka.actor.typed.ActorSystem
// import akka.actor.typed.scaladsl.Behaviors
// import actors._
// import protocols._
// import scala.concurrent.duration._
// import scala.concurrent.Await
// import scala.util.{Success, Failure}
// import akka.actor.typed.scaladsl.AskPattern._
// import scala.concurrent.ExecutionContext.Implicits.global

// object WumpusWorld extends App {

//   // Root actor behavior that creates and manages the game
//   val rootBehavior = Behaviors.setup[WumpusProtocol.Command] { context =>
//     context.log.info("Starting Wumpus World")

//     Behaviors.receiveMessage {
//       case WumpusProtocol.CreateEnvironment(replyTo) =>
//         context.log.info("Creating Environment actor")
//         val environmentRef = context.spawn(EnvironmentActor(), "environment")
//         replyTo ! WumpusProtocol.EnvironmentCreated(environmentRef)
//         Behaviors.same

//       case WumpusProtocol.CreateNavigator(replyTo) =>
//         context.log.info("Creating Navigator actor")
//         val navigatorRef = context.spawn(NavigatorActor(), "navigator")
//         replyTo ! WumpusProtocol.NavigatorCreated(navigatorRef)
//         Behaviors.same

//       case WumpusProtocol.CreateSpeleologist(
//             environmentRef,
//             navigatorRef,
//             replyTo
//           ) =>
//         context.log.info("Creating Speleologist actor")
//         val speleologistRef = context.spawn(
//           SpeleologistActor(environmentRef, navigatorRef),
//           "speleologist"
//         )
//         replyTo ! WumpusProtocol.SpeleologistCreated(speleologistRef)
//         Behaviors.same

//       case WumpusProtocol.StartGame(speleologistRef) =>
//         context.log.info("Starting the game")
//         speleologistRef ! SpeleologistProtocol.StartExploration
//         Behaviors.same

//       case WumpusProtocol.Shutdown =>
//         context.log.info("Shutting down Wumpus World")
//         Behaviors.stopped
//     }
//   }

//   // Create the actor system
//   val system = ActorSystem(rootBehavior, "wumpus-world")

//   // Set up the timeout and scheduler for ask pattern
//   implicit val timeout: akka.util.Timeout = 5.seconds
//   implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

//   // Start the game setup
//   val environmentFuture = system.ask[WumpusProtocol.EnvironmentCreated] { ref =>
//     WumpusProtocol.CreateEnvironment(ref)
//   }

//   val navigatorFuture = system.ask[WumpusProtocol.NavigatorCreated] { ref =>
//     WumpusProtocol.CreateNavigator(ref)
//   }

//   // When both environment and navigator are ready, create the speleologist
//   val setupFuture = for {
//     WumpusProtocol.EnvironmentCreated(environmentRef) <- environmentFuture
//     WumpusProtocol.NavigatorCreated(navigatorRef) <- navigatorFuture
//   } yield {
//     // Create speleologist
//     val speleologistFuture = system.ask[WumpusProtocol.SpeleologistCreated] {
//       ref =>
//         WumpusProtocol.CreateSpeleologist(environmentRef, navigatorRef, ref)
//     }

//     // Start the game when speleologist is ready
//     speleologistFuture.onComplete {
//       case Success(WumpusProtocol.SpeleologistCreated(speleologistRef)) =>
//         // Start the game
//         system ! WumpusProtocol.StartGame(speleologistRef)

//       case Failure(exception) =>
//         println(s"Failed to create speleologist: $exception")
//         system ! WumpusProtocol.Shutdown
//     }
//   }

//   setupFuture.onComplete {
//     case Success(_) =>
//       println("Game setup completed")
//     case Failure(exception) =>
//       println(s"Failed to set up the game: $exception")
//       system ! WumpusProtocol.Shutdown
//   }

//   // Add shutdown hook to gracefully terminate when user presses Ctrl+C
//   sys.addShutdownHook {
//     system ! WumpusProtocol.Shutdown
//     Await.ready(system.whenTerminated, 10.seconds)
//   }

//   // Wait for system termination
//   Await.ready(system.whenTerminated, Duration.Inf)
// }
