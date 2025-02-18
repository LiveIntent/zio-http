import zhttp.http._
import zhttp.service._
import zhttp.service.server.ServerChannelFactory
import zio._

object HelloWorldAdvanced extends App {
  // Set a port
  private val PORT = 8090

  private val fooBar = HttpApp.collect {
    case Method.GET -> Root / "foo" => Response.text("bar")
    case Method.GET -> Root / "bar" => Response.text("foo")
  }

  private val app = HttpApp.collectM {
    case Method.GET -> Root / "random" => random.nextString(10).map(Response.text)
    case Method.GET -> Root / "utc"    => clock.currentDateTime.map(s => Response.text(s.toString))
  }

  private val server =
    Server.port(PORT) ++              // Setup port
      Server.paranoidLeakDetection ++ // Paranoid leak detection (affects performance)
      Server.app(fooBar +++ app)      // Setup the Http app

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    // Configure thread count using CLI
    val nThreads: Int = args.headOption.flatMap(_.toIntOption).getOrElse(0)

    // Create a new server
    server.make
      .use(_ =>
        // Waiting for the server to start
        console.putStrLn(s"Server started on port $PORT")

        // Ensures the server doesn't die after printing
          *> ZIO.never,
      )
      .provideCustomLayer(ServerChannelFactory.auto ++ EventLoopGroup.auto(nThreads))
      .exitCode
  }
}
