import java.lang.Boolean

class Boolean {
  def toYeAnio() {
    delegate ? "예" : "아니오"
  }
}
Boolean.metaClass.toYeAnio {
  delegate ? "예" : "아니오"
}
