package org.db

Binding binding = getBinding()

class Logger {
    static def getOut() {
        return binding.out
    }
}
