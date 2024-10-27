package io.abner.flutter_js

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class FlutterJsPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private lateinit var channel: MethodChannel
    private val jsEngineMap = mutableMapOf<Int, JSEngine>()
    private var engineIdCounter = 0

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "flutter_js")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "createEngine" -> {
                val engine = JSEngine(binding.applicationContext)
                jsEngineMap[++engineIdCounter] = engine
                result.success(engineIdCounter)
            }
            "evaluate" -> {
                val jsCommand: String = call.argument<String>("command")!!
                val engineId: Int = call.argument<Int>("engineId")!!

                jsEngineMap[engineId]?.let { engine ->
                    try {
                        val resultJS = engine.eval(jsCommand)
                        result.success(resultJS)
                    } catch (e: Exception) {
                        result.error("FlutterJSException", e.message, null)
                    }
                } ?: result.error("EngineNotFound", "Engine with id $engineId not found", null)
            }
            "releaseEngine" -> {
                val engineId: Int = call.argument<Int>("engineId")!!
                jsEngineMap[engineId]?.release()
                jsEngineMap.remove(engineId)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // Cleanup if necessary
    }
}
