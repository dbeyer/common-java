/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common.configuration.converters;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.sosy_lab.common.configuration.Configuration.defaultConfiguration;

import com.google.common.base.Ascii;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.converters.FileTypeConverterTest.FileTypeConverterBasicTest;
import org.sosy_lab.common.configuration.converters.FileTypeConverterTest.FileTypeConverterSafeModeTest;
import org.sosy_lab.common.configuration.converters.FileTypeConverterTest.FileTypeConverterUnsafeModeTest;

@RunWith(Suite.class)
@SuiteClasses({
  FileTypeConverterSafeModeTest.class,
  FileTypeConverterUnsafeModeTest.class,
  FileTypeConverterBasicTest.class
})
public class FileTypeConverterTest {

  private FileTypeConverterTest() {}

  public static class FileTypeConverterBasicTest {

    @Test
    public void testgetInstanceForNewConfiguration() throws InvalidConfigurationException {
      Configuration config1 =
          Configuration.builder()
              .setOption("rootDirectory", "root")
              .setOption("output.path", "output")
              .build();
      FileTypeConverter conv1 = FileTypeConverter.createWithSafePathsOnly(config1);

      Configuration config2 = Configuration.builder().setOption("output.path", "output2").build();
      FileTypeConverter conv2 = conv1.getInstanceForNewConfiguration(config2);

      assertThat(conv2.rootPath).isEqualTo(conv1.rootPath);
      assertThat(conv2.safePathsOnly).isEqualTo(conv1.safePathsOnly);
      assertThat(conv2.outputPath).isEqualTo(Paths.get("root", "output2"));
    }
  }

  @Options
  static class FileInjectionTestOptions {
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    @Option(secure = true, description = "none", name = "test.path")
    Path path;
  }

  public static class FileTypeConverterSafeModeTest extends FileTypeConverterTestBase {

    @Override
    FileTypeConverter createFileTypeConverter(Configuration pConfig)
        throws InvalidConfigurationException {
      return FileTypeConverter.createWithSafePathsOnly(pConfig);
    }

    @Override
    boolean isAllowed(boolean isInFile) {
      return isInFile ? isSafeWhenInConfigFile : isSafe;
    }

    @BeforeClass
    public static void skipOnWindows() {
      assume().withMessage("Safe mode not supported on Windows").that(isWindows()).isFalse();
    }
  }

  public static class FileTypeConverterUnsafeModeTest extends FileTypeConverterTestBase {

    @Override
    FileTypeConverter createFileTypeConverter(Configuration pConfig)
        throws InvalidConfigurationException {
      return FileTypeConverter.create(pConfig);
    }

    @Override
    boolean isAllowed(boolean isInFile) {
      return true;
    }
  }

  @RunWith(Parameterized.class)
  public abstract static class FileTypeConverterTestBase {

    @Parameters(name = "{0} (safe={1}, safeInFile={2})")
    public static List<Object[]> testPaths() {
      String tmpDir =
          Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value() + File.separator).toString();
      List<Object[]> tests =
          Lists.newArrayList(
              new Object[][] {
                // path and whether it is allowed in safe mode and when included from config/file
                {"/etc/passwd", false, false},
                {"relative/dir" + File.pathSeparator + "/etc", false, false},
                {"file", true, true},
                {"dir/../file", true, true},
                {"./dir/file", true, true},
                {"../dir", false, true},
                {"dir/../../file", false, true},
                {"../../file", false, false},
                {"dir/../../../file", false, false},
                {tmpDir + "/file", true, true},
                {tmpDir + "/../file", false, false},
              });
      if (!isWindows()) {
        tests.add(new Object[] {"file::name", false, false});
        tests.add(new Object[] {"file::name:illegal", false, false});
        tests.add(new Object[] {"file:::illegal", false, false});
      }
      return tests;
    }

    protected static boolean isWindows() {
      return Ascii.toLowerCase(StandardSystemProperty.OS_NAME.value()).contains("windows");
    }

    @Parameter(0)
    public String testPath;

    @Parameter(1)
    public boolean isSafe;

    @Parameter(2)
    public boolean isSafeWhenInConfigFile;

    @Rule public final ExpectedException thrown = ExpectedException.none();

    @Options
    static class FileInjectionTestOptions {
      @FileOption(Type.OPTIONAL_INPUT_FILE)
      @Option(secure = true, description = "none", name = "test.path")
      Path path;
    }

    abstract FileTypeConverter createFileTypeConverter(Configuration config)
        throws InvalidConfigurationException;

