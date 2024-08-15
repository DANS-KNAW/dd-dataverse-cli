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

import nl.knaw.dans.dvcli.AbstractTestWithTestDir;
import nl.knaw.dans.lib.dataverse.DatasetApi;
import nl.knaw.dans.lib.dataverse.DataverseApi;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import static nl.knaw.dans.dvcli.action.SingleIdOrIdsFile.DEFAULT_TARGET_PLACEHOLDER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SingleOrTest extends AbstractTestWithTestDir {
    // SingleDatasetOrDatasetsFile implicitly tests SingleIdOrIdsFile
    // SingleCollectionOrCollectionsFile too and has little to add

    private final InputStream originalStdin = System.in;

    @AfterEach
    public void tearDown() {
        System.setIn(originalStdin);
    }

    @Test
    public void getCollections_should_return_single_value() throws Exception {
        var alias = "xyz";

        var collections = new SingleCollectionOrCollectionsFile(alias, getClient())
            .getCollections().toList();

        assertThat(collections).hasSize(1);
        var kv = collections.get(0);
        assertThat(kv.getFirst()).isEqualTo(alias);
        assertThat(kv.getSecond()).isInstanceOf(DataverseApi.class);
        // TODO a DataverseApi.toString() showing the subPath would be nice
    }

    private static DataverseClient getClient() throws URISyntaxException {
        var baseUrl = new URI("http://does.not.exist.dans.knaw.nl");
        return new DataverseClient(new DataverseClientConfig(baseUrl, "apiTokenValue"));
    }

    @Test
    public void getPids_should_return_placeHolder() throws IOException {
        var pids = new SingleIdOrIdsFile(DEFAULT_TARGET_PLACEHOLDER, "default")
            .getPids();
        Assertions.assertThat(pids)
            .containsExactlyInAnyOrderElementsOf(List.of("default"));
    }

    @Test
    public void getDatasetIds_should_return_single_dataset_in_aList() throws IOException {
        var pid = "1";

        var dataverseClient = mock(DataverseClient.class);
        var dataset = mock(DatasetApi.class);
        // note that a numeric value is used for the API call
        Mockito.when(dataverseClient.dataset(1)).thenReturn(dataset);

        var datasets = new SingleDatasetOrDatasetsFile(pid, dataverseClient)
            .getDatasets();
        assertThat(datasets).containsExactly(new Pair<>(pid, dataset));

        verify(dataverseClient, times(1)).dataset(1);
        verifyNoMoreInteractions(dataverseClient);
    }

    @Test
    public void getDatasets_should_parse_file_with_white_space() throws IOException {

        var dataverseClient = mock(DataverseClient.class);

        var datasetA = mock(DatasetApi.class);
        Mockito.when(dataverseClient.dataset("a")).thenReturn(datasetA);
        var datasetBlabla = mock(DatasetApi.class);
        Mockito.when(dataverseClient.dataset("blabla")).thenReturn(datasetBlabla);
        var dataset1 = mock(DatasetApi.class); // note the numeric value
        Mockito.when(dataverseClient.dataset(1)).thenReturn(dataset1);

        var filePath = testDir.resolve("ids.txt");
        Files.createDirectories(testDir);
        Files.writeString(filePath, """
            a blabla
            1""");

        var datasets = new SingleDatasetOrDatasetsFile(filePath.toString(), dataverseClient)
            .getDatasets();
        Assertions.assertThat(datasets.toList())
            .containsExactlyInAnyOrderElementsOf(List.of(
                new Pair<>("a", datasetA),
                new Pair<>("1", dataset1),
                new Pair<>("blabla", datasetBlabla)
            ));

        verify(dataverseClient, times(1)).dataset("a");
        verify(dataverseClient, times(1)).dataset(1);
        verify(dataverseClient, times(1)).dataset("blabla");
        verifyNoMoreInteractions(dataverseClient);
    }

    @Test
    public void getDatasets_should_parse_stdin_and_return_empty_lines() throws IOException {

        var dataverseClient = mock(DataverseClient.class);

        var datasetA = mock(DatasetApi.class);
        Mockito.when(dataverseClient.dataset("A")).thenReturn(datasetA);
        var datasetBlank = mock(DatasetApi.class);
        Mockito.when(dataverseClient.dataset("")).thenReturn(datasetBlank);
        var datasetBlabla = mock(DatasetApi.class);
        Mockito.when(dataverseClient.dataset("rabarbera")).thenReturn(datasetBlabla);
        var dataset1 = mock(DatasetApi.class);
        Mockito.when(dataverseClient.dataset("B")).thenReturn(dataset1);

        System.setIn(new ByteArrayInputStream("""
            A
                        
            B rabarbera
                        
            """.getBytes()));

        var datasets = new SingleDatasetOrDatasetsFile("-", dataverseClient)
            .getDatasets();
        Assertions.assertThat(datasets.toList())
            .containsExactlyInAnyOrderElementsOf(List.of(
                new Pair<>("A", datasetA),
                new Pair<>("B", dataset1),
                new Pair<>("rabarbera", datasetBlabla),
                new Pair<>("", datasetBlank),
                new Pair<>("", datasetBlank)
            ));

        verify(dataverseClient, times(1)).dataset("A");
        verify(dataverseClient, times(1)).dataset("B");
        verify(dataverseClient, times(1)).dataset("rabarbera");
        verify(dataverseClient, times(2)).dataset("");
        verifyNoMoreInteractions(dataverseClient);
    }

    @Test
    public void getDatasets_should_read_until_Exception() throws IOException {

        var dataverseClient = mock(DataverseClient.class);

        Mockito.when(dataverseClient.dataset("A"))
            .thenReturn(mock(DatasetApi.class));
        Mockito.when(dataverseClient.dataset("whoops"))
            .thenThrow(new RuntimeException("test"));

        System.setIn(new ByteArrayInputStream("""
            A
            whoops
            B""".getBytes()));

        var datasets = new SingleDatasetOrDatasetsFile("-", dataverseClient)
            .getDatasets();
        assertThatThrownBy(datasets::toList)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("test");

        verify(dataverseClient, times(1)).dataset("A");
        verify(dataverseClient, times(1)).dataset("whoops");
        verify(dataverseClient, times(0)).dataset("B");
        verifyNoMoreInteractions(dataverseClient);
    }
}
