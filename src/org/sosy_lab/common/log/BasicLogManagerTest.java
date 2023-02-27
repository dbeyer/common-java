// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.TestLogHandler;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders.AbstractAppender;

public class BasicLogManagerTest {

  private static final int TRUNCATE_SIZE = 150;
  private static final String LONG_STRING = "1234567890".repeat(100_000); // 1 MB size

  @SuppressWarnings("UnnecessaryAnonymousClass")
  private static final Appender LONG_STRING_APPENDER =
      new AbstractAppender() {

        @Override
        public void appendTo(Appendable pAppendable) throws IOException {
          pAppendable.append(LONG_STRING);
        }
      };

  private TestLogHandler testHandler;
  private LogManager logger;

  @Before
  public void setUp() {
    testHandler = new TestLogHandler();
    testHandler.setLevel(Level.INFO);
    logger = BasicLogManager.createWithHandler(testHandler, TRUNCATE_SIZE);
  }

  @After
  public void tearDown() throws Exception {
    if (logger instanceof AutoCloseable) {
      ((AutoCloseable) logger).close();
    }
  }

  @Test
  public void testSupplierIsLazy() {
    AtomicBoolean called = new AtomicBoolean();
    logger.log(
        Level.FINE,
        () -> {
          called.set(true);
          return "";
        });
    assertThat(called.get()).isFalse();
  }

  private static void checkExpectedLogRecordSource(TestLogHandler handler, String methodName) {
    List<LogRecord> records = handler.getStoredLogRecords();
    assertThat(records).hasSize(1);
    assertThat(records.get(0).getSourceClassName()).isEqualTo(BasicLogManagerTest.class.getName());
    assertThat(records.get(0).getSourceMethodName()).isEqualTo(methodName);
  }

  @Test
  public void testLogRecordSource() {
    logger.log(Level.SEVERE, "test");
    checkExpectedLogRecordSource(testHandler, "testLogRecordSource");
  }

  @Test
  public void testLogRecordSourceWithHelperMethod() {
    logIndirectly();
    checkExpectedLogRecordSource(testHandler, "testLogRecordSourceWithHelperMethod");
  }

  private void logIndirectly() {
    // Do not inline this method!
    // Do not rename this method, its name needs to start with "log"!
    logger.log(Level.SEVERE, "test with a helper method for logging that should not be the source");
  }

  @Test
  public void testLogRecordSourceOfException() {
    try {
      throwException();
    } catch (RuntimeException e) {
      logger.logUserException(Level.SEVERE, e, null);
    }

    checkExpectedLogRecordSource(testHandler, "throwException");
  }

  @SuppressWarnings("ThrowSpecificExceptions")
  private static void throwException() {
    throw new RuntimeException();
  }

  @Test
  public void testWithoutSourceInformation() {
    ((BasicLogManager) logger).log0(Level.SEVERE, null, "test without StackTraceElement");
    List<LogRecord> records = testHandler.getStoredLogRecords();
    assertThat(records).hasSize(1);
    assertThat(records.get(0).getSourceClassName()).isNull();
    assertThat(records.get(0).getSourceMethodName()).isNull();
  }

  private void checkExpectedTruncatedMessage(boolean knownSize) {
    List<LogRecord> records = testHandler.getStoredLogRecords();
    assertThat(records).hasSize(1);
    if (knownSize) {
      assertThat(records.get(0).getMessage())
          .isEqualTo(
              String.format(
                  "| %s... <REMAINING ARGUMENT OMITTED BECAUSE %d CHARACTERS LONG> |",
                  LONG_STRING.substring(0, BasicLogManager.TRUNCATE_REMAINING_SIZE),
                  LONG_STRING.length()));
    } else {
      assertThat(records.get(0).getMessage())
          .isEqualTo(
              String.format(
                  "| %s... <REMAINING ARGUMENT OMITTED BECAUSE >= %d CHARACTERS LONG> |",
                  LONG_STRING.substring(0, BasicLogManager.TRUNCATE_REMAINING_SIZE),
                  TRUNCATE_SIZE));
    }
  }

  @Test
  public void testLogTruncate() {
    logger.log(Level.SEVERE, "|", LONG_STRING, "|");
    checkExpectedTruncatedMessage(true);
  }

  @Test
  public void testLogAppenderTruncate() {
    logger.log(Level.SEVERE, "|", LONG_STRING_APPENDER, "|");
    checkExpectedTruncatedMessage(false);
  }

  @Test
  public void testLogfTruncate() {
    logger.logf(Level.SEVERE, "| %s |", LONG_STRING);
    checkExpectedTruncatedMessage(true);
  }

  private void testLongStringWithoutTrunction(Object longMessage) throws Exception {
    tearDown(); // close logger because we need to recreate it
    logger =
        BasicLogManager.createWithHandler(
            testHandler, /* truncateSize= */ 0); // 0 disables truncation
    logger.log(Level.SEVERE, "|", longMessage, "|");
    List<LogRecord> records = testHandler.getStoredLogRecords();
    assertThat(records).hasSize(1);
    assertThat(records.get(0).getMessage()).isEqualTo("| " + LONG_STRING + " |");
  }

  @Test
  public void testLogNoTruncate() throws Exception {
    testLongStringWithoutTrunction(LONG_STRING);
  }

  @Test
  public void testLogAppenderNoTruncate() throws Exception {
    testLongStringWithoutTrunction(LONG_STRING_APPENDER);
  }
}
