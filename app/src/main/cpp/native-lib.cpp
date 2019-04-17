#include <jni.h>
#include <string>
#include <inttypes.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_guidance_gsight_lasertrainingpro_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

