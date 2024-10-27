package io.abner.flutter_js

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

data class MethodChannelResult(val success: Boolean, val data: Any? = null)

/** FlutterJsPlugin */
class FlutterJsPlugin : FlutterPlugin, MethodCallHandler {
    private var applicationContext: android.content.Context? = null
    private var methodChannel: MethodChannel? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)
    }

    private fun onAttachedToEngine(applicationContext: android.content.Context, messenger: BinaryMessenger) {
        this.applicationContext = applicationContext
        methodChannel = MethodChannel(messenger, "io.abner.flutter_js")
        methodChannel!!.setMethodCallHandler(this)
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = FlutterJsPlugin()
            instance.onAttachedToEngine(registrar.context(), registrar.messenger())
        }

        var jsEngineMap = mutableMapOf<Int, JSEngine>()
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${Build.VERSION.RELEASE}")
            }
            "initEngine" -> {
                val engineId = call.arguments as? Int ?: -1
                if (engineId >= 0) {
                    jsEngineMap[engineId] = JSEngine(applicationContext!!)
                    result.success(mapOf("engineId" to engineId))
                } else {
                    result.error("InvalidEngineId", "Engine ID must be a non-negative integer.", null)
                }
            }
            "evaluate" -> {
                val jsCommand: String = call.argument<String>("command") ?: ""
                val engineId: Int = call.argument<Int>("engineId") ?: -1
                if (jsEngineMap.containsKey(engineId)) {
                    Thread {
                        try {
                            val resultJS = jsEngineMap[engineId]!!.eval(jsCommand)
                            Handler(Looper.getMainLooper()).post { result.success(resultJS.toString()) }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post { result.error("FlutterJSException", e.message, null) }
                        }
                    }.start()
                } else {
                    result.error("EngineNotFound", "Engine with ID $engineId not found.", null)
                }
            }
            "registerChannel" -> {
                val engineId: Int = call.argument<Int>("engineId") ?: -1
                val channelName: String = call.argument<String>("channelName") ?: ""
                if (jsEngineMap.containsKey(engineId)) {
                    val jsEngine = jsEngineMap[engineId]!!
                    jsEngine.registerChannel(channelName) { message ->
                        Handler(Looper.getMainLooper()).post {
                            methodChannel?.invokeMethod("sendMessage", listOf(engineId, channelName, message))
                        }
                        "OK"
                    }
                } else {
                    result.error("EngineNotFound", "Engine with ID $engineId not found.", null)
                }
            }
            "close" -> {
                val engineId: Int = call.argument<Int>("engineId") ?: -1
                if (jsEngineMap.containsKey(engineId)) {
                    jsEngineMap[engineId]?.release()
                    jsEngineMap.remove(engineId)
                    result.success("Engine $engineId closed")
                } else {
                    result.error("EngineNotFound", "Engine with ID $engineId not found.", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        jsEngineMap.values.forEach { it.release() }
        jsEngineMap.clear()
    }
}
