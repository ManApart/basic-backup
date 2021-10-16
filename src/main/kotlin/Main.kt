import java.io.File


fun main(){
    val config = File("./src/main/resources/config.txt").readLines()
    val source = File(config[0])
    val destination = File(config[1])

}