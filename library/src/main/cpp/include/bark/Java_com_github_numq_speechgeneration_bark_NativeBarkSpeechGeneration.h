#include <jni.h>
#include <iostream>
#include <shared_mutex>
#include <mutex>
#include <unordered_map>
#include <memory>
#include <vector>
#include "bark.h"
#include "deleter.h"

#ifndef _Included_com_github_numq_speechgeneration_bark_NativeBarkSpeechGeneration
#define _Included_com_github_numq_speechgeneration_bark_NativeBarkSpeechGeneration
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_github_numq_speechgeneration_bark_NativeBarkSpeechGeneration_initNative
        (JNIEnv *, jclass, jstring);

JNIEXPORT jbyteArray JNICALL Java_com_github_numq_speechgeneration_bark_NativeBarkSpeechGeneration_generateNative
        (JNIEnv *, jclass, jlong, jstring);

JNIEXPORT void JNICALL Java_com_github_numq_speechgeneration_bark_NativeBarkSpeechGeneration_freeNative
        (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
