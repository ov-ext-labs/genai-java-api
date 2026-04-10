cmake_minimum_required(VERSION 3.20)
project(ov_genai_java LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(ov_genai_java_jni SHARED
    src/main/native/ov_genai_java_jni.cpp
)

target_include_directories(ov_genai_java_jni
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/native
)

install(TARGETS ov_genai_java_jni DESTINATION lib)
