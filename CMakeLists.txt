cmake_minimum_required(VERSION 3.20)
project(ov_genai_java LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(ANDROID)
    set(JNI_INCLUDE_DIRS "")
    set(JNI_LIBRARIES "")
else()
    find_package(JNI REQUIRED)
endif()
find_package(OpenVINOGenAI QUIET CONFIG)

set(OV_GENAI_JNI_MODE "AUTO" CACHE STRING "JNI implementation mode: AUTO, REAL, or STUB")
set_property(CACHE OV_GENAI_JNI_MODE PROPERTY STRINGS AUTO REAL STUB)

set(OV_GENAI_JNI_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/src/main/native/ov_genai_java_jni.cpp")
set(OV_GENAI_JNI_REAL OFF)

if(OV_GENAI_JNI_MODE STREQUAL "REAL")
    if(NOT OpenVINOGenAI_FOUND)
        message(FATAL_ERROR "OV_GENAI_JNI_MODE=REAL requires OpenVINOGenAI package configuration")
    endif()
    set(OV_GENAI_JNI_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/src/main/native/ov_genai_java_jni_real.cpp")
    set(OV_GENAI_JNI_REAL ON)
elseif(OV_GENAI_JNI_MODE STREQUAL "AUTO")
    if(OpenVINOGenAI_FOUND)
        set(OV_GENAI_JNI_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/src/main/native/ov_genai_java_jni_real.cpp")
        set(OV_GENAI_JNI_REAL ON)
    endif()
elseif(NOT OV_GENAI_JNI_MODE STREQUAL "STUB")
    message(FATAL_ERROR "Unsupported OV_GENAI_JNI_MODE=${OV_GENAI_JNI_MODE}")
endif()

add_library(ov_genai_java_jni SHARED
    ${OV_GENAI_JNI_SOURCE}
)

target_include_directories(ov_genai_java_jni
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/native
        ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ov_genai_java_jni
    PRIVATE
        ${JNI_LIBRARIES}
)

if(OV_GENAI_JNI_REAL)
    target_link_libraries(ov_genai_java_jni
        PRIVATE
            openvino::genai
    )
    target_compile_definitions(ov_genai_java_jni PRIVATE OV_GENAI_JNI_REAL=1)
else()
    target_compile_definitions(ov_genai_java_jni PRIVATE OV_GENAI_JNI_STUB=1)
endif()

install(TARGETS ov_genai_java_jni DESTINATION lib)
