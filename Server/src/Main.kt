import com.CliChannel
import com.CommandDispatcher
import dal.DatabaseExecutor
import dal.DatabaseFacade
import dal.SQLiteConnection

fun main() {
    val dispatcher = CommandDispatcher(DatabaseFacade(DatabaseExecutor(SQLiteConnection("test"))))
    val cli = CliChannel(dispatcher)
    cli.start()
}
