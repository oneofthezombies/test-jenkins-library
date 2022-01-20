import java.lang.Boolean

Boolean.metaClass.toYeAnio {
  delegate ? "예" : "아니오"
}
