package eu.monniot.mill.publish

import ammonite.ops.Path
import mill.{T, define}
import mill.define.{ExternalModule, Task}


/**
  * Configuration necessary for publishing a Scala module to Maven Central or similar
  */
trait PublishToSonatypeModule extends PublishBaseModule {

  import mill.scalalib.publish._

  def sonatypeUri: String = "https://oss.sonatype.org/service/local"

  def sonatypeSnapshotUri: String = "https://oss.sonatype.org/content/repositories/snapshots"

  def publish(sonatypeCreds: String,
              gpgPassphrase: String,
              release: Boolean): define.Command[Unit] = T.command {
    val PublishBaseModule.PublishData(artifactInfo, artifacts) = publishArtifacts()
    new SonatypePublisher(
      sonatypeUri,
      sonatypeSnapshotUri,
      sonatypeCreds,
      gpgPassphrase,
      T.ctx().log
    ).publish(artifacts.map { case (a, b) => (a.path, b) }, artifactInfo, release)
  }

}

object PublishToSonatypeModule extends ExternalModule {

  import mill.scalalib.publish._

  def publishAll(sonatypeCreds: String,
                 gpgPassphrase: String,
                 publishArtifacts: mill.main.Tasks[PublishBaseModule.PublishData],
                 release: Boolean = false,
                 sonatypeUri: String = "https://oss.sonatype.org/service/local",
                 sonatypeSnapshotUri: String = "https://oss.sonatype.org/content/repositories/snapshots") = T.command {

    val x: Seq[(Seq[(Path, String)], Artifact)] = Task.sequence(publishArtifacts.value)().map {
      case PublishBaseModule.PublishData(a, s) => (s.map { case (p, f) => (p.path, f) }, a)
    }
    new SonatypePublisher(
      sonatypeUri,
      sonatypeSnapshotUri,
      sonatypeCreds,
      gpgPassphrase,
      T.ctx().log
    ).publishAll(
      release,
      x: _*
    )
  }

  def millDiscover: mill.define.Discover[this.type] = mill.define.Discover[this.type]

}

