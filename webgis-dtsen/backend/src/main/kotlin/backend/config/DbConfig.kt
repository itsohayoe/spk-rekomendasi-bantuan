
object DbConfig {
    private val config = HikariConfig().apply {
        jdbcUrl = System.getenv("DB_URL")
            ?: error("DB_URL belum diatur. Silakan konfigurasi environment variable.")

        username = System.getenv("DB_USER")
            ?: error("DB_USER belum diatur. Silakan konfigurasi environment variable.")

        password = System.getenv("DB_PASSWORD")
            ?: error("DB_PASSWORD belum diatur. Silakan konfigurasi environment variable.")

        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 30000
        maxLifetime = 1800000
        connectionTimeout = 30000
    }

    val dataSource: DataSource = HikariDataSource(config)
}
