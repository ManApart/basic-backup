import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.Exception

private const val CHUNK_SIZE = 100

fun main() {
    parseConfig().forEach { (source, destination) ->
        println("Backing up $source to $destination")
        val allFiles = File(source).getAllFiles()
        println("Found ${allFiles.size} source files.")

        val backedUp = allFiles.chunked(CHUNK_SIZE).sumOf { processChunk(it, source, destination) }
        println("Backed up $backedUp files.")
    }
}

private fun parseConfig(): List<Pair<String, String>> {
    val config = File("./src/main/resources/config.txt").readLines().filter { it.isNotBlank() }
    if (config.size % 2 != 0) throw Exception("Must have an even number of lines. Do you have one destination per source?")
    return config.chunked(2).map { Pair(it.first(), it.last()) }
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

private fun File.getAllFiles(): List<File> {
    return if (isDirectory) {
        listFiles()!!.flatMap { it.getAllFiles() }
    } else {
        listOf(this)
    }
}
