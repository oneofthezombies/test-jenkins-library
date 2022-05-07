import groovy.sql.DataSet
import groovy.sql.Sql
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration


static def connect() {
    def db = GlobalDatabaseConfiguration.get().getDatabase()
    String jdbcUrl = db.getJdbcUrl()
    String driverClass = db.getDriverClass()
    println "DB JDBC URL: [${jdbcUrl}] DriverClass: [${driverClass}]"
    return new Sql(db.getDataSource())
}

static Integer updateMyProjectVersion(String branch) {
    Integer version = 0
    def sql = connect()
    def result = sql.executeInsert """
        INSERT INTO TblMyProjectVersion (_branch)
        VALUES (${branch})
    """, ['_version']
    println result
    version = result[0]['GENERATED_KEY']
    return version
}
