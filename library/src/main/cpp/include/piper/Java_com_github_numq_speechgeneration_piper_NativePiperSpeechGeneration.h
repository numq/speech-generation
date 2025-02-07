#include <jni.h>
#include <iostream>
#include <shared_mutex>
#include <mutex>
#include <unordered_map>
#include <memory>
#include <vector>
#include "espeak_ng.h"

#ifndef _Included_com_github_numq_speechgeneration_piper_NativePiperSpeechGeneration
#define _Included_com_github_numq_speechgeneration_piper_NativePiperSpeechGeneration
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_github_numq_speechgeneration_piper_NativePiperSpeechGeneration_initNative
        (JNIEnv *, jclass, jstring);

JNIEXPORT jobjectArray JNICALL Java_com_github_numq_speechgeneration_piper_NativePiperSpeechGeneration_phonemizeNative
        (JNIEnv *, jclass, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif
