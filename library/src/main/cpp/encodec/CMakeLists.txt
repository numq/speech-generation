cmake_minimum_required(VERSION 3.24)
project(encodec VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 23)

option(BUILD_WITH_CUDA "Build with CUDA support" OFF)

add_library(encodec SHARED src/encodec.cpp src/ops.cpp)

target_include_directories(encodec PRIVATE include include/ggml)

if (BUILD_WITH_CUDA)
    target_link_directories(encodec PRIVATE bin/cuda)

    target_link_libraries(encodec PRIVATE ggml-base ggml-cpu ggml-cuda ggml)
else ()
    target_link_directories(encodec PRIVATE bin/cpu)

    target_link_libraries(encodec PRIVATE ggml-base ggml-cpu ggml)
endif ()