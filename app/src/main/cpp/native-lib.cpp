#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "NativeDroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativeapp_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    LOGI("Calling native C++ method stringFromJNI for %s", "NativeCppApp");
    std::string hello = "Hello from Native C++ (NDK Core) in NativeCppApp!";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_nativeapp_MainActivity_calculateFactorial(
        JNIEnv* env,
        jobject /* this */,
        jint n) {
    if (n <= 1) return 1;
    int result = 1;
    for (int i = 2; i <= n; ++i) {
        result *= i;
    }
    return result;
}