import java.lang.Boolean

def call() {
  injectBoolean()
}

def injectBoolean() {
  Boolean.metaClass.toKorean {
    delegate ? "예" : "아니오"
  }
}
