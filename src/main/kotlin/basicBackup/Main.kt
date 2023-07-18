@file:JvmName("basic-backup")

import basicBackup.Configs
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.awt.SystemColor.text
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.Exception

private const val CHUNK_SIZE = 100
private const val MAX_FILES_PER_FOLDER = 2000
val log = BufferedWriter(FileWriter("./log.txt"))

fun main() {
    val config = Json.decodeFromString<Configs>(File("./src/main/resources/config.json").readText())
    config.configs.forEach {
        with(it) {
            println("Backing up $source to $destination")
            val start = System.currentTimeMillis()
            val allFiles = File(source).getFilesThoroughly(source, destination, exclusions)
            println("Found ${allFiles.size} source files in ${System.currentTimeMillis() - start}.")

            val backedUp = allFiles.chunked(CHUNK_SIZE).sumOf { processChunk(it, source, destination) }
            println("Backed up $backedUp files.")
        }
    }
    log.close()
}

private fun log(text: String){
    log.write(text + "\n")
}

private fun processChunk(chunk: List<File>, sourceRoot: String, destinationRoot: String): Int {
    return runBlocking {
        val backedUp = chunk.map {
            async {
                backupFile(it, sourceRoot, destinationRoot)
            }
        }.awaitAll().count { it }
        println("Backed up $backedUp/${chunk.size} files. Eg: ${chunk.first().path}")
        return@runBlocking backedUp
    }
}

private fun backupFile(file: File, sourceRoot: String, destinationRoot: String): Boolean {
    val relativePath = file.path.substring(sourceRoot.length, file.path.length)
    val destFile = File("$destinationRoot/$relativePath")

    if (!destFile.exists() || file.lastModified() > destFile.lastModified()) {
        try {
            file.parentFile.mkdirs()
            file.copyTo(destFile, overwrite = true)
            return true
        } catch (ex: Exception) {
            println("Failed to backup ${file.path}")
            println(ex.message ?: ex)
        }
    }
    return false
}

//Only checks one folder deep for modified
private fun File.getFilesQuickly(sourceRoot: String, destinationRoot: String, exclusions: Set<String>): List<File> {
    return if (isDirectory) {
        val relativePath = path.substring(sourceRoot.length, path.length)
        val destFile = File("$destinationRoot/$relativePath")
        if (lastModified() > destFile.lastModified()) {
            listFiles()!!.flatMap { it.getFilesQuickly(sourceRoot, destinationRoot, exclusions) }
        } else {
            println("Skipping ${this.absolutePath}")
            listOf()
        }
    } else {
        listOf(this)
    }
}

//Checks every file all the way down
private fun File.getFilesThoroughly(sourceRoot: String, destinationRoot: String, exclusions: Set<String>): List<File> {
    val relativePath = path.substring(sourceRoot.length, path.length)
    val destFile = File("$destinationRoot/$relativePath")
    return when {
        name.startsWith(".") -> listOf()
        exclusions.contains(path) -> listOf()
        isDirectory -> {
            if (listFiles()!!.size > 2000) {
                println("Skipping $path because it has over $MAX_FILES_PER_FOLDER files")
                listOf()
            } else {
                log(path)
                listFiles()!!.flatMap { it.getFilesThoroughly(sourceRoot, destinationRoot, exclusions) }
            }
        }

        lastModified() > destFile.lastModified() -> listOf(this)
        else -> listOf()
    }
}

private fun File.getFilesWithWalk(sourceRoot: String, destinationRoot: String, exclusions: Set<String>): List<File> {
    val folders = mutableSetOf<String>()
    return walk()
        .filter { !it.isDirectory }
        .filter { !it.name.startsWith(".") }
        .filter { !exclusions.contains(it.parent) }
        .filter { file ->
            val relativePath = file.path.substring(sourceRoot.length, file.path.length)
            val destFile = File("$destinationRoot/$relativePath")
            if (!folders.contains(file.parent)) {
                folders.add(file.parent)
                log(file.parent)
            }
            file.lastModified() > destFile.lastModified()
        }.toList()
}
