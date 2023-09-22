// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.InlineMe;
import com.google.errorprone.annotations.Var;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.TempFile.TempFileBuilder;

/** Provides helper functions for file access. */
@Deprecated
public final class MoreFiles {

  private MoreFiles() {
    /* utility class */
  }

  /** See {@link com.google.common.io.Files#asByteSink(java.io.File, FileWriteMode...)}. */
  @Deprecated
  public static ByteSink asByteSink(Path path, FileWriteMode... options) {
    return com.google.common.io.MoreFiles.asByteSink(path, fileWriteModeToOption(options));
  }

  /** See {@link com.google.common.io.Files#asByteSource(java.io.File)}. */
  @Deprecated
  @InlineMe(replacement = "com.google.common.io.MoreFiles.asByteSource(path)")
  public static ByteSource asByteSource(Path path) {
    return com.google.common.io.MoreFiles.asByteSource(path);
  }

  /** See {@link com.google.common.io.Files#asCharSink(java.io.File, Charset, FileWriteMode...)}. */
  @Deprecated
  public static CharSink asCharSink(Path path, Charset charset, FileWriteMode... options) {
    return com.google.common.io.MoreFiles.asCharSink(path, charset, fileWriteModeToOption(options));
  }

  /** See {@link com.google.common.io.Files#asCharSource(java.io.File, Charset)}. */
  @Deprecated
  @InlineMe(replacement = "com.google.common.io.MoreFiles.asCharSource(path, charset)")
  public static CharSource asCharSource(Path path, Charset charset) {
    return com.google.common.io.MoreFiles.asCharSource(path, charset);
  }

  private static OpenOption[] fileWriteModeToOption(FileWriteMode[] modes) {
    @Var boolean append = false;
    for (FileWriteMode mode : modes) {
      if (mode == FileWriteMode.APPEND) {
        append = true;
      } else if (mode != null) {
        throw new AssertionError("unknown FileWriteMode " + mode);
      }
    }
    return append ? new OpenOption[] {StandardOpenOption.APPEND} : new OpenOption[0];
  }

  /**
   * Creates a temporary file with an optional content. The file is marked for deletion when the
   * Java VM exits.
   *
   * @param prefix The prefix string to be used in generating the file's name; must be at least
   *     three characters long
   * @param suffix The suffix string to be used in generating the file's name; may be <code>null
   *     </code>, in which case the suffix <code>".tmp"</code> will be used
   * @param content The content to write (may be null). Will be written with default charset.
   * @throws IllegalArgumentException If the <code>prefix</code> argument contains fewer than three
   *     characters
   * @throws IOException If a file could not be created
   * @deprecated Use {@link TempFile#builder()}.
   */
  @Deprecated
  public static Path createTempFile(
      String prefix, @Nullable String suffix, @Nullable String content) throws IOException {
    TempFileBuilder builder = TempFile.builder().prefix(prefix);
    if (suffix != null) {
      builder.suffix(suffix);
    }
    if (content != null) {
      builder.initialContent(content, Charset.defaultCharset());
    }
    return builder.create();
  }

  /**
   * Create a temporary file similar to {@link java.io.File#createTempFile(String, String)}.
   *
   * <p>The resulting {@link Path} object is wrapped in a {@link DeleteOnCloseFile}, which deletes
   * the file as soon as {@link DeleteOnCloseFile#close()} is called.
   *
   * <p>It is recommended to use the following pattern: <code>
   * try (DeleteOnCloseFile tempFile = Files.createTempFile(...)) {
   *   // use tempFile.toPath() for writing and reading of the temporary file
   * }
   * </code> The file can be opened and closed multiple times, potentially from different processes.
   *
   * @deprecated Use {@link TempFile#builder()} and {@link
   *     TempFile.TempFileBuilder#createDeleteOnClose()}.
   */
  @Deprecated
  public static DeleteOnCloseFile createTempFile(String prefix, @Nullable String suffix)
      throws IOException {
    TempFileBuilder builder = TempFile.builder().prefix(prefix);
    if (suffix != null) {
      builder.suffix(suffix);
    }
    return builder.createDeleteOnClose();
  }

