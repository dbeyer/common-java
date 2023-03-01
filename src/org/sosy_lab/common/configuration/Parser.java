// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.converters.TypeConverter;
import org.sosy_lab.common.io.IO;

/**
 * A parser for a simple configuration file format based on "key = value" pairs.
 *
 * <p>The configuration file will always be interpreted as UTF-8.
 *
 * <p>Supported features:
 *
 * <ul>
 *   <li>Comments at line beginnings with '# ' (with space!) and '//'
 *   <li>Whitespace ignored before comments and around keys and values
 *   <li>Key format is at least one word consisting of a-zA-Z0-9_+-, words are separated by '.'.
 *       Example: foo.bar
 *   <li>Keys may be specified exactly once per file.
 *   <li>Options with a common prefix of words in their key can be put into a section so that the
 *       prefix does not need to be written each time (similar to Windows-Ini-files). Example:
 *       <code>
 *   [foo]
 *   bar = abc
 *   </code> is equal to <code>foo.bar = abc</code>
 *   <li>Options before the first section start or in a section with an empty name have no such
 *       prefix.
 *   <li>Inside the value, put '\' at the line end to append the next line to the current value (not
 *       possible in other places like key or section start). Whitespace at the beginning and end of
 *       all lines will be removed, so indentation is possible.
 *   <li>Other files can be included (recursively) with {@literal #include <FILE>}. If the file name
 *       is a relative one, it is considered relative to the directory of the current file.
 *       Directives in the current file will always overwrite included directives, no matter of
 *       their placement. Directives from an included file will overwrite directives from previously
 *       included files. Circular inclusions are now allowed.
 * </ul>
 */
final class Parser {

  static final class InvalidConfigurationFileException extends InvalidConfigurationException {

    private static final long serialVersionUID = 8146907093750189669L;

    private InvalidConfigurationFileException(
        String msg, int lineno, @Nullable Path source, String line) {
      super(msg + " in line " + lineno + (source != null ? " of " + source : "") + ": " + line);
    }

    private InvalidConfigurationFileException(String msg) {
      super(msg);
    }
  }

  private static final Pattern OPTION_NAME =
      Pattern.compile("^[a-zA-Z0-9_+-]+(\\.[a-zA-Z0-9_+-]+)*((::required)?)$");

  private final Map<String, String> options = new HashMap<>();
  private final Map<String, Path> sources = new HashMap<>();

  // inclusion stack for finding circular includes
  private final Deque<String> includeStack = new ArrayDeque<>();

  private Parser() {}

  /** Get the map with all configuration directives in the parsed file. */
  Map<String, String> getOptions() {
    return Collections.unmodifiableMap(options);
  }

  /** Get the map with the source location of each defined option. */
  Map<String, Path> getSources() {
    return Collections.unmodifiableMap(sources);
  }

