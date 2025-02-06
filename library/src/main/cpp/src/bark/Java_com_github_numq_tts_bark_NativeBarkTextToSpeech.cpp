#include "Java_com_github_numq_tts_bark_NativeBarkTextToSpeech.h"

static jclass exceptionClass;
static std::shared_mutex mutex;
static std::unordered_map<jlong, bark_context_ptr> pointers;

void handleException(JNIEnv *env, const std::string &errorMessage) {
    env->ThrowNew(exceptionClass, errorMessage.c_str());
}

bark_context *getPointer(jlong handle) {
    auto it = pointers.find(handle);
    if (it == pointers.end()) {
        throw std::runtime_error("Invalid handle");
    }
    return it->second.get();
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

    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) return;

    if (exceptionClass) env->DeleteGlobalRef(exceptionClass);

    pointers.clear();
}

JNIEXPORT jlong JNICALL
Java_com_github_numq_tts_bark_NativeBarkTextToSpeech_initNative(JNIEnv *env, jclass thisClass, jstring modelPath) {
    std::unique_lock<std::shared_mutex> lock(mutex);

    try {
        const char *modelPathChars = env->GetStringUTFChars(modelPath, nullptr);
        if (!modelPathChars) {
            throw std::runtime_error("Failed to get model path string");
        }

        std::string modelPathStr(modelPathChars);
        env->ReleaseStringUTFChars(modelPath, modelPathChars);

        if (modelPathStr.empty()) {
            throw std::runtime_error("Model path should not be empty");
        }

        auto params = bark_context_default_params();

        auto context = bark_load_model(modelPathStr.c_str(), params, 0);
        if (!context) {
            throw std::runtime_error("Failed to create native instance");
        }

        bark_context_ptr ptr(context);

        auto handle = reinterpret_cast<jlong>(ptr.get());

        pointers[handle] = std::move(ptr);

        return handle;
    } catch (const std::exception &e) {
        handleException(env, e.what());
        return -1;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_github_numq_tts_bark_NativeBarkTextToSpeech_generateNative(JNIEnv *env, jclass thisClass, jlong handle,
                                                                    jstring text) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto context = getPointer(handle);

        const char *textChars = env->GetStringUTFChars(text, nullptr);
        if (!textChars) {
            throw std::runtime_error("Failed to get text string");
        }

        std::string textStr(textChars);
        env->ReleaseStringUTFChars(text, textChars);

        if (bark_generate_audio(context, textStr.c_str(), 1)) {
            auto data = bark_get_audio_data(context);

            auto data_size = bark_get_audio_data_size(context);

            if (!data || data_size <= 0) {
                throw std::runtime_error("Invalid audio data or size");
            }

            auto length = static_cast<jsize>(data_size * sizeof(float));

            auto byteArray = env->NewByteArray(length);

            if (byteArray == nullptr) {
                throw std::runtime_error("Failed to allocate byte array");
            }

            env->SetByteArrayRegion(byteArray, 0, length, reinterpret_cast<const jbyte *>(data));

            return byteArray;
        }

        return env->NewByteArray(0);
    } catch (const std::exception &e) {
        handleException(env, e.what());
        return nullptr;
    }
}

JNIEXPORT void JNICALL
Java_com_github_numq_tts_bark_NativeBarkTextToSpeech_freeNative(JNIEnv *env, jclass thisClass, jlong handle) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        if (pointers.erase(handle) == 0) {
            handleException(env, "Unable to free native pointer");
        }
    } catch (const std::exception &e) {
        handleException(env, e.what());
    }
}