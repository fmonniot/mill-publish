package eu.monniot.mill.publish

import ammonite.ops._
import mill.{T, define}
import mill.define.Task
import mill.eval.PathRef
import mill.scalalib.{Lib, ScalaModule}
import mill.scalalib.publish.Artifact


/**
  * Configuration necessary for publishing a Scala module.
  * This is taken as is from the mill source, with some changes:
  * - the publish method extracted to PublishTo* traits
  * - the publishLocal method renamed into publishIvyLocal
  * - added a publishMavenLocal command
  */
trait PublishBaseModule extends ScalaModule {

  import mill.scalalib.publish._

  override def moduleDeps = Seq.empty[PublishBaseModule]

  def pomSettings: T[PomSettings]

  def publishVersion: T[String]

  def artifactId: T[String] = T {
    s"${artifactName()}${artifactSuffix()}"
  }

  def publishSelfDependency = T {
    Artifact(pomSettings().organization, artifactId(), publishVersion())
  }

  def publishXmlDeps = T.task {
    val ivyPomDeps = ivyDeps().map(
      Artifact.fromDep(_, scalaVersion(), Lib.scalaBinaryVersion(scalaVersion()))
    )
    val modulePomDeps = Task.sequence(moduleDeps.map(_.publishSelfDependency))()
    ivyPomDeps ++ modulePomDeps.map(Dependency(_, Scope.Compile))
  }

  def pom = T {
    val pom = Pom(artifactMetadata(), publishXmlDeps(), artifactId(), pomSettings())
    val pomPath = T.ctx().dest / s"${artifactId()}-${publishVersion()}.pom"
    write.over(pomPath, pom)
    PathRef(pomPath)
  }

  def ivy = T {
    val ivy = Ivy(artifactMetadata(), publishXmlDeps())
    val ivyPath = T.ctx().dest / "ivy.xml"
    write.over(ivyPath, ivy)
    PathRef(ivyPath)
  }

  def artifactMetadata: T[Artifact] = T {
    Artifact(pomSettings().organization, artifactId(), publishVersion())
  }

  def publishArtifacts = T {
    val baseName = s"${artifactId()}-${publishVersion()}"
    PublishBaseModule.PublishData(
      artifactMetadata(),
      Seq(
        jar() -> s"$baseName.jar",
        sourceJar() -> s"$baseName-sources.jar",
        docJar() -> s"$baseName-javadoc.jar",
        pom() -> s"$baseName.pom"
      )
    )
  }

  def publishIvyLocal(): define.Command[Unit] = T.command {
    LocalPublisher.publish(
      jar = jar().path,
      sourcesJar = sourceJar().path,
      docJar = docJar().path,
      pom = pom().path,
      ivy = ivy().path,
      artifact = artifactMetadata()
    )
  }

  def publishMavenLocal(): define.Command[Unit] = T.command {
    // TODO
  }

}

object PublishBaseModule {

  case class PublishData(meta: Artifact, payload: Seq[(PathRef, String)])

  object PublishData {
    implicit def jsonify: upickle.default.ReadWriter[PublishData] = upickle.default.macroRW
  }

}