package eu.monniot.mill.publish

import ammonite.ops.{Path, home}
import mill.{T, define}
import mill.define.ExternalModule


trait PublishToBintrayModule extends PublishBaseModule {

  def bintrayCredentialsPath: T[Path] = T(home / ".bintray" / ".credentials")

  /**
    * The Bintray repository to push the module to.
    */
  def bintrayRepository: T[String] = T("maven")

  /**
    * The Bintray package name. It defaults to the module name.
    */
  def bintrayPackage: T[String] = T {
    val segments = millModuleSegments.value

    segments.head.pathSegments.head
  }


  /**
    * Publish the current project as a maven artifact to a Bintray account.
    *
    * We voluntary do not let people pass their bintray credentials from the command line.
    * Instead, we expect people to use either:
    * - Java properties (this one is from SBT and it would make sense to switch to CLI args)
    * - Environment variables (BINTRAY_USER and BINTRAY_PASS)
    * - The bintray properties file (path configurable in the build description)
    *
    * The Java properties way may be remove in the future, as it means the credentials are still
    * available in the command executed. Or we may use CLI arguments instead.
    *
    * @param release Whether the new uploaded artifact should be published as well
    * @param sign    Whether a signature file should be generated and uploaded as well
    */
  // TODO Can we use Option here instead of null to indicates an argument is optional ?
  // This would be really useful for gpg passphrase or bintray credentials
  def publishMaven(release: Boolean = true, sign: Boolean = false): define.Command[Unit] = T.command {
    val credentials = BintrayCredentials.read(bintrayCredentialsPath())
    val PublishBaseModule.PublishData(artifactInfo, artifacts) = publishArtifacts()

    new BintrayPublisher(bintrayRepository(), bintrayPackage(), credentials, T.ctx().log)
      .publishMaven(artifacts.map { case (a, b) => (a.path, b) }, artifactInfo, release, sign)
  }

}

object PublishToBintrayModule extends ExternalModule {
  def millDiscover: mill.define.Discover[this.type] = mill.define.Discover[this.type]
}
