cmake_minimum_required(VERSION 3.19)
project(sls_simulator VERSION 1.0.0 LANGUAGES CXX)

option(SLS_ENABLE_SANITIZERS "Build with AddressSanitizer and UBSan" OFF)
set(SLS_NATIVE_SOURCE_SHA256 "UNKNOWN" CACHE STRING "Canonical native source digest")
set(SLS_GIT_COMMIT "UNKNOWN" CACHE STRING "Git commit embedded in the native module")

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
target_compile_definitions(_lightspeed PRIVATE
    SLS_NATIVE_SOURCE_SHA256="${SLS_NATIVE_SOURCE_SHA256}"
    SLS_GIT_COMMIT="${SLS_GIT_COMMIT}"
)

if(MSVC)
    # Production invariants must remain active in Release builds.
    target_compile_options(_lightspeed PRIVATE /O2 /EHsc /UNDEBUG /W4)
else()
    target_compile_options(_lightspeed PRIVATE
        -O3 -UNDEBUG -Wall -Wextra -Wpedantic -Wno-shift-count-overflow
    )
    if(SLS_ENABLE_SANITIZERS)
        target_compile_options(_lightspeed PRIVATE
            -fsanitize=address,undefined -fno-omit-frame-pointer
        )
        target_link_options(_lightspeed PRIVATE -fsanitize=address,undefined)
    endif()
endif()

if(NOT DEFINED SLS_NATIVE_OUTPUT_DIR)
    set(SLS_NATIVE_OUTPUT_DIR "${CMAKE_BINARY_DIR}/python")
endif()
set_target_properties(_lightspeed PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${SLS_NATIVE_OUTPUT_DIR}"
    RUNTIME_OUTPUT_DIRECTORY "${SLS_NATIVE_OUTPUT_DIR}"
)
