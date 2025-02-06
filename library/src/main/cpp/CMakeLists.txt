cmake_minimum_required(VERSION 3.24)
project(tts VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 23)

option(BUILD_WITH_CUDA "Build with CUDA support" OFF)

# BARK

add_library(tts_bark SHARED
        src/bark/encodec.cpp
        src/bark/ops.cpp
        src/bark/bark.cpp
        src/bark/Java_com_github_numq_tts_bark_NativeBarkTextToSpeech.cpp
)

find_package(JNI)

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    target_include_directories(tts_bark PRIVATE ${JNI_INCLUDE_DIRS})
else ()
    message(FATAL_ERROR "JNI not found.")
endif ()

target_include_directories(tts_bark PRIVATE include/bark include/bark/ggml include/bark/encodec)

if (BUILD_WITH_CUDA)
    target_link_directories(tts_bark PRIVATE bin/bark/cuda)

    target_link_libraries(tts_bark PRIVATE ggml ggml-base ggml-cpu ggml-cuda)
else ()
    target_link_directories(tts_bark PRIVATE bin/bark/cpu)

    target_link_libraries(tts_bark PRIVATE ggml ggml-base ggml-cpu)
endif ()

# PIPER

add_library(tts_piper SHARED src/piper/Java_com_github_numq_tts_piper_NativePiperTextToSpeech.cpp)

find_package(JNI)

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    target_include_directories(tts_piper PRIVATE ${JNI_INCLUDE_DIRS})
else ()
    message(FATAL_ERROR "JNI not found.")
endif ()

target_include_directories(tts_piper PRIVATE include/piper include/piper/espeak include/piper/espeak-ng include/piper/libespeak-ng include/piper/piper-phonemize)

target_link_directories(tts_piper PRIVATE bin/piper)

target_link_libraries(tts_piper PRIVATE espeak-ng)