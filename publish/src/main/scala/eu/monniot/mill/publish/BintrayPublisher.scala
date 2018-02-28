package eu.monniot.mill.publish

import java.math.BigInteger
import java.security.MessageDigest

import ammonite.ops.{%, Path, read}
import mill.util.Logger
import scalaj.http.HttpResponse


class BintrayPublisher(repository: String,
                       pkg: String,
                       credentials: BintrayCredentials,
                       log: Logger) {

  import mill.scalalib.publish._

  private val api = new BintrayHttpApi(repository, pkg, credentials, log)

  // Publish maven style the given artifact
  def publishMaven(fileMapping: Seq[(Path, String)], artifact: Artifact, release: Boolean, sign: Boolean): Unit =
    publishMavenAll(release, sign, fileMapping -> artifact)

  def publishMavenAll(release: Boolean, sign: Boolean, artifacts: (Seq[(Path, String)], Artifact)*): Unit = {
    val mappings = for ((mapping, artifact) <- artifacts) yield {
      val publishPath = Seq(
        artifact.group.replace(".", "/"),
        artifact.id,
        artifact.version
      ).mkString("/")

      log.info(s"Creating mapping for $publishPath")

      val fileMapping = mapping.map { case (file, name) => (file, publishPath + "/" + name) }

      val signedArtifacts = fileMapping ++ fileMapping.collect {
        case (file, name) if sign => poorMansSign(file, credentials.gpgPassphrase) -> s"$name.asc"
      }

      artifact -> signedArtifacts.flatMap { case (path, name) =>

        val content = read.bytes(path)

        Seq(
          name -> content,
          (name + ".md5") -> md5hex(content),
          (name + ".sha1") -> sha1hex(content)
        )
      }
    }

    val results = mappings.map { case (artifact, payloads) =>
      val uploadResults = payloads.map { case (fileName, data) =>
        log.info(s"Uploading $fileName to bintray")
        api.mavenUpload(fileName, data)
      }

      val publishResult = if (release) {
        log.info(s"Releasing $pkg version ${artifact.version} on Bintray")
        Option(api.publish(artifact.version))
      } else None

      artifact -> (uploadResults ++ publishResult)
    }

    reportResults(results)
  }

  private def reportResults(results: Seq[(Artifact, Seq[HttpResponse[String]])]): Unit = {

    val (ok, failed) = results.partition(_._2.forall(_.is2xx))

    if (ok.nonEmpty) {
      log.info(s"Published ${ok.map(_._1.id).mkString(", ")} to Bintray")
    }
    if (failed.nonEmpty) {
      throw new RuntimeException(
        failed
          .map { case (artifact, responses) =>
            val errors = responses.filterNot(_.is2xx).map { response =>
              s"Code: ${response.code}, message: ${response.body}"
            }

            s"Failed to publish ${artifact.id} to Bintray. Errors: \n${errors.mkString("\n")}"
          }
          .mkString("\n")
      )
    }
  }

  // http://central.sonatype.org/pages/working-with-pgp-signatures.html#signing-a-file
  // Assuming it's the same for Bintray, as they let you sync to Maven Central afterwards
  // If needed we can offer the possibility of doing it via the Bintray API
  private def poorMansSign(file: Path, passphrase: Option[String]): Path = {
    val fileName = file.toString
    import ammonite.ops.ImplicitWd._

    val args = List("gpg", "--yes", "-a", "-b", "--batch") ++
      passphrase.map(pp => List("--passphrase", pp)).getOrElse(List.empty) :+
      fileName

    %(args)
    Path(fileName + ".asc")
  }

  private def md5hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(md5.digest(bytes)).getBytes

  private def sha1hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(sha1.digest(bytes)).getBytes

  private def md5 = MessageDigest.getInstance("md5")

  private def sha1 = MessageDigest.getInstance("sha1")

  private def hexArray(arr: Array[Byte]) =
    String.format("%0" + (arr.length << 1) + "x", new BigInteger(1, arr))

}