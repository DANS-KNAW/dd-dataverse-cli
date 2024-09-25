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
import nl.knaw.dans.dvcli.command.collection.CollectionCmd;
import nl.knaw.dans.dvcli.command.collection.CollectionCreateDataset;
import nl.knaw.dans.lib.dataverse.DataverseApi;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import nl.knaw.dans.lib.dataverse.DataverseHttpResponse;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetCreationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CollectionCreateDatasetTest extends AbstractCapturingTest {

    private final InputStream originalStdin = System.in;

    @AfterEach
    public void tearDown() {
        System.setIn(originalStdin);
    }

    @Test
    public void doCall_continues_on_unknownHost() throws Exception {

        var unknownHostConfig = new DataverseClientConfig(new URI("https://does.not.exist.dans.knaw.nl"), "apiToken");
        var client = new DataverseClient(unknownHostConfig);

        var metadataKeys = new HashMap<String, String>();
        var json = "src/test/resources/debug-etc/config.yml"; // invalid json file, don't care
        System.setIn(new ByteArrayInputStream("A B C".getBytes()));

        // command under test
        var cmd = getCmd("-", metadataKeys, json, client);
        cmd.doCall();

        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 3
            DEBUG buildUri: https://does.not.exist.dans.knaw.nl/api/dataverses/A/datasets
            INFO  Processing item 2 of 3
            DEBUG buildUri: https://does.not.exist.dans.knaw.nl/api/dataverses/B/datasets
            INFO  Processing item 3 of 3
            DEBUG buildUri: https://does.not.exist.dans.knaw.nl/api/dataverses/C/datasets
            INFO  Finished batch processing of 3 items
            """);
        assertThat(stderr.toString()).isEqualTo("""
            A: FAILED: Exception type = UnknownHostException, message = does.not.exist.dans.knaw.nl: Name or service not known
            B: FAILED: Exception type = UnknownHostException, message = does.not.exist.dans.knaw.nl
            C: FAILED: Exception type = UnknownHostException, message = does.not.exist.dans.knaw.nl
            """); // TODO implement fail fast in BatchProcessor for these type of exceptions?
    }

    @Test
    public void doCall_is_happy() throws Exception {

        var metadataKeys = new HashMap<String, String>();
        var jsonFile = "src/test/resources/debug-etc/config.yml"; // invalid json file, don't care

        // mock objects
        @SuppressWarnings("unchecked")
        DataverseHttpResponse<DatasetCreationResult> response = Mockito.mock(DataverseHttpResponse.class);
        var client = Mockito.mock(DataverseClient.class);
        var api = Mockito.mock(DataverseApi.class);

        // mock behavior
        Mockito.when(client.dataverse("A")).thenReturn(api);
        Mockito.when(api.createDataset(Files.readString(Path.of(jsonFile)), metadataKeys)).thenReturn(response);
        Mockito.when(response.getEnvelopeAsString()).thenReturn("mock response");

        // command under test
        CollectionCreateDataset cmd = getCmd("A", metadataKeys, jsonFile, client);
        cmd.doCall();

        assertThat(stderr.toString()).isEqualTo("A: OK. ");
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 1
            mock response
            INFO  Finished batch processing of 1 items
            """);

        verify(api, times(1)).createDataset((String) any(), any());
        verify(response, times(1)).getEnvelopeAsString();
        verify(client, times(1)).dataverse(any());
        verifyNoMoreInteractions(api);
    }

    @Test
    public void doCall_with_dir_as_json_file_fails() throws Exception {

        var metadataKeys = new HashMap<String, String>();
        var jsonFile = "src/test/resources/debug-etc";
        var client = new DataverseClient(new DataverseClientConfig(null));
        var originalIn = System.in;
        System.setIn(new ByteArrayInputStream("A B".getBytes()));

        try {
            // command under test
            CollectionCreateDataset cmd = getCmd("-", metadataKeys, jsonFile, client);
            cmd.doCall();
        }
        finally {
            System.setIn(originalIn);
        }

        assertThat(stderr.toString()).isEqualTo("""
            A: FAILED: Exception type = IOException, message = Is a directory
            B: FAILED: Exception type = IOException, message = Is a directory
            """);
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 2
            INFO  Processing item 2 of 2
            INFO  Finished batch processing of 2 items
            """);
    }

    private static CollectionCreateDataset getCmd(String target, HashMap<String, String> metadataKeys, String json, final DataverseClient client)
        throws NoSuchFieldException, IllegalAccessException {

        // set private fields with reflection

        var cmd = new CollectionCmd(client);

        var targetsField = AbstractSubcommandContainer.class.getDeclaredField("targets");
        targetsField.setAccessible(true);
        targetsField.set(cmd, target);

        var subCmd = new CollectionCreateDataset();

        var datasetField = CollectionCreateDataset.class.getDeclaredField("dataset");
        datasetField.setAccessible(true);
        datasetField.set(subCmd, json);

        var metadataKeysField = CollectionCreateDataset.class.getDeclaredField("metadataKeys");
        metadataKeysField.setAccessible(true);
        metadataKeysField.set(subCmd, metadataKeys);

        var collectionCmdField = CollectionCreateDataset.class.getDeclaredField("collectionCmd");
        collectionCmdField.setAccessible(true);
        collectionCmdField.set(subCmd, cmd);

        return subCmd;
    }
}
