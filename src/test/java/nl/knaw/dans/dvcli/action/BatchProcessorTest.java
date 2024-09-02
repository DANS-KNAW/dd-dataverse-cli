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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import nl.knaw.dans.dvcli.AbstractCapturingTest;
import nl.knaw.dans.lib.dataverse.DatasetApi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BatchProcessorTest extends AbstractCapturingTest {
    public static Stream<String> messagesOf(ListAppender<ILoggingEvent> logged) {
        return logged.list.stream().map(iLoggingEvent -> iLoggingEvent.getLevel() + "  " + iLoggingEvent.getFormattedMessage());
    }

    @Test
    public void batchProcessor_should_continue_after_failure() {
        var mockedDatasetApi = Mockito.mock(DatasetApi.class);

        BatchProcessor.<DatasetApi, String> builder()
            .labeledItems(List.of(
                new Pair<>("a", Mockito.mock(DatasetApi.class)),
                new Pair<>("b", mockedDatasetApi),
                new Pair<>("c", Mockito.mock(DatasetApi.class))
            ))
            .action(datasetApi -> {
                if (!datasetApi.equals(mockedDatasetApi))
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
                           a: OK. b: FAILED: Exception type = RuntimeException, message = test
                           c: OK.""" + " "); // java text block trims trailing spaces
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 3
            ok
            DEBUG Sleeping for 1 ms
            INFO  Processing item 2 of 3
            DEBUG Sleeping for 1 ms
            INFO  Processing item 3 of 3
            ok
            INFO  Finished batch processing of 3 items
            """);
        assertThat(messagesOf(logged))
            .containsExactly("INFO  Starting batch processing",
                "INFO  Processing item 1 of 3",
                "DEBUG  Sleeping for 1 ms",
                "INFO  Processing item 2 of 3",
                "DEBUG  Sleeping for 1 ms",
                "INFO  Processing item 3 of 3",
                "INFO  Finished batch processing of 3 items");
    }

    @Test
    public void batchProcessor_sleep_a_default_amount_of_time_only_between_processing() {
        BatchProcessor.<DatasetApi, String> builder()
            .labeledItems(List.of(
                new Pair<>("a", Mockito.mock(DatasetApi.class)),
                new Pair<>("b", Mockito.mock(DatasetApi.class)),
                new Pair<>("c", Mockito.mock(DatasetApi.class))
            ))
            .action(datasetApi -> "ok")
            .report(new ConsoleReport<>())
            .build()
            .process();

        assertThat(stderr.toString())
            .isEqualTo("a: OK. b: OK. c: OK. ");
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 3
            ok
            DEBUG Sleeping for 1000 ms
            INFO  Processing item 2 of 3
            ok
            DEBUG Sleeping for 1000 ms
            INFO  Processing item 3 of 3
            ok
            INFO  Finished batch processing of 3 items
            """);
    }

    @Test
    public void batchProcessor_should_not_report_sleeping() {
        BatchProcessor.<DatasetApi, String> builder()
            .labeledItems(List.of(
                new Pair<>("A", Mockito.mock(DatasetApi.class)),
                new Pair<>("B", Mockito.mock(DatasetApi.class)),
                new Pair<>("C", Mockito.mock(DatasetApi.class))
            ))
            .action(datasetApi -> "ok")
            .delay(0L)
            .report(new ConsoleReport<>())
            .build()
            .process();

        assertThat(stderr.toString()).isEqualTo("A: OK. B: OK. C: OK. ");
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 3
            ok
            INFO  Processing item 2 of 3
            ok
            INFO  Processing item 3 of 3
            ok
            INFO  Finished batch processing of 3 items
            """);
    }

    @Test
    public void batchProcessor_uses_a_default_report() {
        BatchProcessor.<DatasetApi, String> builder()
            .labeledItems(List.of(
                new Pair<>("X", Mockito.mock(DatasetApi.class)),
                new Pair<>("Y", Mockito.mock(DatasetApi.class)),
                new Pair<>("Z", Mockito.mock(DatasetApi.class))
            ))
            .action(datasetApi -> "ok")
            .delay(0L)
            .build()
            .process();

        assertThat(stderr.toString()).isEqualTo("X: OK. Y: OK. Z: OK. ");
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 3
            ok
            INFO  Processing item 2 of 3
            ok
            INFO  Processing item 3 of 3
            ok
            INFO  Finished batch processing of 3 items
            """);
    }

    @Test
    public void batchProcessor_reports_empty_list() {
        BatchProcessor.<DatasetApi, String> builder()
            .labeledItems(List.of())
            .action(datasetApi -> "ok")
            .report(new ConsoleReport<>())
            .build()
            .process();

        assertThat(stderr.toString()).isEqualTo("");
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Finished batch processing of 0 items
            """);
        assertThat(messagesOf(logged)).containsExactly(
            "INFO  Starting batch processing",
            "INFO  Finished batch processing of 0 items");
    }

    @Test
    public void batchProcessor_throws_on_missing_list() {
        var processor = BatchProcessor.<DatasetApi, String> builder()
            .action(datasetApi -> "ok")
            .report(new ConsoleReport<>());

        assertThatThrownBy(processor::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("labeledItems is marked non-null but is null");

        assertThat(stderr.toString()).isEqualTo("");
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(messagesOf(logged)).containsExactly();
    }

    @Test
    public void batchProcessor_fails_fast_on_missing_action() {
        var processor = BatchProcessor.<DatasetApi, String> builder()
            .labeledItems(List.of());
        assertThatThrownBy(processor::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("action is marked non-null but is null");

        assertThat(stderr.toString()).isEqualTo("");
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(messagesOf(logged)).containsExactly();
    }
}
