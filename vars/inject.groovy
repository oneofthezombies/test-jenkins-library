import java.lang.Boolean

@NonCPS
def call() {
  Boolean.metaClass.toKorean {
    delegate ? "예" : "아니오"
  }
}
