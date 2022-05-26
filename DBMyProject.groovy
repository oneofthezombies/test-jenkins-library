import groovy.sql.DataSet
import groovy.sql.Sql


static Integer updateMyProjectVersion(String branch) {
    Integer version = 0
    def sql = db.Common.connect()
    def result = sql.executeInsert """
        INSERT INTO TblMyProjectVersion (_branch)
        VALUES (${branch})
    """, ['_version']
    getBinding().out.println "db insert reuslt: [${result}]"
    version = result[0]['GENERATED_KEY']
    return version
}
