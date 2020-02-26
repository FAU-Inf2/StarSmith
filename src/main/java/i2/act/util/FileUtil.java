package i2.act.util;

import i2.act.errors.RPGException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class FileUtil {

  public static final String changeFileExtension(final String originalFileName,
      final String newExtension) {
    final int positionOfLastSeparator = originalFileName.lastIndexOf(File.separator);
    final int positionOfLastDot = originalFileName.lastIndexOf('.');

    if (positionOfLastDot <= positionOfLastSeparator) {
      // last file name of path does not contain a file extension -> append new extension
      return originalFileName + "." + newExtension;
    } else {
      // last file name of path already contains an extension -> replace it
      return originalFileName.substring(0, positionOfLastDot + 1) + newExtension;
    }
  }

  public static final String getFileExtension(final String fileName) {
    final int positionOfLastSeparator = fileName.lastIndexOf(File.separator);
    final int positionOfLastDot = fileName.lastIndexOf('.');

    if (positionOfLastDot <= positionOfLastSeparator) {
      return "";
    } else {
      return fileName.substring(positionOfLastDot + 1);
    }
  }

  public static final String prependBeforeFileExtension(final String originalFileName,
      final String prefix) {
    return changeFileExtension(originalFileName, prefix + "." + getFileExtension(originalFileName));
  }

  public static final String getBaseName(final String fileName) {
    final File file = new File(fileName);
    return file.getName();
  }

  public static final String getStrippedBaseName(final String fileName) {
    final String baseName = getBaseName(fileName);
    final int positionOfLastDot = baseName.lastIndexOf('.');

    if (positionOfLastDot == -1) {
      // base name does not contain a file extension
      return baseName;
    } else {
      // base name contains an extension -> remove it
      return baseName.substring(0, positionOfLastDot);
    }
  }

  public static final boolean fileExists(final String fileName) {
    final File file = new File(fileName);
    return fileExists(file);
  }

  public static final boolean fileExists(final File file) {
    return file.exists() && file.isFile();
  }

  public static final boolean deleteFile(final String fileName) {
    final File file = new File(fileName);
    return deleteFile(file);
  }

  public static final boolean deleteFile(final File file) {
    return file.delete();
  }

  public static final BufferedWriter openFileForWriting(final String fileName) {
    return openFileForWriting(fileName, false);
  }

  public static final BufferedWriter openFileForWriting(final String fileName,
      final boolean createPath) {
    final File file = new File(fileName);
    return openFileForWriting(file, createPath);
  }

  public static final BufferedWriter openFileForWriting(final File file) {
    return openFileForWriting(file, false);
  }

  public static final BufferedWriter openFileForWriting(final File file, final boolean createPath) {
    try {
      if (createPath && file.getParentFile() != null) {
        file.getParentFile().mkdirs();
      }

      final FileWriter fileWriter = new FileWriter(file);
      return new BufferedWriter(fileWriter);
    } catch (final IOException exception) {
      throw new RPGException(
          String.format("unable to open file '%s' for writing", file.getPath()));
    }
  }

  public static final void writeToFile(final String string, final String fileName) {
    final BufferedWriter writer = FileUtil.openFileForWriting(fileName);
    FileUtil.write(string, writer);
    FileUtil.closeWriter(writer);
  }

  public static final BufferedReader openFileForReading(final String fileName) {
    final File file = new File(fileName);
    return openFileForReading(file);
  }

  public static final BufferedReader openFileForReading(final File file) {
    try {
      final FileReader fileReader = new FileReader(file);
      return new BufferedReader(fileReader);
    } catch (final IOException exception) {
      throw new RPGException(
          String.format("unable to open file '%s' for reading", file.getPath()));
    }
  }

  public static final void closeWriter(final BufferedWriter writer) {
    try {
      writer.close();
    } catch (final IOException exception) {
      throw new RPGException("unable to close writer");
    }
  }

  public static final void closeReader(final BufferedReader reader) {
    try {
      reader.close();
    } catch (final IOException exception) {
      throw new RPGException("unable to close reader");
    }
  }

  public static final void write(final Object object, final BufferedWriter writer) {
    write(object.toString(), writer);
  }

  public static final void write(final String string, final BufferedWriter writer) {
    try {
      writer.write(string);
    } catch (final IOException exception) {
      throw new RPGException("unable to write to writer", exception);
    }
  }

  public static final String readLine(final BufferedReader reader) {
    try {
      return reader.readLine();
    } catch (final IOException exception) {
      throw new RPGException("unable to read from reader", exception);
    }
  }

  public static final void flushWriter(final BufferedWriter writer) {
    try {
      writer.flush();
    } catch (final IOException exception) {
      throw new RPGException("unable to flush writer", exception);
    }
  }

  public static final String readFile(final String fileName) {
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(fileName));
      return new String(bytes);
    } catch (final IOException exception) {
      throw new RPGException("unable to read file", exception);
    }
  }

}