    abstract boolean isAllowed(boolean isInFile);

    private void expectExceptionAbout(String... msgParts) {
      thrown.expect(InvalidConfigurationException.class);
      thrown.expectMessage(testPath.replace('/', File.separatorChar));
      for (String part : msgParts) {
        thrown.expectMessage(part);
      }
    }

    @Test
    public void testCheckSafePath() throws InvalidConfigurationException {
      FileTypeConverter conv = createFileTypeConverter(defaultConfiguration());

      Path path = Paths.get(testPath);

      if (!isAllowed(false)) {
        expectExceptionAbout("safe mode", "dummy");
      }

      assertThat(conv.checkSafePath(path, "dummy")).isEqualTo(path);
    }

    @Test
    public void testCreation_RootDirectory() throws InvalidConfigurationException {
      Configuration config = Configuration.builder().setOption("rootDirectory", testPath).build();

      if (!isAllowed(false)) {
        expectExceptionAbout("safe mode", "rootDirectory");
      }

      FileTypeConverter conv = createFileTypeConverter(config);
      assertThat(conv.getOutputDirectory())
          .isEqualTo(Paths.get(testPath).resolve("output").toString());
    }

    @Test
    public void testCreation_OutputPath() throws InvalidConfigurationException {
      Configuration config = Configuration.builder().setOption("output.path", testPath).build();

      if (!isAllowed(false)) {
        expectExceptionAbout("output.path");
      }

      FileTypeConverter conv = createFileTypeConverter(config);
      assertThat(conv.getOutputDirectory()).isEqualTo(Paths.get(".").resolve(testPath).toString());
    }

    @Test
    public void testConvert_InjectPath() throws InvalidConfigurationException {
      Configuration config =
          Configuration.builder()
              .setOption("test.path", testPath)
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();

      if (!isAllowed(false)) {
        expectExceptionAbout("safe mode", "test.path");
      }

      config.inject(options);
      assertThat(options.path).isEqualTo(Paths.get(testPath));
    }

    @Test
    public void testConvert_DefaultPath() throws InvalidConfigurationException {
      Configuration config =
          Configuration.builder()
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();
      options.path = Paths.get(testPath);

      if (!isAllowed(false)) {
        expectExceptionAbout("safe mode", "test.path");
      }

      config.inject(options);
      assertThat(options.path).isEqualTo(Paths.get(".").resolve(testPath));
    }

    @Test
    public void testConvert_DefaultPathWithRootDirectory() throws InvalidConfigurationException {
      Configuration configForConverter =
          Configuration.builder()
              .setOption("rootDirectory", "root")
              .setOption("output.path", "output")
              .build();

      Configuration config =
          Configuration.builder()
              .addConverter(FileOption.class, createFileTypeConverter(configForConverter))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();
      options.path = Paths.get(testPath);

      if (!isAllowed(true)) {
        expectExceptionAbout("safe mode", "test.path");
      }

      config.inject(options);
      assertThat(options.path).isEqualTo(Paths.get("root").resolve(testPath));
    }

    @Test
    public void testConvert_InjectPathFromFile() throws InvalidConfigurationException, IOException {
      CharSource configFile = CharSource.wrap("test.path = " + testPath);
      Configuration config =
          Configuration.builder()
              .loadFromSource(configFile, "config", "config/file")
              .addConverter(FileOption.class, createFileTypeConverter(defaultConfiguration()))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();

      if (!isAllowed(true)) {
        expectExceptionAbout("safe mode", "test.path");
      }

      config.inject(options);
      assertThat((Comparable<?>) options.path).isEqualTo(Paths.get("config").resolve(testPath));
    }

    @Test
    public void testConvertDefaultValueFromOtherInstance() throws InvalidConfigurationException {
      Configuration configForConverter =
          Configuration.builder()
              .setOption("rootDirectory", "root")
              .setOption("output.path", "output")
              .build();

      Configuration config =
          Configuration.builder()
              .addConverter(FileOption.class, createFileTypeConverter(configForConverter))
              .build();
      FileInjectionTestOptions options = new FileInjectionTestOptions();
      options.path = Paths.get(testPath);
      FileInjectionTestOptions options2 = new FileInjectionTestOptions();

      if (!isAllowed(false)) {
        expectExceptionAbout("safe mode", "test.path");
      }

      config.injectWithDefaults(options2, FileInjectionTestOptions.class, options);
      assertThat(options2.path).isEqualTo(Paths.get(testPath));
    }
  }
}
