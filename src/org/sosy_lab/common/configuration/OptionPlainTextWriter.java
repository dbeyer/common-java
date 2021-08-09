// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.errorprone.annotations.Var;
import java.io.PrintStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import org.sosy_lab.common.configuration.OptionCollector.AnnotationInfo;
import org.sosy_lab.common.configuration.OptionCollector.OptionInfo;

/** Class that creates a plain-text documentation of options. */
class OptionPlainTextWriter {

  private static final String COMMENT_PREFIX = "# ";

  private static final int CHARS_PER_LINE = 75; // for description

  private final boolean verbose;
  private final PrintStream out;

  // Keep state between different items:
  // We want to group consecutive options with the same description,
  // and we want to show identical option infos only once.
  private String lastDescription = "";
  private String lastInfo = "";

  OptionPlainTextWriter(boolean pVerbose, PrintStream pOut) {
    verbose = pVerbose;
    out = checkNotNull(pOut);
  }

  /**
   * Write header with copyright and license information.
   *
   * @param copyrights A sorted set of REUSE-compatible copyright declarations
   * @param licenses A sorted set of REUSE-compatible license declarations
   */
  void writeHeader(NavigableSet<String> copyrights, NavigableSet<String> licenses) {
    for (String s : copyrights) {
      out.append(COMMENT_PREFIX).println(s);
    }
    if (!copyrights.isEmpty() && !licenses.isEmpty()) {
      out.println(COMMENT_PREFIX.stripTrailing());
    }
    for (String s : licenses) {
      out.append(COMMENT_PREFIX).println(s);
    }
  }

  /**
   * Write output for a single option.
   *
   * @param allInstances All appearances of this option with the same name.
   */
  void writeOption(Iterable<AnnotationInfo> allInstances) {
    @Var boolean first = true;
    for (AnnotationInfo annotation : allInstances) {
      String description = getOptionDescription(annotation.element());
      if (!description.isEmpty() && !lastDescription.equals(description)) {
        if (first) {
          out.append('\n');
          first = false;
        }
        out.append(description);
        lastDescription = description;
      }
    }
    for (OptionInfo option : FluentIterable.from(allInstances).filter(OptionInfo.class)) {
      String infoText = getOptionInfo(option);
      if (!lastInfo.equals(infoText)) {
        out.append(infoText);
        lastInfo = infoText;
      }
    }
  }

  /**
   * This function returns the formatted description of an {@link Option}.
   *
   * @param element field with the option
   */
  static String getOptionDescription(AnnotatedElement element) {
    @Var String text;
    if (element.isAnnotationPresent(Option.class)) {
      text = element.getAnnotation(Option.class).description();
    } else if (element.isAnnotationPresent(Options.class)) {
      text = element.getAnnotation(Options.class).description();
    } else {
      throw new AssertionError();
    }

    if (element.isAnnotationPresent(Deprecated.class)) {
      text = "DEPRECATED: " + text;
    }

    return formatText(text);
  }

  /** This function returns the formatted information about an {@link Option}. */
  private String getOptionInfo(OptionInfo info) {
    StringBuilder optionInfo = new StringBuilder(200);
    optionInfo.append(info.name());

    if (verbose) {
      if (info.element() instanceof Field) {
        optionInfo.append("\n  field:    ").append(((Field) info.element()).getName()).append('\n');
      } else if (info.element() instanceof Method) {
        optionInfo
            .append("\n  method:   ")
            .append(((Method) info.element()).getName())
            .append('\n');
      }

      Class<?> cls = ((Member) info.element()).getDeclaringClass();
      optionInfo
          .append("  class:    ")
          .append(cls.toString().substring(6))
          .append("\n  type:     ")
          .append(info.type().getSimpleName())
          .append("\n  default value: ");
      if (!info.defaultValue().isEmpty()) {
        optionInfo.append(info.defaultValue());
      } else {
        optionInfo.append("not available");
      }

    } else {
      if (!info.defaultValue().isEmpty()) {
        optionInfo.append(" = ").append(info.defaultValue());
      } else {
        optionInfo.append(" = no default value");
      }
    }
    optionInfo.append('\n');
    appendAllowedValues(info.element(), info.type(), optionInfo);

    return optionInfo.toString();
  }

  /**
   * This function formats text and splits lines, if they are too long. This functions adds "#"
   * before each line.
   */
  private static String formatText(String text) {
    return formatText(text, COMMENT_PREFIX, /* useLineStartInFirstLine= */ true);
  }

