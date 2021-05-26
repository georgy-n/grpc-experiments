import java.io.File.separator

name := "experiment"

version := "0.1"

scalaVersion := "2.12.13"
organization in ThisBuild := "ru.meetup"

val exclusions = new {
  val findBugs       = ExclusionRule(organization = "com.google.code.findbugs")
  val nettyHandler   = ExclusionRule(organization = "io.netty", name = "netty-handler")
  val zioCore        = ExclusionRule(organization = "dev.zio", name = "zio_2.12")
  val zioInteropCats = ExclusionRule(organization = "dev.zio", name = "zio-interop-cats_2.12")
  val catsCore       = ExclusionRule(organization = "org.typelevel", name = "cats-core_2.12")
}
val versions = new {
  val cats = new {
    val core   = "2.1.1"
    val effect = "2.1.2"
  }

  val zio = new {
    val main    = "1.0.1"
    val cats    = "2.1.4.0"
    val twitter = "20.8.0.0"
  }
}

val protoSrc = project.in(file("proto-sources"))
  .settings(
    version := "0.1.0",
    name := "proto-src",
    buildInfoPackage := "ru.meetup.proto-src",
    crossPaths := false,
    unmanagedResourceDirectories in Compile += { baseDirectory.value / "src" / "main" / "protobuf" }
  )

val proto = project.in(file("proto-generated"))
  .settings(
    name := "proto-generated",
    libraryDependencies += ("ru.meetup" % "proto-src" % "0.1.0" % "protobuf").changing(),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime"      % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "io.grpc"               % "grpc-netty-shaded"    % scalapb.compiler.Version.grpcJavaVersion
    ).map(_.excludeAll(exclusions.findBugs)),
    PB.protoSources in Compile += target.value / "protobuf_external",
    includeFilter in PB.generate := new SimpleFileFilter((f: File) =>
      f.getParent.contains(s"ru${separator}meetup") && f.isFile
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
    )
  )

lazy val server = project.in(file("server"))
  .settings(
    scalacOptions += "-Ypartial-unification",
    libraryDependencies += "org.typelevel" %% "cats-core"       % "2.1.1",
    libraryDependencies += "org.typelevel" %% "cats-effect"     % "2.1.2",
    libraryDependencies += "com.beachape"  %% "enumeratum"      % "1.6.1",
    libraryDependencies += "com.beachape"  %% "enumeratum-cats" % "1.6.1",
    libraryDependencies += "io.grpc"        % "grpc-services"   % scalapb.compiler.Version.grpcJavaVersion,

    // zio
    libraryDependencies +="dev.zio" %% "zio"                 % versions.zio.main,
    libraryDependencies +="dev.zio" %% "zio-interop-cats"    % versions.zio.cats,
    libraryDependencies +="dev.zio" %% "zio-interop-twitter" % versions.zio.twitter excludeAll exclusions.zioCore,
    libraryDependencies ++= Seq(

    "org.slf4j" % "slf4j-api" % "1.7.28",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    )
  )
  .aggregate(proto)
  .dependsOn(proto)

lazy val client =
  project.in(file("client"))
    .settings(
      scalacOptions += "-Ypartial-unification",
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % "2.1.1",
        "org.typelevel" %% "cats-effect"     % "2.1.2",

        "dev.zio" %% "zio"                 % versions.zio.main,
        "dev.zio" %% "zio-interop-cats"    % versions.zio.cats,
        "dev.zio" %% "zio-interop-twitter" % versions.zio.twitter excludeAll exclusions.zioCore,

         "com.beachape"  %% "enumeratum"      % "1.6.1",
         "com.beachape"  %% "enumeratum-cats" % "1.6.1",

      ),
      libraryDependencies ++= Seq(

        "org.slf4j" % "slf4j-api" % "1.7.28",
        "ch.qos.logback" % "logback-classic" % "1.2.3",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      )
    )
    .aggregate(proto)
    .dependsOn(proto)