cmake_minimum_required(VERSION 3.19)
project(spirecomm_lightspeed VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

include(FetchContent)

set(LIGHTSPEED_COMMIT "7476a81954020087da31d41d16fddf475746ec2d" CACHE STRING
    "Audited sts_lightspeed commit")
set(LIGHTSPEED_SOURCE_DIR "" CACHE PATH "Optional existing sts_lightspeed checkout")

if(LIGHTSPEED_SOURCE_DIR)
    set(lightspeed_SOURCE_DIR "${LIGHTSPEED_SOURCE_DIR}")
else()
    FetchContent_Declare(
        lightspeed
        GIT_REPOSITORY https://github.com/gamerpuppy/sts_lightspeed.git
        GIT_TAG ${LIGHTSPEED_COMMIT}
        GIT_SHALLOW FALSE
        GIT_SUBMODULES json
        PATCH_COMMAND ${CMAKE_COMMAND} -E chdir <SOURCE_DIR>
            git apply "${CMAKE_CURRENT_SOURCE_DIR}/patches/0001-ironclad-parity.patch"
        SOURCE_SUBDIR _spirecomm_no_upstream_cmake
    )
    FetchContent_MakeAvailable(lightspeed)
endif()

find_package(Python 3.12 REQUIRED COMPONENTS Interpreter Development.Module)
find_package(pybind11 CONFIG REQUIRED)

file(GLOB_RECURSE LIGHTSPEED_SOURCES CONFIGURE_DEPENDS
    "${lightspeed_SOURCE_DIR}/src/*.cpp")

pybind11_add_module(_lightspeed MODULE bridge.cpp ${LIGHTSPEED_SOURCES})
target_include_directories(_lightspeed PRIVATE
    "${lightspeed_SOURCE_DIR}/include"
    "${lightspeed_SOURCE_DIR}/json/single_include")

if(MSVC)
    target_compile_options(_lightspeed PRIVATE /O2 /EHsc)
else()
    target_compile_options(_lightspeed PRIVATE -O3 -Wno-shift-count-overflow)
endif()

set_target_properties(_lightspeed PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../spirecomm/simulator")

install(TARGETS _lightspeed LIBRARY DESTINATION spirecomm/simulator)
