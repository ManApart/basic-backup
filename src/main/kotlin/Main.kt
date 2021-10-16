import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File


fun main() {
    val config = File("./src/main/resources/config.txt").readLines()
    val source = config[0]
    val destination = config[1]

    val allFiles = File(source).getAllFiles()
    println("Found ${allFiles.size} files.")

    allFiles.chunked(100).forEach { processChunk(it, source, destination) }
}

private fun processChunk(chunk: List<File>, sourceRoot: String, destinationRoot: String) {
    runBlocking {
        val backedUp = chunk.map {
            async {
                backupFile(it, sourceRoot, destinationRoot)
            }
        }.awaitAll().count { it }
        println("Backed up $backedUp files")
    }
}

private fun backupFile(file: File, sourceRoot: String, destinationRoot: String): Boolean {
    val relativePath = file.path.substring(sourceRoot.length, file.path.length)
    val destFile = File("$destinationRoot/$relativePath")

    if (!destFile.exists() || file.lastModified() > destFile.lastModified()){
        file.copyTo(destFile, overwrite = true)
        return true
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
