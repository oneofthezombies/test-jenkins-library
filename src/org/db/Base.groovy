import groovy.sql.Sql
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration
import org.jenkinsci.plugins.database.Database


class Base {
    Sql connect() {
        Database db = GlobalDatabaseConfiguration.get().getDatabase()
        if (db == null) {
            throw new Exception('check your GlobalDatabaseConfiguration with Database plugin.')
        }
        String jdbcUrl = db.getJdbcUrl()
        String driverClass = db.getDriverClass()
        getBinding().out.println "DB.connect() JDBC URL: [${jdbcUrl}] DriverClass: [${driverClass}]"
        return new Sql(db.getDataSource())
    }
}
