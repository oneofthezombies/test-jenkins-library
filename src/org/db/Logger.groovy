package org.db

Binding binding = getBinding()

class ALogger {
    static def getOut() {
        return binding.out
    }
}
