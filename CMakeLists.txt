cmake_minimum_required(VERSION 3.5)
project(AdeniumCPP VERSION 1.0 LANGUAGES CXX)
# add_definitions("-std=c++23")
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED YES)
add_compile_options(-Wno-deprecated-declarations)

if(CMAKE_COMPILER_IS_GNUCC OR CMAKE_CXX_COMPILER_ID STREQUAL "Clang")
    add_compile_options("-Werror")
    add_compile_options(-Wno-unused-private-field)
elseif(MSVC)
    add_compile_options(/wd4100)
endif()


find_package(LevelDB REQUIRED)
if (APPLE)
    set(LevelDB_Location "/opt/homebrew/Cellar/leveldb/1.23_2")
    set(LevelDB_INCLUDE_DIRS "${LevelDB_Location}/include")
    set(LevelDB_LIBRARY_DIRS "${LevelDB_Location}/lib")
    set(LevelDB_LIBRARIES "leveldb")  # or "libleveldb.a" depending on your setup
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

add_executable(Adenium ${SOURCE_FILES})

target_include_directories(Adenium PRIVATE
    src
    blst
    third_party
    bls-signatures/src
    ${Boost_INCLUDE_DIRS}
    ${OPENSSL_INCLUDE_DIR}
)

target_link_libraries(Adenium PRIVATE
    ${CMAKE_SOURCE_DIR}/bls-signatures/build/src/libbls.a  # <--- .a extension needed
    ${LevelDB_LIBRARIES}
    ${CMAKE_SOURCE_DIR}/bls-signatures/build/_deps/sodium-build/libsodium.a
    ${OPENSSL_LIBRARIES}
)

target_compile_options(Adenium PRIVATE
    -Wall
    -Wextra
    -Werror
)

install(TARGETS Adenium DESTINATION bin)


message(STATUS "LevelDB include dirs: ${LevelDB_INCLUDE_DIRS}")
message(STATUS "LevelDB library dirs: ${LevelDB_LIBRARY_DIRS}")
message(STATUS "CXX FLAGS: ${CMAKE_CXX_FLAGS}")