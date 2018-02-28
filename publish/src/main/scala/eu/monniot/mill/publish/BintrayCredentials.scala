package eu.monniot.mill.publish

import ammonite.ops.Path

import scala.util.Try

case class BintrayCredentials(user: String, pass: String, gpgPassphrase: Option[String] = None)

object BintrayCredentials {

  def read(path: Path): BintrayCredentials =
    propsCredentials
      .orElse(envCredentials)
      .orElse(fileCredentials(path))
      .getOrElse(throw new IllegalStateException("Publishing to bintray requires you to pass a username and api key"))

  private def propsCredentials =
    for {
      name <- sys.props.get("bintray.user")
      pass <- sys.props.get("bintray.pass")
      gpg = sys.props.get("gpg.passphrase")
    } yield BintrayCredentials(name, pass, gpg)

  private def envCredentials =
    for {
      name <- sys.env.get("BINTRAY_USER")
      pass <- sys.env.get("BINTRAY_PASS")
      gpg = sys.env.get("GPG_PASSPHRASE")
    } yield BintrayCredentials(name, pass, gpg)

  private def fileCredentials(path: Path) = {
    path match {
      case creds if creds.toIO.exists() =>
        for {
          mapped <- readPropertiesFile(path)
          user <- mapped.get("user")
          pass <- mapped.get("password")
          gpg = mapped.get("gpg.passphrase")
        } yield BintrayCredentials(user, pass, gpg)

      case _ => None
    }
  }

  private def readPropertiesFile(creds: Path) = Try {
    import scala.collection.JavaConverters._

    val props = new java.util.Properties
    props.load(ammonite.ops.read.getInputStream(creds))

    props.asScala.map {
      case (k, v) => (k.toString, v.toString.trim)
    }.toMap
  }.toOption
}
