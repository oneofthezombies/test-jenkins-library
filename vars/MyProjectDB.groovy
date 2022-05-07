import groovy.sql.DataSet
import groovy.sql.Sql


static void connect(Closure query) {
    Map param = [
        url:'jdbc:mysql://127.0.0.1:3306/test',
        // user: 'root',
        // password:'hunhoekim',
        driver: 'com.mysql.cj.jdbc.Driver'
    ]
    Sql.withInstance(param, query)
}

static Integer updateMyProjectVersion(String branch) {
    Integer version = 0
    connect({ sql ->
        def result = sql.executeInsert """
            INSERT INTO TblMyProjectVersion (_branch)
            VALUES (${branch})
        """, ['_version']
        println result
        version = result[0]['GENERATED_KEY']
    })
    return version
}
