import org.scalajs.linker.interface.ModuleSplitStyle

Global / onChangedBuildSource := IgnoreSourceChanges // not working well with webpack devserver

name                     := "OutwatchExample"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"

val versions = new {
  val outwatch  = "1.0.0-RC14"
  val scalaTest = "3.2.15"
}


lazy val scalaJsMacrotaskExecutor = Seq(
  // https://github.com/scala-js/scala-js-macrotask-executor
  libraryDependencies       += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1",
  //Compile / npmDependencies += "setimmediate"  -> "1.0.5", // polyfill
)

lazy val webapp = project
  .enablePlugins(
    ScalaJSPlugin,
  )
  .settings(scalaJsMacrotaskExecutor)
  .settings(
    libraryDependencies          ++= Seq(
      "io.github.outwatch" %%% "outwatch"  % versions.outwatch,
      "org.scalatest"      %%% "scalatest" % versions.scalaTest % Test,
    ),
    scalacOptions --= Seq(
      "-Xfatal-warnings",
    ), // overwrite option from https://github.com/DavidGregory084/sbt-tpolecat

//    useYarn       := true, // Makes scalajs-bundler use yarn instead of npm
//    yarnExtraArgs += "--prefer-offline",
    scalaJSUseMainModuleInitializer   := true, // On Startup, call the main function
    //Test / requireJsDomEnv            := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("testvite")))
    },
    publicDev := linkerOutputDirectory((Compile / fastLinkJS).value).getAbsolutePath(),
    publicProd := linkerOutputDirectory((Compile / fullLinkJS).value).getAbsolutePath(),

  )

def linkerOutputDirectory(v: Attributed[org.scalajs.linker.interface.Report]): File = {
  v.get(scalaJSLinkerOutputDirectory.key).getOrElse {
    throw new MessageOnlyException(
      "Linking report was not attributed with output directory. " +
        "Please report this as a Scala.js bug.")
  }
}

val publicDev = taskKey[String]("output directory for `npm run dev`")
val publicProd = taskKey[String]("output directory for `npm run build`")
