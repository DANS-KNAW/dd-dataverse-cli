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
import nl.knaw.dans.lib.dataverse.DataverseClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import static nl.knaw.dans.dvcli.action.SingleIdOrIdsFile.DEFAULT_TARGET_PLACEHOLDER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings({ "unchecked", "rawtypes" }) // for mapSecondToString
public class SingleOrTest extends AbstractTestWithTestDir {
    // SingleDatasetOrDatasetsFile implicitly tests SingleIdOrIdsFile
    // SingleCollectionOrCollectionsFile too and has little to add

    private final InputStream originalStdin = System.in;

    @AfterEach
    public void tearDown() {
        System.setIn(originalStdin);
    }

    public static <A> Stream<Pair<String, A>> mapSecondToString(List<Pair<String, A>> collections) {
        return collections.stream().map(p -> new Pair(p.getFirst(), p.getSecond().toString()));
    }

    @Test
    public void getCollections_should_return_single_value() throws Exception {

        var collections = new SingleCollectionOrCollectionsFile("xyz", new DataverseClient(null))
            .getCollections().toList();

        assertThat(mapSecondToString(collections)).containsExactly(
            new Pair("xyz", "DataverseApi(subPath=api/dataverses/xyz)")
        );
    }

    @Test
    public void getPids_should_return_placeHolder() throws Exception {
        var pids = new SingleIdOrIdsFile(DEFAULT_TARGET_PLACEHOLDER, "default")
            .getPids();
        Assertions.assertThat(pids)
            .containsExactlyInAnyOrderElementsOf(List.of("default"));
    }

    @Test
    public void getDatasetIds_should_return_single_dataset_in_aList() throws Exception {
        var datasets = new SingleDatasetOrDatasetsFile("1", new DataverseClient(null))
            .getDatasets().toList();
        assertThat(mapSecondToString(datasets))
            .containsExactly(new Pair("1", "DatasetApi(id='1, isPersistentId=false)"));
    }

    @Test
    public void getDatasets_should_parse_file_with_white_space() throws Exception {

        var filePath = testDir.resolve("ids.txt");
        Files.createDirectories(testDir);
        Files.writeString(filePath, """
            a blabla
            1""");

        var datasets = new SingleDatasetOrDatasetsFile(filePath.toString(), new DataverseClient(null))
            .getDatasets().toList();

        assertThat(mapSecondToString(datasets)).containsExactly(
            new Pair("a", "DatasetApi(id='a, isPersistentId=true)"),
            new Pair("blabla", "DatasetApi(id='blabla, isPersistentId=true)"),
            new Pair("1", "DatasetApi(id='1, isPersistentId=false)")
        );
    }

    @Test
    public void getDatasets_should_throw_when_parsing_a_directory() {

        var ids = new SingleDatasetOrDatasetsFile("src/test/resources", new DataverseClient(null));
        assertThatThrownBy(ids::getDatasets)
            .isInstanceOf(IOException.class)
            .hasMessage("src/test/resources is not a regular file");
    }

    @Test
    public void getDatasets_should_parse_stdin_and_return_empty_lines() throws Exception {

        System.setIn(new ByteArrayInputStream("""
            A
                        
            B rabarbera
                        
            """.getBytes()));

        var datasets = new SingleDatasetOrDatasetsFile("-", new DataverseClient(null))
            .getDatasets().toList();
        assertThat(mapSecondToString(datasets)).containsExactly(
            new Pair("A", "DatasetApi(id='A, isPersistentId=true)"),
            new Pair("", "DatasetApi(id=', isPersistentId=true)"),
            new Pair("B", "DatasetApi(id='B, isPersistentId=true)"),
            new Pair("rabarbera", "DatasetApi(id='rabarbera, isPersistentId=true)"),
            new Pair("", "DatasetApi(id=', isPersistentId=true)")
        );
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored") // for toList in assertThatThrownBy
    public void getDatasets_should_read_until_Exception() throws Exception {

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
