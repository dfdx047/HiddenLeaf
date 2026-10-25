# The engine bridge and SDL's Java side are called from native code (JNI).
-keep class com.dfdx047.hiddenleaf.engine.** { *; }
-keep class org.libsdl.app.** { *; }
