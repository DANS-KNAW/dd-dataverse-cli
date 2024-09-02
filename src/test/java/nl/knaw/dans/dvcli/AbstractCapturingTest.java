/*
 * Copyright (C) 2024 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.dvcli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public abstract class AbstractCapturingTest {
    private final PrintStream originalStdout = System.out;
    private final PrintStream originalStderr = System.err;
    protected OutputStream stdout;
    protected OutputStream stderr;
    protected ListAppender<ILoggingEvent> logged;

    @AfterEach
    public void tearDown() {

        System.setOut(originalStdout);
        System.setErr(originalStderr);
    }

    @BeforeEach
    public void setUp() {
        stdout = captureStdout();
        stderr = captureStderr();
        logged = captureLog(Level.DEBUG, "nl.knaw.dans");
    }

    public static ListAppender<ILoggingEvent> captureLog(Level error, String loggerName) {
        var logger = (Logger) LoggerFactory.getLogger(loggerName);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.setLevel(error);
        logger.addAppender(listAppender);
        return listAppender;
    }

    public static ByteArrayOutputStream captureStdout() {
        var outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        return outContent;
    }

    public static ByteArrayOutputStream captureStderr() {
        var outContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outContent));
        return outContent;
    }
}