  /**
   * A simple wrapper around {@link Path} that calls {@link Files#deleteIfExists(Path)} from {@link
   * AutoCloseable#close()}.
   *
   * @deprecated Use {@link TempFile.DeleteOnCloseFile}.
   */
  @Immutable
  @Deprecated
  public static class DeleteOnCloseFile implements AutoCloseable {

    private final Path path;

    DeleteOnCloseFile(Path pFile) {
      path = pFile;
    }

    public Path toPath() {
      return path;
    }

    @Override
    public void close() throws IOException {
      Files.deleteIfExists(path);
    }
  }

  /**
   * Read the full content of a file.
   *
   * @param file The file.
   * @deprecated use {@code asCharSource(file, charset).read()}
   */
  @Deprecated
  @InlineMe(replacement = "com.google.common.io.MoreFiles.asCharSource(file, charset).read()")
  public static String toString(Path file, Charset charset) throws IOException {
    return com.google.common.io.MoreFiles.asCharSource(file, charset).read();
  }

  /**
   * Writes content to a file.
   *
   * @param file The file.
   * @param content The content which shall be written.
   * @deprecated moved to {@link IO}
   */
  @Deprecated
  @InlineMe(
      replacement = "IO.writeFile(file, charset, content)",
      imports = "org.sosy_lab.common.io.IO")
  public static void writeFile(Path file, Charset charset, Object content) throws IOException {
    IO.writeFile(file, charset, content);
  }

  /**
   * Writes content to a file compressed in GZIP format.
   *
   * @param file The file.
   * @param content The content which shall be written.
   * @deprecated moved to {@link IO}
   */
  @Deprecated
  @InlineMe(
      replacement = "IO.writeGZIPFile(file, charset, content)",
      imports = "org.sosy_lab.common.io.IO")
  @SuppressWarnings("MemberName")
  public static void writeGZIPFile(Path file, Charset charset, Object content) throws IOException {
    IO.writeGZIPFile(file, charset, content);
  }

  /**
   * Open a buffered Writer to a file. This method creates necessary parent directories beforehand.
   *
   * @deprecated moved to {@link IO}
   */
  @Deprecated
  public static Writer openOutputFile(Path file, Charset charset, FileWriteMode... options)
      throws IOException {
    return IO.openOutputFile(file, charset, fileWriteModeToOption(options));
  }

  /**
   * Appends content to a file (without overwriting the file, but creating it if necessary).
   *
   * @param file The file.
   * @param content The content which will be written to the end of the file.
   * @deprecated moved to {@link IO}
   */
  @Deprecated
  @InlineMe(
      replacement = "IO.appendToFile(file, charset, content)",
      imports = "org.sosy_lab.common.io.IO")
  public static void appendToFile(Path file, Charset charset, Object content) throws IOException {
    IO.appendToFile(file, charset, content);
  }

  /**
   * Verifies if a file exists, is a normal file and is readable. If this is not the case, a
   * FileNotFoundException with a nice message is thrown.
   *
   * @param path The file to check.
   * @throws FileNotFoundException If one of the conditions is not true.
   * @deprecated moved to {@link IO}
   */
  @Deprecated
  @InlineMe(replacement = "IO.checkReadableFile(path)", imports = "org.sosy_lab.common.io.IO")
  public static void checkReadableFile(Path path) throws FileNotFoundException {
    IO.checkReadableFile(path);
  }

  /** See {@link com.google.common.io.Files#createParentDirs(java.io.File)}. */
  @Deprecated
  @InlineMe(replacement = "com.google.common.io.MoreFiles.createParentDirectories(path)")
  public static void createParentDirs(Path path) throws IOException {
    com.google.common.io.MoreFiles.createParentDirectories(path);
  }
}
