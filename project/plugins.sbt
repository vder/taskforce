addSbtPlugin("io.spray"       % "sbt-revolver"        % "0.10.0")
//TODO: flyway-sbt v10.0.0+ wont work some problems with jsbc driver
addSbtPlugin("com.github.sbt" % "flyway-sbt"          % "10.21.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.10.4")
addSbtPlugin("ch.epfl.scala"  % "sbt-scalafix"        % "0.9.34")
//TODO: check how to properly configure  sbt-github-actions
//addSbtPlugin("com.github.sbt"        % "sbt-github-actions"  % "0.24.0")
addSbtPlugin("org.typelevel"  % "sbt-tpolecat"        % "0.5.0")
addDependencyTreePlugin
