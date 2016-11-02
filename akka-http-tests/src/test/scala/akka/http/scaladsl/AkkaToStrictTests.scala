package akka.http.scaladsl

import javax.net.ssl._
import java.security._
import java.security.{ KeyStore, SecureRandom }
import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }

import scala.concurrent._
import duration._
import akka.util._
import akka.actor._
import akka.http.impl.util.ExampleHttpContexts
import akka.stream._
import scaladsl._
import akka.http.scaladsl._
import model._
import org.scalatest._
import org.scalatest.concurrent._

class AkkaToStrictTests extends FlatSpec with ScalaFutures {
  val port = scala.util.Random.nextInt(65535 - 1025) + 1025

  implicit val system = ActorSystem("ToStrictTests")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  "Test web server" should "start" in {
    import akka.http.scaladsl.server.Directives._

    info(s"Starting test web server on port $port")

    /* Setup test route */
    val route = get {
      val promise = Promise[ByteString]()
      val ent = HttpEntity.CloseDelimited(
        ContentTypes.`text/plain(UTF-8)`,
        Source.single(ByteString("Strictness FTW!"))
          ++ Source.fromFuture(promise.future)
      )

      system.scheduler.scheduleOnce(1.second)(promise.success(ByteString.empty))

      complete(HttpResponse(StatusCodes.OK, Nil, ent))
    }

    /* Startup server */
    val bindingFuture = Http().bindAndHandle(route, "localhost", port,
      connectionContext = ExampleHttpContexts.exampleServerContext)
    bindingFuture.isReadyWithin(10.seconds)
  }

  "ToStrict" should "get strict data via https" in {
    val req = HttpRequest(HttpMethods.GET, s"https://akka.example.org:$port/")
    val resp = Http().singleRequest(req, connectionContext = ExampleHttpContexts.exampleClientContext)
    resp.isReadyWithin(10.seconds)

    /* We need a closedelimited */
    assert(resp.futureValue.entity.isInstanceOf[HttpEntity.CloseDelimited])

    /* Strictify it */
    val strict = resp.futureValue.toStrict(3.second)
    strict.isReadyWithin(10.seconds)

    strict.futureValue.entity match {
      case e: HttpEntity.Strict ⇒
        assert(e.data.decodeString("utf-8") === "Strictness FTW!")
      case _ ⇒ fail("Entity isn't strict!")
    }
  }

  "cleanup" should "kill actor system" in {
    system.terminate().isReadyWithin(10.seconds)
  }
}
