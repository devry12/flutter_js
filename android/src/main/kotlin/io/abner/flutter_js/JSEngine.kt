package io.abner.flutter_js

import android.content.Context
import android.util.Log
import java.util.*

class JSEngine(context: Context) {

    private var runtime: JsBridge = JsBridge(JsBridgeConfig.standardConfig())
    private var messageChannelMap = mutableMapOf<String, (message: String) -> String>()

    init {
        val errorListener = object : JsBridge.ErrorListener() {
            override fun onError(error: JsBridgeError) {
                Log.e("JSEngine", error.errorString())
            }
        }
        runtime.registerErrorListener(errorListener)

        // Register a UUID function in JS
        JsValue.fromNativeFunction0(runtime) {
            UUID.randomUUID().toString()
        }.assignToGlobal("FLUTTERJS_getUUID")

        // Register a message sending function in JS
        JsValue.fromNativeFunction2(runtime) { channelName: String, message: String ->
            try {
                messageChannelMap[channelName]?.invoke(message)
                "$channelName:$message"
            } catch (e: Exception) {
                e.message ?: "Error"
            }
        }.assignToGlobal("FLUTTERJS_sendMessage")

        // Initialize pending messages in JS
        runtime.evaluateBlocking(
            """
            var FLUTTERJS_pendingMessages = {};
            function sendMessage(channel, message) {
                var idMessage = FLUTTERJS_getUUID();
                return new Promise((resolve, reject) => {
                    FLUTTERJS_pendingMessages[idMessage] = { 
                        resolve: (v) => { resolve(v); return v;}, 
                        reject: reject
                    };
                    FLUTTERJS_sendMessage(channel, JSON.stringify({ id: idMessage, message: message }));
                });
            }
            """.trimIndent(),
            JsonObjectWrapper::class.java
        )
    }

    fun registerChannel(channelName: String, channelFn: (message: String) -> String) {
        messageChannelMap[channelName] = channelFn
    }

    fun eval(script: String): JsonObjectWrapper {
        return runtime.evaluateBlocking(script, JsonObjectWrapper::class.java) as JsonObjectWrapper
    }

    fun release() {
        runtime.release()
    }
}