  /** This function formats text and splits lines, if they are too long. */
  private static String formatText(String text, String lineStart, boolean useLineStartInFirstLine) {
    checkNotNull(lineStart);
    if (text.isEmpty()) {
      return text;
    }

    // split lines into more lines, if they are too long
    List<String> splittedLines = new ArrayList<>();
    for (String fullLine : Splitter.on('\n').split(text)) {
      @Var String remainingLine = fullLine;
      while (remainingLine.length() > CHARS_PER_LINE) {

        @Var int spaceIndex = remainingLine.lastIndexOf(' ', CHARS_PER_LINE);
        if (spaceIndex == -1) {
          spaceIndex = remainingLine.indexOf(' ');
        }
        if (spaceIndex == -1) {
          spaceIndex = remainingLine.length() - 1;
        }

        String start = remainingLine.substring(0, spaceIndex);
        if (!start.isEmpty()) {
          splittedLines.add(start);
        }
        remainingLine = remainingLine.substring(spaceIndex + 1);
      }
      splittedLines.add(remainingLine);
    }

    // remove last element, if empty (useful if previous line is too long)
    if (splittedLines.get(splittedLines.size() - 1).isEmpty()) {
      splittedLines.remove(splittedLines.size() - 1);
    }

    // add "# " before each line
    StringBuilder formattedLines = new StringBuilder();
    if (!useLineStartInFirstLine && !splittedLines.isEmpty()) {
      formattedLines.append(splittedLines.remove(0));
      formattedLines.append('\n');
    }
    for (String line : splittedLines) {
      formattedLines.append(lineStart);
      formattedLines.append(line);
      formattedLines.append('\n');
    }

    return formattedLines.toString();
  }

  /**
   * This function returns the allowed values or interval for a field.
   *
   * @param field field with the {@link Option}-annotation
   */
  private void appendAllowedValues(AnnotatedElement field, Class<?> type, StringBuilder str) {
    // if the type is enum,
    // the allowed values can be extracted the enum-class
    if (type.isEnum()) {
      Object[] enums = type.getEnumConstants();
      String[] enumTitles = new String[enums.length];
      for (int i = 0; i < enums.length; i++) {
        enumTitles[i] = ((Enum<?>) enums[i]).name();
      }
      str.append("  enum:     ")
          .append(
              formatText(
                  Arrays.toString(enumTitles),
                  "             ",
                  /* useLineStartInFirstLine= */ false));
    }

    appendOptionValues(field, str);
    appendClassOptionValues(field, str);
    appendFileOptionValues(field, str);
    appendIntegerOptionValues(field, str);
    appendTimeSpanOptionValues(field, str);
  }

  /**
   * This method returns text representing the values, that are defined in the {@link
   * Option}-annotation.
   */
  private void appendOptionValues(AnnotatedElement field, StringBuilder str) {
    Option option = field.getAnnotation(Option.class);
    assert option != null;
    if (option.values().length != 0) {
      str.append("  allowed values: ").append(Arrays.toString(option.values())).append('\n');
    }

    if (verbose) {
      if (!option.regexp().isEmpty()) {
        str.append("  regexp:   ").append(option.regexp()).append('\n');
      }
      if (option.toUppercase()) {
        str.append("  uppercase: true\n");
      }
    }
  }

  /**
   * This method returns text representing the values, that are defined in the {@link
   * ClassOption}-annotation.
   */
  private void appendClassOptionValues(AnnotatedElement field, StringBuilder str) {
    ClassOption classOption = field.getAnnotation(ClassOption.class);
    if (classOption != null) {
      if (verbose && classOption.packagePrefix().length != 0) {
        str.append("  packagePrefix: ");
        Joiner.on(", ").appendTo(str, classOption.packagePrefix());
        str.append('\n');
      }
    }
  }

  /**
   * This method returns text representing the values, that are defined in the {@link
   * FileOption}-annotation.
   */
  private void appendFileOptionValues(AnnotatedElement field, StringBuilder str) {
    FileOption fileOption = field.getAnnotation(FileOption.class);
    if (fileOption != null) {
      if (verbose) {
        str.append("  type of file: ").append(fileOption.value()).append('\n');
      }
    }
  }

  /**
   * This method returns text representing the values, that are defined in the {@link
   * IntegerOption}-annotation.
   */
  private void appendIntegerOptionValues(AnnotatedElement field, StringBuilder str) {
    IntegerOption intOption = field.getAnnotation(IntegerOption.class);
    if (intOption != null) {
      if (verbose) {
        if (intOption.min() == Long.MIN_VALUE) {
          str.append("  min:      Long.MIN_VALUE\n");
        } else {
          str.append("  min:      ").append(intOption.min()).append('\n');
        }
        if (intOption.max() == Long.MAX_VALUE) {
          str.append("  max:      Long.MAX_VALUE\n");
        } else {
          str.append("  max:      ").append(intOption.max()).append('\n');
        }
      }
    }
  }

  /**
   * This method returns text representing the values, that are defined in the {@link
   * TimeSpanOption}-annotation.
   */
  private void appendTimeSpanOptionValues(AnnotatedElement field, StringBuilder str) {
    TimeSpanOption timeSpanOption = field.getAnnotation(TimeSpanOption.class);
    if (timeSpanOption != null) {
      if (verbose) {
        str.append("  code unit:     ")
            .append(timeSpanOption.codeUnit())
            .append("\n  default unit:  ")
            .append(timeSpanOption.defaultUserUnit())
            .append('\n');
        if (timeSpanOption.min() == Long.MIN_VALUE) {
          str.append("  time min:      Long.MIN_VALUE\n");
        } else {
          str.append("  time min:      ").append(timeSpanOption.min()).append('\n');
        }
        if (timeSpanOption.max() == Long.MAX_VALUE) {
          str.append("  time max:      Long.MAX_VALUE\n");
        } else {
          str.append("  time max:      ").append(timeSpanOption.max()).append('\n');
        }
      }
    }
  }
}
