cmake_minimum_required(VERSION 3.19)
project(sls_simulator VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(Python 3.12 REQUIRED COMPONENTS Interpreter Development.Module)
find_package(pybind11 CONFIG REQUIRED)

file(GLOB_RECURSE SLS_SIMULATOR_SOURCES CONFIGURE_DEPENDS
    "${CMAKE_CURRENT_SOURCE_DIR}/src/*.cpp")

pybind11_add_module(_lightspeed MODULE
    python/module.cpp
    ${SLS_SIMULATOR_SOURCES}
)
target_include_directories(_lightspeed PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/include"
    "${CMAKE_CURRENT_SOURCE_DIR}/third_party/nlohmann"
)

if(MSVC)
    target_compile_options(_lightspeed PRIVATE /O2 /EHsc)
else()
    target_compile_options(_lightspeed PRIVATE -O3 -Wno-shift-count-overflow)
endif()

if(NOT DEFINED SLS_NATIVE_OUTPUT_DIR)
    set(SLS_NATIVE_OUTPUT_DIR "${CMAKE_BINARY_DIR}/python")
endif()
set_target_properties(_lightspeed PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${SLS_NATIVE_OUTPUT_DIR}"
    RUNTIME_OUTPUT_DIRECTORY "${SLS_NATIVE_OUTPUT_DIR}"
)
