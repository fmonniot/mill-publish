// build.sc
import mill._
import mill.scalalib._
import mill.scalalib.publish.{PomSettings, License, Developer, SCM}
import ammonite.ops._

object publish extends CassieModule

trait CassieModule extends SbtModule {

  override def artifactName = "mill-publish"
  def scalaVersion = "2.12.4"

  val version = "0.1.0-SNAPSHOT"

  override def scalacOptions = Seq(
    "-Ypartial-unification",
    "-deprecation",
    "-feature"
  )

  override def ivyDeps = Agg(
    ivy"com.lihaoyi::mill-dev:0.1.3-6-386cd7"
  )

  object test extends Tests {
    override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.4")

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

  def pomSettings = PomSettings(
    description = "revamped publish which explicit where you publish, and Bintray support for fun",
    organization = "eu.monniot.mill",
    url = "https://github.com/fmonniot/mill-publish",
    licenses = Seq(
      License("Apache 2.0", "https://opensource.org/licenses/Apache-2.0")
    ),
    scm = SCM(
      "git://github.com/fmonniot/mill-publish.git",
      "scm:git://github.com/fmonniot/mill-publish.git"
    ),
    developers = Seq(
      Developer("fmonniot", "François Monniot", "https://francois.monniot.eu")
    )
  )

  def publishVersion = {
    if(version.endsWith("-SNAPSHOT")) {
      import ImplicitWd._
      val commit = %%("git", "rev-parse", "HEAD").out.lines.mkString
      version.replace("SNAPSHOT", commit)
    } else version
  }

}
