import sbt._

object PluginDef extends Build {
  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file(".")) dependsOn( paradoxPlugin )
  lazy val paradoxPlugin =
    //ProjectRef(uri("git://github.com/lightbend/paradox.git#fbcc9798c0f968f5a85a63dc45d765dfd7a70b34"), "plugin")
    ProjectRef(uri("git://github.com/jrudolph/paradox.git#w/paradox-0.2.2-with-fixed-line-breaks"), "plugin")
    //ProjectRef(file("/home/johannes/git/lightbend/paradox"), "plugin")
}