  /**
   * Parse a configuration file with the format as defined above.
   *
   * @param file The file to parse.
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  @CheckReturnValue
  static Parser parse(Path file) throws IOException, InvalidConfigurationException {

    Parser parser = new Parser();
    parser.parse0(file);
    verify(parser.includeStack.isEmpty());
    return parser;
  }

  /**
   * Parse a configuration file with the format as defined above.
   *
   * @param file The file to parse.
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  private void parse0(Path file) throws IOException, InvalidConfigurationException {
    IO.checkReadableFile(file);

    String fileName = file.toAbsolutePath().toString();
    if (includeStack.contains(fileName)) {
      throw new InvalidConfigurationFileException(
          "Circular inclusion of file " + file.toAbsolutePath());
    }
    includeStack.addLast(fileName);

    try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      parse(r, Optional.of(file), file);
    }
    includeStack.removeLast();
  }

  /**
   * Parse a configuration file given as a {@link CharSource} with the format as defined above.
   *
   * <p>A stream from this source is opened and closed by this method. This method may additionally
   * access more files from the file system if they are included.
   *
   * @param source The source to read the file from.
   * @param basePath If #include filenames are relative, resolve them as sibling of basePath. Source
   *     must not contain #include if absent.
   * @param sourcePath An optional Path to use as source of the options in error messages or for
   *     other uses by the {@link TypeConverter}.
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  @CheckReturnValue
  static Parser parse(CharSource source, Optional<Path> basePath, @Nullable Path sourcePath)
      throws IOException, InvalidConfigurationException {

    Parser parser = new Parser();
    try (BufferedReader r = source.openBufferedStream()) {
      parser.parse(r, basePath, sourcePath);
    }
    verify(parser.includeStack.isEmpty());
    return parser;
  }

  /**
   * Parse a configuration file given as a {@link BufferedReader} with the format as defined above.
   *
   * <p>The reader is left open after this method returns. This method may additionally access more
   * files from the file system if they are included.
   *
   * @param r The reader to read the file from.
   * @param basePath If #include filenames are relative, resolve them as sibling of basePath. Source
   *     must not contain #include if absent.
   * @param source An optional Path to use as source of the options in error messages or for other
   *     uses by the {@link TypeConverter}.
   * @throws IOException If an I/O error occurs.
   * @throws InvalidConfigurationException If the configuration file has an invalid format.
   */
  @SuppressFBWarnings(
      value = "SBSC_USE_STRINGBUFFER_CONCATENATION",
      justification = "performance irrelevant compared to I/O, String much more convenient")
  private void parse(BufferedReader r, Optional<Path> basePath, @Nullable Path source)
      throws IOException, InvalidConfigurationException {
    checkNotNull(basePath);

    @Var String line;
    @Var int lineno = 0;
    @Var String currentPrefix = "";
    @Var String currentOptionName = null;
    @Var String currentValue = null;
    Map<String, String> definedOptions = new HashMap<>();

    while ((line = r.readLine()) != null) {
      lineno++;
      line = line.trim();
      String fullLine = line;

      assert (currentValue == null) == (currentOptionName == null);

      if (currentValue != null) {
        // we are in the continuation of a key = value pair
        currentValue += line;

        // no continue here, we need to run the code at the end of the loop body

      } else if (line.isEmpty()
          || line.equals("#")
          || line.startsWith("# ")
          || line.startsWith("//")) {
        // empty or comment
        continue;

      } else if (line.startsWith("#")) {
        // it is a parser directive
        // currently only #include is supported.

        if (!line.startsWith("#include")) {
          throw new InvalidConfigurationFileException(
              "Illegal parser directive", lineno, source, fullLine);
        }

        line = line.substring("#include".length()).trim();
        if (line.isEmpty()) {
          throw new InvalidConfigurationFileException(
              "Include without filename", lineno, source, fullLine);
        }

        checkArgument(
            basePath.isPresent(),
            "File %s contains #include directive, but base path not given.",
            source);

        // parse included file (content will be in fields of this class)
        parse0(basePath.orElseThrow().resolveSibling(line));
        continue;

      } else if (line.startsWith("[") && line.endsWith("]")) {
        // category
        line = line.substring(1, line.length() - 1);
        line = line.trim();

        if (line.isEmpty()) {
          // this is allowed, it clears the prefix
          currentPrefix = "";

        } else if (!OPTION_NAME.matcher(line).matches()) {
          throw new InvalidConfigurationFileException(
              "Invalid category \"" + line + "\"", lineno, source, fullLine);

        } else {
          currentPrefix = line + ".";
        }
        continue;

      } else if (line.length() < 3) {
        throw new InvalidConfigurationFileException("Illegal content", lineno, source, fullLine);

      } else {
        // normal key=value line
        String[] bits = line.split("=", 2);
        if (bits.length != 2) {
          throw new InvalidConfigurationFileException(
              "Missing key-value separator", lineno, source, fullLine);
        }

        currentOptionName = bits[0].trim();
        if (!OPTION_NAME.matcher(currentOptionName).matches()) {
          throw new InvalidConfigurationFileException(
              "Invalid option \"" + currentOptionName + "\"", lineno, source, fullLine);
        }
        if (definedOptions.containsKey(currentPrefix + currentOptionName)) {
          throw new InvalidConfigurationFileException(
              "Duplicate option \"" + currentPrefix + currentOptionName + "\"",
              lineno,
              source,
              fullLine);
        }

        currentValue = bits[1].trim();
      }

      assert (currentValue != null) && (currentOptionName != null);

      if (currentValue.endsWith("\\")) {
        // continuation
        currentValue = currentValue.substring(0, currentValue.length() - 1);

      } else {
        definedOptions.put(currentPrefix + currentOptionName, currentValue);
        currentValue = null;
        currentOptionName = null;
      }
    }

    assert (currentValue == null) == (currentOptionName == null);

    if (currentValue != null) {
      definedOptions.put(currentPrefix + currentOptionName, currentValue);
    }

    // now overwrite included options with local ones
    options.putAll(definedOptions);

    if (source != null) {
      sources.putAll(Maps.asMap(definedOptions.keySet(), Functions.constant(source)));
    } else {
      sources.keySet().removeAll(definedOptions.keySet());
    }
  }
}
