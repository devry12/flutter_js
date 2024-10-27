package io.abner.flutter_js

class JsBridge(config: JsBridgeConfig) {
    // Implementation of JsBridge

    fun registerErrorListener(listener: ErrorListener) {
        // Register error listener
    }

    fun evaluateBlocking(script: String, wrapperClass: Class<JsonObjectWrapper>): JsonObjectWrapper {
        // Evaluate the script and return result
        return JsonObjectWrapper() // Replace with actual implementation
    }

    fun release() {
        // Release resources
    }

    interface ErrorListener {
        fun onError(error: JsBridgeError)
    }
}

class JsBridgeConfig {
    companion object {
        fun standardConfig(): JsBridgeConfig {
            return JsBridgeConfig()
        }
    }
}

class JsBridgeError {
    fun errorString(): String {
        return "Some error occurred"
    }
}
