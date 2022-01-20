import java.lang.Boolean

def call() {
  Boolean.metaClass.toYeAnio {
    delegate ? "예" : "아니오"
  }
}
