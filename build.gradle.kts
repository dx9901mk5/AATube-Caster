// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

// Automatically redirect build outputs outside synced cloud folders / non-ASCII paths on Windows
// to prevent Google Drive file-lock collisions and speed up incremental builds.
if (rootDir.absolutePath.any { it.code > 127 }) {
  val fastBuildRoot = File(System.getProperty("java.io.tmpdir"), "AATube-Caster-build")
  layout.buildDirectory.set(File(fastBuildRoot, "root"))
  subprojects {
    layout.buildDirectory.set(File(fastBuildRoot, name))
  }
}
