# get the current working directory
import os
cwd = os.getcwd()
print(cwd)

# import platform to check which os we're running
import platform

# define helper functions
def findFile(dir, n):
    for entry in os.scandir(dir):
        if entry.is_dir() and n in entry.name:
            return os.path.join(dir, entry.name)
    return dir

# set the global variables
tools   = os.path.join(cwd, "tools")
if not os.path.exists(tools):
    os.makedirs(tools)

# download openjdk from link
import urllib.request
url     = ""
openjdk = ""

def install_jdk():
    if 'win' in platform.system().lower():
        print("downloading windows (x64) version of openjdk")
        url = 'https://download.java.net/openjdk/jdk16/ri/openjdk-16+36_windows-x64_bin.zip'
        openjdk = os.path.join(cwd, "tools", "jdk.zip")
    elif 'mac' in platform.system().lower():
        print("could not download OpenJDK")
        print("no available options for Mac OS")
        print("please manually install a JDK")
        quit()
    else:
        print("downloading linux (x64) version of openjdk")
        url = 'https://download.java.net/openjdk/jdk16/ri/openjdk-16+36_linux-x64_bin.tar.gz'
        openjdk = os.path.join(cwd, "tools", "jdk.tar.gz")

    print("downloading openjdk from '" + url + "'")
    urllib.request.urlretrieve(url, openjdk)
    print("installing openjdk to '" + openjdk + "'")

    # unzip openjdk to file 'openjdk'
    import zipfile
    import tarfile

    print("unzipping package 'openjdk' to '" + os.path.join(tools, "openjdk") + "'")
    if openjdk.endswith(".zip"):
        with zipfile.ZipFile(openjdk, 'r') as zip:
            for member in zip.infolist():
                zip.extract(member, tools)
                print("extracted: " + member.filename)
    else:
        os.makedirs(os.path.join(tools, "openjdk"))
        with tarfile.open(openjdk, "r:gz") as tar:
            for info in tar.getmembers():
                if info.name.startswith('jdk-16/'):
                    tar.extract(info.name, os.path.join(os.path.join(tools, "openjdk"), info.name.replace('jdk-16/', '')))
                    print("extracted: " + info.name)

    file    = findFile(tools, 'jdk')
    print("unzipped package 'openjdk' to '" + file + "'")
    print("renaming file '" + file + "' to '" + os.path.join(tools, 'openjdk') + "'")
    os.rename(file, os.path.join(tools, 'openjdk'))
    print("renamed file '" + file + "' to '" + os.path.join(tools, 'openjdk') + "'")
    print("deleting artifact '" + openjdk + "'")
    os.remove(openjdk)
    print("deleted artifact '" + openjdk + "'")

    # write a log file to the tools directory to indicate that the openjdk has been installed
    print("writing log file to '" + os.path.join(tools, "openjdk.log") + "'")
    with open(os.path.join(tools, "openjdk.log"), "w") as log:
        log.write("openjdk installed")
    print("wrote log file to '" + os.path.join(tools, "openjdk.log") + "'")

# check if the openjdk has been installed using the log file and print a message if it has
print("checking if openjdk has been installed")
if os.path.exists(os.path.join(tools, "openjdk.log")):
    with open(os.path.join(tools, "openjdk.log"), "r") as log:
        if "openjdk installed" in log.read():
            print("openjdk has already been installed")
        else:
            print("openjdk has not been installed")
            install_jdk()
else:
    print("openjdk has not been installed")
    install_jdk()