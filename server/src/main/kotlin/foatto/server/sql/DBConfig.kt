package foatto.server.sql

class DBConfig(
    val url: String,
    val login: String,
    val password: String,
) {

    companion object {
        private const val CONFIG_DB_URL = "db_url"
        private const val CONFIG_DB_LOGIN = "db_login"
        private const val CONFIG_DB_PASSWORD = "db_password"

        fun loadConfig(hmConfig: Map<String, String>): DBConfig =
            DBConfig(
                url = hmConfig[CONFIG_DB_URL]!!,
                login = hmConfig[CONFIG_DB_LOGIN]!!,
                password = hmConfig[CONFIG_DB_PASSWORD]!!,
            )
    }

}
