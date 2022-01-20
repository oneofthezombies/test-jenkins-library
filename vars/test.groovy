import java.lang.Boolean

@NonCPS
def call() {
  Boolean.metaClass.toYeAnio {
    delegate ? "예" : "아니오"
  }

  def a = true
  println a
  def b = a.toYeAnio()
  println b
}
