package org.db

// Binding binding = getBinding()

class ALogger {
    static ALogger self = null
    static Binding binding = null

    ALogger() {
        // this.binding = binding
    }

    static def get() {
        if (!self) {
            self = new ALogger()
        }
        return self
    }
}
