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

import nl.knaw.dans.dvcli.AbstractCapturingTest;
import nl.knaw.dans.dvcli.action.Pair;
import nl.knaw.dans.dvcli.command.collection.CollectionCmd;
import nl.knaw.dans.dvcli.command.dataset.DatasetCmd;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AbstractSubcommandContainerTest extends AbstractCapturingTest {
    private static final Logger log = LoggerFactory.getLogger(AbstractSubcommandContainerTest.class);

    private static class TestCmd extends AbstractSubcommandContainer<Object> {

        public TestCmd(String targets) {
            super(new DataverseClient(null));
            // Inject target with reflection
            try {
                var targetsField = AbstractSubcommandContainer.class.getDeclaredField("targets");
                targetsField.setAccessible(true);
                targetsField.set(this, targets);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                log.error("Failed to inject targets", e);
            }
        }

        @Override
        protected List<Pair<String, Object>> getItems() throws IOException {
            return List.of(
                new Pair<>("1", "value of 1")
            );
        }
    }

    @Test
    public void datasetCmd_with_dir_as_targets_file_throws() throws Exception {

        var cmd = new DatasetCmd(new DataverseClient(null)){
            @Override
            public void doCall() throws IOException {
                getItems();
            }
        };

        // set private field
        var targetField = AbstractSubcommandContainer.class.getDeclaredField("targets");
        targetField.setAccessible(true);
        targetField.set(cmd, "src/test/resources");

        assertThatThrownBy(cmd::doCall)
            .isInstanceOf(IOException.class)
            .hasMessage("src/test/resources is not a regular file");

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void collectionCmd_with_dir_as_targets_file_throws() throws Exception {

        var cmd = new CollectionCmd(new DataverseClient(null)) {

            @Override
            public void doCall() throws IOException {
                log.debug("doCall");
                getItems();
            }
        };

        // set private field
        var targetField = AbstractSubcommandContainer.class.getDeclaredField("targets");
        targetField.setAccessible(true);
        targetField.set(cmd, "src/test/resources");

        assertThatThrownBy(cmd::doCall)
            .isInstanceOf(IOException.class)
            .hasMessage("src/test/resources is not a regular file");

        assertThat(stdout.toString()).isEqualTo("DEBUG doCall\n");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void call_should_return_one_on_a_dataverseException_by_doCall() throws Exception {
        var cmd = new TestCmd("1") {

            @Override
            public void doCall() throws DataverseException {
                throw new DataverseException(999, "test");
            }
        };

        assertThat(cmd.call()).isEqualTo(1);

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("""
            status: 999; message: test
            """);
    }

    @Test
    public void call_should_throw_on_an_ioException_by_doCall() {
        var cmd = new TestCmd("1") {

            @Override
            public void doCall() throws IOException {
                throw new IOException("test");
            }
        };

        assertThatThrownBy(cmd::call)
            .isInstanceOf(IOException.class)
            .hasMessage("test");

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void call_should_throw_on_an_runtimeException_by_doCall() {
        var cmd = new TestCmd("1") {

            @Override
            public void doCall() {
                throw new RuntimeException("test");
            }
        };

        assertThatThrownBy(cmd::call)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("test");

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void call_should_return_zero_with_the_default_doCall_which_does_nothing() throws Exception {
        var cmd = new TestCmd("1");

        assertThat(cmd.call()).isEqualTo(0);

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void batchProcessorBuilder_throws_when_getItems_throws_ioException() {
        var cmd = new TestCmd("1") {

            @Override
            protected List<Pair<String, Object>> getItems() throws IOException {
                throw new IOException("test");
            }
        };

        assertThatThrownBy(cmd::batchProcessorBuilder)
            .isInstanceOf(IOException.class)
            .hasMessage("test");

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void batchProcessorBuilder_throws_when_getItems_throws_runtimeException() {
        var cmd = new TestCmd("1") {

            @Override
            protected List<Pair<String, Object>> getItems() {
                throw new RuntimeException("test");
            }
        };

        assertThatThrownBy(cmd::batchProcessorBuilder)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("test");

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void batchProcessorBuilder_can_build_without_labeled_items() throws Exception {
        var builder = new TestCmd("1")
            .batchProcessorBuilder()
            .action(Object::toString);

        assertDoesNotThrow(builder::build);

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void paramsBatchProcessorBuilder_requires_labeled_items() {
        var builder = new TestCmd("1")
            .paramsBatchProcessorBuilder()
            .action(Object::toString);

        assertThatThrownBy(builder::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("labeledItems is marked non-null but is null");

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void paramsBatchProcessorBuilder_can_build_with_labeled_items() throws Exception {
        var cmd = new TestCmd("1");
        var builder = cmd
            .paramsBatchProcessorBuilder()
            .labeledItems(cmd.getItems())
            .action(Object::toString);

        assertDoesNotThrow(builder::build);

        assertThat(logged.list).isEmpty();
        assertThat(stdout.toString()).isEqualTo("");
        assertThat(stderr.toString()).isEqualTo("");
    }

    @Test
    public void batchProcessor_does_not_throw() {
        assertDoesNotThrow(() ->
            new TestCmd("1")
                .batchProcessor(Object::toString)
                .process()
        );

        assertThat(stderr.toString()).isEqualTo("1: OK. ");
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 1
            value of 1
            INFO  Finished batch processing of 1 items
            """);
        assertThat(logged.list.stream().map(Object::toString).toList()).containsExactly(
            "[INFO] Starting batch processing",
            "[INFO] Processing item 1 of 1",
            "[INFO] Finished batch processing of 1 items"
        );
    }
}
