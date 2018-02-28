package eu.monniot.mill.publish

import mill.scalalib.publish.PatientHttp
import mill.util.Logger
import scalaj.http.{HttpOptions, HttpResponse}

import scala.concurrent.duration._


class BintrayHttpApi(bintrayRepository: String,
                     bintrayPackage: String,
                     credentials: BintrayCredentials,
                     log: Logger) {

  private val subject = credentials.user

  private val baseUri = "https://bintray.com/api/v1"

  private val uploadTimeout = 5.minutes.toMillis.toInt

  // PUT /maven/:subject/:repo/:package/:file_path[;publish=0/1]
  def mavenUpload(filePath: String,
                  data: Array[Byte]): HttpResponse[String] = {
    val uri = s"$baseUri/maven/$subject/$bintrayRepository/$bintrayPackage/$filePath"

    PatientHttp(uri)
      .option(HttpOptions.readTimeout(uploadTimeout))
      .method("PUT")
      .auth(credentials.user, credentials.pass)
      .header("Content-Type", "application/binary")
      .put(data)
      .asString
  }


  // POST /content/:subject/:repo/:package/:version/publish
  def publish(version: String): HttpResponse[String] = {
    val uri = s"$baseUri/content/$subject/$bintrayRepository/$bintrayPackage/$version/publish"

    PatientHttp(uri)
      .option(HttpOptions.readTimeout(uploadTimeout))
      .method("POST")
      .auth(credentials.user, credentials.pass)
      .header("Content-Type", "application/json")
      .asString
  }
}
