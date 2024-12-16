cmake_minimum_required(VERSION 3.22.1)

project(Camera-Rendering)

add_library(
        ${CMAKE_PROJECT_NAME}
        SHARED
        RenderingPlugin.cpp
)

find_library(
        dl
        GLESv3
        log )

target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        log)