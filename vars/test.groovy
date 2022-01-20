println '1'

import java.lang.Boolean

println '2'

Boolean.metaClass.toYeAnio {
  delegate ? "예" : "아니오"
}

println '3'
