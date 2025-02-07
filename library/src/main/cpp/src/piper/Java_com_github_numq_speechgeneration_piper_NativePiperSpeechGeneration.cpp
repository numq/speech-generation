#include "Java_com_github_numq_speechgeneration_piper_NativePiperSpeechGeneration.h"

static jclass exceptionClass;
static jclass stringClass;
static std::shared_mutex mutex;
static bool isInitialized = false;

void handleException(JNIEnv *env, const std::string &errorMessage) {
    env->ThrowNew(exceptionClass, errorMessage.c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }

    exceptionClass = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/RuntimeException")));
    if (exceptionClass == nullptr) {
        return JNI_ERR;
    }

    stringClass = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/String")));
    if (stringClass == nullptr) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) return;

    if (exceptionClass) env->DeleteGlobalRef(exceptionClass);
    if (stringClass) env->DeleteGlobalRef(stringClass);

    std::unique_lock<std::shared_mutex> lock(mutex);
    if (isInitialized) {
        espeak_Terminate();
        isInitialized = false;
    }
}

JNIEXPORT void JNICALL
Java_com_github_numq_speechgeneration_piper_NativePiperSpeechGeneration_initNative(JNIEnv *env, jclass thisClass,
                                                                                   jstring dataPath) {
    std::unique_lock<std::shared_mutex> lock(mutex);

    try {
        if (isInitialized) {
            return;
        }

        const char *dataPathChars = env->GetStringUTFChars(dataPath, nullptr);
        if (!dataPathChars) {
            throw std::runtime_error("Failed to get eSpeak data path string");
        }

        std::string dataPathStr(dataPathChars);
        env->ReleaseStringUTFChars(dataPath, dataPathChars);

        int options = 1;

        if (espeak_Initialize(AUDIO_OUTPUT_RETRIEVAL, 0, dataPathStr.c_str(), options) == -1) {
            throw std::runtime_error("Failed to initialize espeak-ng, check your data path");
        }

        isInitialized = true;
    } catch (const std::exception &e) {
        handleException(env, e.what());
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_github_numq_speechgeneration_piper_NativePiperSpeechGeneration_phonemizeNative(JNIEnv *env, jclass thisClass,
                                                                                        jstring voice,
                                                                                        jstring text) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        if (!isInitialized) {
            throw std::runtime_error("eSpeak is not initialized");
        }

        const char *voiceChars = env->GetStringUTFChars(voice, nullptr);
        if (!voiceChars) {
            throw std::runtime_error("Failed to get voice string");
        }
        std::string voiceStr(voiceChars);
        env->ReleaseStringUTFChars(voice, voiceChars);

        if (espeak_SetVoiceByName(voiceStr.c_str()) != EE_OK) {
            throw std::runtime_error("Failed to set voice");
        }

        const char *textChars = env->GetStringUTFChars(text, nullptr);
        if (!textChars) {
            throw std::runtime_error("Failed to get text string");
        }
        std::string textStr(textChars);
        env->ReleaseStringUTFChars(text, textChars);

        const char *inputTextPointer = textStr.c_str();

        int terminator = 0;

        auto clausePhonemes = espeak_TextToPhonemesWithTerminator((const void **) &inputTextPointer, espeakCHARS_AUTO,
                                                                  espeakPHONEMES_IPA, &terminator);

        jobjectArray resultArray = env->NewObjectArray(3, env->FindClass("java/lang/String"), nullptr);
        if (!resultArray) {
            throw std::runtime_error("Failed to create result array");
        }

        env->SetObjectArrayElement(resultArray, 0, env->NewStringUTF(inputTextPointer));
        env->SetObjectArrayElement(resultArray, 1, env->NewStringUTF(clausePhonemes));
        env->SetObjectArrayElement(resultArray, 2, env->NewStringUTF(std::to_string(terminator).c_str()));

        return resultArray;

    } catch (const std::exception &e) {
        handleException(env, e.what());
        return nullptr;
    }
}