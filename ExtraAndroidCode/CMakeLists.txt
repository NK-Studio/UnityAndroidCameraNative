cmake_minimum_required(VERSION 3.4.1)
project(Unity)

add_library(
        NativeCameraPlugin
        SHARED
        RenderingPlugin.cpp
)

find_library( # Sets the name of the path variable.
        dl
        GLESv3

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

add_subdirectory(FramePacing)

target_link_libraries( # Specifies the target library.
        NativeCameraPlugin
        log

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib} )