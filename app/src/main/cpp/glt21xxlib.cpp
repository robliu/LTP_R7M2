//
// Created by Rob on 4/17/2017.
//
#include <jni.h>



extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_TimerCameraView_GetThreshold(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_TimerCameraView_GetThresholdLow(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = (t1 * 4 + 55)/5;  // t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = (t1 * 3 + 55)/4;  // t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t2;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_TimerCameraView_GetIPBK(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t3;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_CameraView_GetThreshold(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_CameraView_GetThresholdLow(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t2;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_CameraView_GetIPBK(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t3;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_CameraView_GetIPBKK(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 244;
        t2 = t1 - 10;
        t3 = 177;
    }
    else if (t1 <= 160) {
        t1 += 66;
        t2 = t1 - 86;
        t3 = 155;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 165;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 185;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 220;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 230;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 240;
    }

    return t3;
}

extern "C"
JNIEXPORT jint JNICALL
        Java_com_get_gsappalpha1_TestCameraView_GetThreshold(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_TestCameraView_GetThresholdLow(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t2;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_TestCameraView_GetThresholdLow_v2(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3, jint t4, jint t5) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    if((t4 > t5*3) || (t4 > 2*t5 && t5 < 55 )) {
        jint s1 = t4 + 10;
        if(s1 < t2) t2 = s1;
    }

    return t2;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_TestCameraView_GetIPBK(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    return t3;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_get_gsappalpha1_TestCameraView_GetIPBK_v2(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3, jint t4, jint t5) {

    if (t1 >= 253) {
        t1 = 254;
        t2 = t1 - 10;
        t3 = 210;
    }
    else if (t1 <= 160) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 55;
    }
    else if (t1 <= 175) {
        t1 += 6;
        t2 = t1 - 86;
        t3 = 65;
    }
    else if (t1 <= 200) {
        t1 += 5;
        t2 = t1 - 20;
        t3 = 115;
    }
    else if (t1 < 240) {
        t1 += 4;
        t2 = t1 - 15;
        t3 = 170;
    }
    else if (t1 < 250) {
        t1 += 3;
        t2 = t1 - 10;
        t3 = 180;
    }
    else { // 251 - 252
        t1 += 2;
        t2 = t1 - 10;
        t3 = 190;
    }

    if((t4 > t5*3) || (t4 > 2*t5 && t5 < 55 )) {
        jint s1 = (t4+t5 * 4)/5;
        if(s1 < t3) t3 = s1;
    }

    return t3;
}


extern "C"
JNIEXPORT jdouble JNICALL
Java_com_get_gsappalpha1_laserHit_GetIntenseWeight(JNIEnv *env, jobject obj, jint t1, jint t2, jint t3) {

    jdouble  t4;
    if (t3 == 0 || t1 < t2) {
        t4 = 0.0;
    } else {
        t4 = ((jdouble)(t1 - t2)) / (t3 + 0.00001);
    }

    return t4;
}