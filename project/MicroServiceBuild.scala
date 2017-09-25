import sbt._

object MicroServiceBuild extends Build with MicroService {
  override val appName = "catalogue-frontend"
  override lazy val plugins: Seq[Plugins] = Seq()
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._


  private val frontendBootstrapVersion = "0.8.0"
  private val urlBuilderVersion = "1.1.0"
  private val catsVersion = "0.9.0"
  private val playReactivemongoVersion = "5.2.0"

  private val hmrcTestVersion = "2.3.0"


  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-play-25" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-config" % "4.3.0",
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.apache.httpcomponents" % "httpcore" % "4.3.2",
    "org.apache.httpcomponents" % "httpclient" % "4.3.5",
    "com.github.tototoshi" %% "scala-csv" % "1.3.4" ,
    "com.github.melrief" %% "purecsv" % "0.1.0",
    "com.opencsv" % "opencsv" % "4.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "uk.gov.hmrc" %% "reactivemongo-test" % "2.0.0" % scope,
        "org.scalatest" %% "scalatest" % "2.2.6" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,        
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.tomakehurst" % "wiremock" % "1.52" % scope,
        "org.jsoup" % "jsoup" % "1.9.2" % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

