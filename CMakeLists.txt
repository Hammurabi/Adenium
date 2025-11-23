cmake_minimum_required(VERSION 3.26)
# Stxop VSCode from overriding compilers
if(APPLE)
    set(CMAKE_C_COMPILER "/usr/bin/clang" CACHE FILEPATH "C compiler" FORCE)
    set(CMAKE_CXX_COMPILER "/usr/bin/clang++" CACHE FILEPATH "C++ compiler" FORCE)
endif()
project(AdeniumCPP VERSION 1.0 LANGUAGES C CXX)

# Now add libc++ linking
if(APPLE)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libc++")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -lc++ -lc++abi")
endif()

# Disable IPO/LTO for all build types to prevent hanging
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION OFF)
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION_RELEASE OFF)
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION_MINSIZEREL OFF)
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION_RELWITHDEBINFO OFF)

# Use -O2 instead of -O3 for faster compilation on Apple Silicon
if(APPLE AND CMAKE_BUILD_TYPE MATCHES "Release|MinSizeRel")
    set(CMAKE_CXX_FLAGS_RELEASE "-O2 -DNDEBUG")
    set(CMAKE_C_FLAGS_RELEASE "-O2 -DNDEBUG")
    set(CMAKE_CXX_FLAGS_MINSIZEREL "-Os -DNDEBUG")
    set(CMAKE_C_FLAGS_MINSIZEREL "-Os -DNDEBUG")
endif()

set(BUILD_SHARED_LIBS ON)
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED YES)
set(CMAKE_C_STANDARD 17)
set(CMAKE_C_STANDARD_REQUIRED ON)
add_compile_options(-Wno-deprecated-declarations)

set(BUILD_BLS_PYTHON_BINDINGS "0" CACHE STRING "")
set(BUILD_BLS_TESTS "0" CACHE STRING "")
set(BUILD_BLS_BENCHMARKS "0" CACHE STRING "")

add_subdirectory(bls-signatures)
add_subdirectory(BLAKE3/c)

find_package(LevelDB REQUIRED)
if (APPLE)
    set(LevelDB_Location "/opt/homebrew/Cellar/leveldb/1.23_2")
    set(LevelDB_INCLUDE_DIRS "${LevelDB_Location}/include")
    set(LevelDB_LIBRARY_DIRS "${LevelDB_Location}/lib")
    set(LevelDB_LIBRARIES "leveldb")
endif()
find_package(Boost REQUIRED)
find_package(OpenSSL REQUIRED)

include_directories(
    ${LevelDB_INCLUDE_DIRS}
)

link_directories(
    ${LevelDB_LIBRARY_DIRS}
)

file(GLOB_RECURSE SOURCE_FILES
    src/*.cpp
    blst/src/*.cpp
    third_party/*.cpp
)

set(RESOURCE_DIR "${CMAKE_SOURCE_DIR}/res")
set(DEST_DIR "${CMAKE_BINARY_DIR}/res")

add_custom_target(copy_resources ALL
    COMMAND ${CMAKE_COMMAND} -E copy_directory
            ${RESOURCE_DIR}
            ${DEST_DIR}
    COMMENT "Copying resources to build directory"
)

add_executable(Adenium ${SOURCE_FILES})
add_dependencies(Adenium copy_resources)

target_include_directories(Adenium PRIVATE
    src
    blst
    BLAKE3/c
    third_party
    bls-signatures/src
    ${Boost_INCLUDE_DIRS}
    ${OPENSSL_INCLUDE_DIR}
)

target_link_libraries(Adenium PRIVATE
    blake3
    bls
    sodium
    ${LevelDB_LIBRARIES}
    ${OPENSSL_LIBRARIES}
)

# Apply additional compile flags for Apple Silicon to avoid hangs
if(APPLE)
    target_compile_options(Adenium PRIVATE
        -fno-inline-functions  # Disable aggressive inlining that can hang
        -fno-unroll-loops      # Disable loop unrolling
    )
endif()

# Fix for beacon.cpp hanging on Apple Silicon with optimizations
if(APPLE)
    set_source_files_properties(src/core/chain/beacon.cpp PROPERTIES
        COMPILE_FLAGS "-O0 -fno-inline-functions -fno-unroll-loops"
    )
    set_source_files_properties(src/main.cpp PROPERTIES
        COMPILE_FLAGS "-O0 -fno-inline-functions -fno-unroll-loops"
    )
    message(STATUS "Applied -O0 optimization to main.cpp/beacon.cpp to prevent hanging")
endif()

install(TARGETS Adenium DESTINATION bin)

message(STATUS "LevelDB include dirs: ${LevelDB_INCLUDE_DIRS}")
message(STATUS "LevelDB library dirs: ${LevelDB_LIBRARY_DIRS}")
message(STATUS "CXX FLAGS: ${CMAKE_CXX_FLAGS}")
message(STATUS "CXX FLAGS RELEASE: ${CMAKE_CXX_FLAGS_RELEASE}")
message(STATUS "Build type: ${CMAKE_BUILD_TYPE}")