cmake_minimum_required(VERSION 3.22.1)
project("honorroot" C)
set(CMAKE_C_STANDARD 11)

add_library(honorroot SHARED
    exploit.c
    su_injector.c
    overlay_mount.c
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(honorroot ${log-lib} ${android-lib})

target_compile_options(honorroot PRIVATE
    -O2 -Wall -Wno-unused-parameter
    -fvisibility=hidden -fstack-protector-strong
)
