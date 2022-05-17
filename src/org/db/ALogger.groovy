package org.db


class ALogger {
    static Binding binding = null

    static def setBinding(Binding binding) {
        this.binding = binding
    }

    static def println(message) {
        this.binding.out.println(message)
    }
}
