package io.adenium.utils;

import java.io.*;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

public class FileService {
    private String entryPoint;

    public FileService(Object entryPoint) {
        this.entryPoint = new File(entryPoint.toString()).toString().replaceAll("\\/", separator());
        if (this.entryPoint.endsWith("\\") || this.entryPoint.endsWith("/"))
            this.entryPoint = this.entryPoint.substring(0, this.entryPoint.length() - 1);
    }

    public static FileService homeDir() {
        return new FileService(System.getProperty("user.home"));
    }

    public static FileService appDir() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win"))
        {
            return new FileService(System.getProperty("user.home")).newFile("AppData").newFile("Roaming");
        }
        else {
            return new FileService(System.getProperty("user.home")).newFile("Applications");
        }
    }

    public long length() {
        return file().length();
    }

    public boolean exists() {
        return file().exists();
    }

    public FileService newFile(String name) {
        return new FileService(entryPoint + File.separator + name);
    }

    public String separator() {
        return File.separator;
    }

    public File file() {
        return new File(entryPoint);
    }

    public <T> T as(Class<?> ctype) throws IOException {
        if (ctype == BufferedReader.class)
            return (T) new BufferedReader(new FileReader(file()));
        else if (ctype == InputStream.class)
            return (T) new FileInputStream(file());
        else if (ctype == DataInputStream.class)
            return (T) new DataInputStream(new FileInputStream(file()));


        else if (ctype == BufferedWriter.class)
            return (T) new BufferedWriter(new FileWriter(file()));
        else if (ctype == OutputStream.class)
            return (T) new FileOutputStream(file());
        else if (ctype == DataOutputStream.class)
            return (T) new DataOutputStream(new FileOutputStream(file()));
        else if (ctype == InflaterOutputStream.class)
            return (T) new DataOutputStream(new InflaterOutputStream(new FileOutputStream(file())));
        else if (ctype == InflaterInputStream.class)
            return (T) new DataInputStream(new InflaterInputStream(new FileInputStream(file())));
        else if (ctype == DeflaterOutputStream.class)
            return (T) new DataOutputStream(new DeflaterOutputStream(new FileOutputStream(file())));
        else if (ctype == DeflaterInputStream.class)
            return (T) new DataInputStream(new DeflaterInputStream(new FileInputStream(file())));
        else
            return null;
    }

    public void move(FileService service) throws IOException {
        copyTo(service);
        file().delete();
    }

    public void copyTo(FileService service) throws IOException {
        FileInputStream instream = new FileInputStream(file());

        FileOutputStream out = new FileOutputStream(service.file());

        while (instream.available() > 0)
            out.write(instream.read());

        instream.close();
        out.flush();
        out.close();
    }

    @Override
    public String toString() {
        return file().toString();
    }

    public boolean makeDirectory() {
        return file().mkdir();
    }

    public boolean makeDirectories() {
        return file().mkdirs();
    }

    public boolean delete() {
        return file().delete();
    }

    public boolean isDirectory() {
        return file().isDirectory();
    }

    public String simpleName() {
        return file().getName();
    }

    public FileInputStream openFileInputStream() throws IOException {
        return new FileInputStream(file());
    }

    public OutputStream openFileOutputStream() throws FileNotFoundException {
        return new FileOutputStream(file());
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openFileInputStream());
    }

    public DataOutputStream openDataOutputStream() throws FileNotFoundException {
        return new DataOutputStream(openFileOutputStream());
    }

    public byte[] readBytes() throws IOException {
        if (!exists())
            return null;

        if (isDirectory())
            return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        FileInputStream inputStream = new FileInputStream(file());
        byte buffer[] = new byte[8192];
        int count = 0;

        while ((count = inputStream.read(buffer)) > 0)
            stream.write(buffer, 0, count);

        inputStream.close();
        stream.flush();
        stream.close();

        return stream.toByteArray();
    }

    public String readUTF() throws IOException {
        if (!exists())
            return null;

        if (isDirectory()) {
            String dir = "directory: " + simpleName();
            for (File file : file().listFiles())
                dir += "\n\t" + (file.isDirectory() ? "directory: " : "file: ") + file.getName();

            return dir;
        }

        BufferedReader reader = as(BufferedReader.class);
        String line = "";
        String lines = "";

        while ((line = reader.readLine()) != null)
            lines += line + "\n";
        reader.close();

        return lines;
    }

    public void writeTo(DataOutputStream writer) throws Exception {
        FileInputStream inputStream = new FileInputStream(file());
        byte buffer[] = new byte[8192];
        int count = 0;

        while ((count = inputStream.read(buffer)) > 0)
            writer.write(buffer, 0, count);

        inputStream.close();
    }

    public void writeBytes(byte[] bytes) throws IOException {
        FileOutputStream stream = new FileOutputStream(file());
        stream.write(bytes);
        stream.flush();
        stream.close();
    }

    public FileService append(String s) {
        return new FileService(entryPoint + s);
    }

    public void writeUTF(String toString) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file()));
        writer.write(toString);
        writer.flush();
        writer.close();
    }

    public FileReader openFileReader() throws FileNotFoundException {
        return new FileReader(file());
    }

    public RandomAccessFile randomAccess() throws FileNotFoundException {
        return new RandomAccessFile(file(), "rw");
    }
}