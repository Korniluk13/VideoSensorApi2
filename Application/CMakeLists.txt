# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

SET(TARGET native-lib)
SET(LIBRARY_SOURCES_DIR ${CMAKE_CURRENT_LIST_DIR}/src/main/cpp)
file(GLOB LIBRARY_SOURCES  ${LIBRARY_SOURCES_DIR}/*.cpp)
message(ERROR " ${LIBRARY_SOURCES}")

add_library( # Sets the name of the library.
             ${TARGET}

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${LIBRARY_SOURCES})



string(REPLACE _ - OPENCV_POLLY_MASK
        -api-${ANDROID_NATIVE_API_LEVEL}-${ANDROID_ABI}-*${ANDROID_TOOLCHAIN})


set(OPENCV_POLLY_INSTALL_ROOT /home/anastasiya/Documents/melsc/MELCodeReader/opencv/_install)


FILE(GLOB children  RELATIVE ${OPENCV_POLLY_INSTALL_ROOT}
    ${OPENCV_POLLY_INSTALL_ROOT}/*${OPENCV_POLLY_MASK}*)
SET(OPENCV_abi_dirlist "")
FOREACH(child ${children})
    IF(IS_DIRECTORY ${OPENCV_POLLY_INSTALL_ROOT}/${child})
        LIST(APPEND OPENCV_abi_dirlist ${child})
    ENDIF()
ENDFOREACH()

set(OpenCV_DIR ${OPENCV_POLLY_INSTALL_ROOT}/${OPENCV_abi_dirlist}/sdk/native/jni/abi-)

find_package(OpenCV REQUIRED COMPONENTS core imgproc)

target_link_libraries(${TARGET} ${OpenCV_LIBS}
               -O2 -fuse-ld=gold -Wl,--gc-sections -Wl,--icf=safe -Wl,--as-needed
)

target_include_directories(${TARGET} PRIVATE ${OpenCV_INCLUDE_DIRS} ${LIBRARY_SOURCES_DIR})


target_compile_options(${TARGET} PUBLIC -O2 -frtti -fexceptions -fvisibility=hidden -ffunction-sections -fdata-sections -std=c++1z)

set_property(TARGET ${TARGET} PROPERTY INTERPROCEDURAL_OPTIMIZATION True)