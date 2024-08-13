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
package nl.knaw.dans.dvcli.action;

import ch.qos.logback.classic.Level;
import nl.knaw.dans.dvcli.TestUtils;
import nl.knaw.dans.lib.dataverse.DatasetApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.PrintStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchProcessorTest {

    private final PrintStream originalStdout = System.out;
    private final PrintStream originalStderr = System.err;

    @AfterEach
    public void tearDown() {

        System.setOut(originalStdout);
        System.setErr(originalStderr);
    }

    @Test
    public void test() {
        var datasetApi1 = Mockito.mock(DatasetApi.class);
        var datasetApi2 = Mockito.mock(DatasetApi.class);

        var stdout = TestUtils.captureStdout();
        var stderr = TestUtils.captureStderr();
        var logged = TestUtils.captureLog(Level.DEBUG, "nl.knaw.dans");

        BatchProcessor.<DatasetApi, String> builder()
            .labeledItems(List.of(
                new Pair<>("1", datasetApi1),
                new Pair<>("2", datasetApi2)
            ))
            .action(Object::toString)
            .report(new ConsoleReport<>())
            .delay(1L)
            .build()
            .process();

        assertThat(stderr.toString())
            .isEqualTo("1: OK. 2: OK. ");
        assertThat(stdout.toString().split("\n"))
            .containsExactly("INFO  Starting batch processing",
                "INFO  Processing item 1 of 2",
                datasetApi1.toString(),
                "DEBUG Sleeping for 1 ms",
                "INFO  Processing item 2 of 2",
                datasetApi2.toString(),
                "INFO  Finished batch processing"
            );
        assertThat(TestUtils.messagesOf(logged))
            .containsExactly("INFO  Starting batch processing",
                "INFO  Processing item 1 of 2",
                "DEBUG  Sleeping for 1 ms",
                "INFO  Processing item 2 of 2",
                "INFO  Finished batch processing");
    }

    @Test
    public void test_should() {
        var datasetApi1 = Mockito.mock(DatasetApi.class);
        var datasetApi2 = Mockito.mock(DatasetApi.class);
        var datasetApi3 = Mockito.mock(DatasetApi.class);

        var stdout = TestUtils.captureStdout();
        var stderr = TestUtils.captureStderr();
        var logged = TestUtils.captureLog(Level.DEBUG, "nl.knaw.dans");

        BatchProcessor.<DatasetApi, String> builder()
            .labeledItems(List.of(
                new Pair<>("1", datasetApi1),
                new Pair<>("2", datasetApi2),
                new Pair<>("2", datasetApi3)
            ))
            .action(datasetApi -> {
                if (!datasetApi.equals(datasetApi2))
                    return "ok";
                else
                    throw new RuntimeException("test");
            })
            .report(new ConsoleReport<>())
            .delay(1L)
            .build()
            .process();

        assertThat(stderr.toString())
            .isEqualTo("""
                1: OK. 2: FAILED: Exception type = RuntimeException, message = test
                2: OK.""" + " "); // java text block trims trailing spaces
        assertThat(stdout.toString().split("\n"))
            .containsExactly("INFO  Starting batch processing",
                "INFO  Processing item 1 of 3",
                "ok",
                "DEBUG Sleeping for 1 ms",
                "INFO  Processing item 2 of 3",
                "DEBUG Sleeping for 1 ms",
                "INFO  Processing item 3 of 3",
                "ok",
                "INFO  Finished batch processing"
                );
            assertThat(TestUtils.messagesOf(logged))
            .containsExactly("INFO  Starting batch processing",
                "INFO  Processing item 1 of 3",
                "DEBUG  Sleeping for 1 ms",
                "INFO  Processing item 2 of 3",
                "DEBUG  Sleeping for 1 ms",
                "INFO  Processing item 3 of 3",
                "INFO  Finished batch processing");
    }

}
