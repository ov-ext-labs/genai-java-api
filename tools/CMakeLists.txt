cmake_minimum_required(VERSION 3.23)

project(ov_genai_android_tools LANGUAGES CXX)

find_package(OpenVINO REQUIRED COMPONENTS Runtime Threading)
find_package(OpenVINOGenAI REQUIRED)

add_executable(android_gfx_smoke_runner android_gfx_smoke_runner.cpp)
target_compile_features(android_gfx_smoke_runner PRIVATE cxx_std_17)
target_link_libraries(android_gfx_smoke_runner PRIVATE openvino::runtime openvino::genai)
