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
package nl.knaw.dans.dvcli.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import nl.knaw.dans.dvcli.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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
        stdout = TestUtils.captureStdout();
        stderr = TestUtils.captureStderr();
        logged = TestUtils.captureLog(Level.DEBUG, "nl.knaw.dans");
    }
}
