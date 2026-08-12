import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

abstract class BumpVersionTask : DefaultTask() {
    @get:InputFile
    abstract val versionFile: RegularFileProperty

    @TaskAction
    fun bump() {
        val file = versionFile.get().asFile
        val props = Properties()
        if (file.exists()) file.inputStream().use { props.load(it) }

        val phone = props.getProperty("phoneVersionCode", "20").toInt()
        val wear = props.getProperty("wearVersionCode", "21").toInt()

        val nextPhone = phone + 2
        val nextWear = wear + 2

        props.setProperty("phoneVersionCode", nextPhone.toString())
        props.setProperty("wearVersionCode", nextWear.toString())

        file.outputStream().use { props.store(it, "Updated release version codes") }
        logger.lifecycle("Auto-bumped release version codes: Phone -> $nextPhone, Wear -> $nextWear")
    }
}

tasks.register<BumpVersionTask>("bumpReleaseVersion") {
    group = "versioning"
    description = "Increments phone and wear version codes in version.properties for Google Play releases"
    versionFile.set(layout.projectDirectory.file("version.properties"))
}

tasks.register<Copy>("collectReleases") {
    group = "release"
    description = "Collects release bundles from app and wear modules into a single releases/ folder"

    val timestamp = SimpleDateFormat("yyyyMMdd-HHmm").format(Date())
    val rootDir = layout.projectDirectory.asFile
    val destinationDir = File(rootDir, "releases/release-$timestamp")
    val vFile = File(rootDir, "version.properties")
    val notesFile = File(rootDir, "docs/RELEASE_NOTES.txt")
    val roadmapFile = File(rootDir, "docs/PUBLIC_ROADMAP.md")

    dependsOn(":app:bundleRelease")
    dependsOn(":wear:bundleRelease")

    from("app/build/outputs/bundle/release") {

        include("app-release.aab")
    }
    from("wear/build/outputs/bundle/release") {
        include("wear-release.aab")
    }

    into(destinationDir)

    doLast {
        val versionProps = Properties()
        if (vFile.exists()) vFile.inputStream().use { versionProps.load(it) }

        val name = versionProps.getProperty("versionName", "1.0.0")
        val code = versionProps.getProperty("phoneVersionCode", "1")

        // 1. Check docs/RELEASE_NOTES.txt
        var notesList = if (notesFile.exists()) {
            notesFile.readLines().map { it.trim() }.filter { it.isNotBlank() }
        } else emptyList()

        // 2. Fallback to docs/PUBLIC_ROADMAP.md finished features
        if (notesList.isEmpty()) {
            if (roadmapFile.exists()) {
                val roadmapText = roadmapFile.readText()
                val extracted = mutableListOf<String>()
                val sections = roadmapText.split("### ")
                for (section in sections) {
                    if (section.contains("**Status: Finished**")) {
                        val lines = section.lines()
                        val whatChangedIndex = lines.indexOfFirst { it.trim().startsWith("What changed:") }
                        if (whatChangedIndex != -1) {
                            lines.drop(whatChangedIndex + 1)
                                .takeWhile { it.trim().startsWith("-") }
                                .forEach { extracted.add(it.trim()) }
                        }
                    }
                }
                if (extracted.isNotEmpty()) {
                    notesList = extracted.take(6)
                }
            }
        }

        // 3. Fallback to git log
        if (notesList.isEmpty()) {
            notesList = try {
                val process = ProcessBuilder("git", "log", "-n", "8", "--pretty=format:- %s").start()
                process.inputStream.bufferedReader().readLines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.lowercase().contains("merge") }
                    .take(5)
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (notesList.isEmpty()) {
            notesList = listOf(
                "- New workout import system (Hevy API & CSV support)",
                "- Chest opening reveal animations and loot polish",
                "- Compare-to-equipped stat deltas and locked tier messaging",
                "- General performance improvements and bug fixes"
            )
        }

        var formattedNotes = notesList.joinToString("\n")
        if (formattedNotes.length > 490) {
            val truncated = StringBuilder()
            for (line in notesList) {
                if ((truncated.length + line.length + 1) <= 490) {
                    if (truncated.isNotEmpty()) truncated.append("\n")
                    truncated.append(line)
                } else break
            }
            formattedNotes = truncated.toString()
        }

        val infoFile = File(destinationDir, "release_info.artifact.md")
        infoFile.writeText("""
            # Release Metadata: v$name (Build $code)

            ## 📋 Google Play Store Release Notes (${formattedNotes.length}/500 chars)

            Copy and paste the block below into Google Play Console (Release Notes / <en-US>):

            ```text
            <en-US>
            $formattedNotes
            </en-US>
            ```

            ## 📦 Release Artifacts
            - `app-release.aab` (Android Phone Bundle)
            - `wear-release.aab` (Wear OS Watch Bundle)
            - Created At: $timestamp
        """.trimIndent())

        logger.lifecycle("Releases collected in: ${destinationDir.absolutePath}")
        logger.lifecycle("Release info generated: ${infoFile.absolutePath}")

        // Auto-prune old release folders beyond the 3 latest to prevent disk bloat
        val releasesParent = File(rootDir, "releases")
        val oldReleases = releasesParent.listFiles { f -> f.isDirectory && f.name.startsWith("release-") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(3) ?: emptyList()

        for (oldFolder in oldReleases) {
            oldFolder.deleteRecursively()
            logger.lifecycle("Pruned old release folder to save storage: ${oldFolder.name}")
        }
    }
}
