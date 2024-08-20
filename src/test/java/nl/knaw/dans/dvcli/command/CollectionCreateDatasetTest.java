package nl.knaw.dans.dvcli.command;

import nl.knaw.dans.dvcli.AbstractCapturingTest;
import nl.knaw.dans.dvcli.action.Pair;
import nl.knaw.dans.lib.dataverse.DataverseApi;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import nl.knaw.dans.lib.dataverse.DataverseHttpResponse;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetCreationResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CollectionCreateDatasetTest extends AbstractCapturingTest {
    @Test
    public void doCall_continues_on_unknownHost() throws Exception {

        var metadataKeys = new HashMap<String, String>();
        var unknownHostConfig = new DataverseClientConfig(new URI("https://does.not.exist.dans.knaw.nl"), "apiToken");
        var cmd = getCmd(
            metadataKeys,
            List.of(
                new Pair<>("A", new DataverseClient(unknownHostConfig).dataverse("A")),
                new Pair<>("B", new DataverseClient(unknownHostConfig).dataverse("B")),
                new Pair<>("C", new DataverseClient(unknownHostConfig).dataverse("B"))
            )
        );

        // method under test
        cmd.doCall();

        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 3
            DEBUG buildUri: https://does.not.exist.dans.knaw.nl/api/dataverses/A/datasets
            INFO  Processing item 2 of 3
            DEBUG buildUri: https://does.not.exist.dans.knaw.nl/api/dataverses/B/datasets
            INFO  Processing item 3 of 3
            DEBUG buildUri: https://does.not.exist.dans.knaw.nl/api/dataverses/B/datasets
            INFO  Finished batch processing of 3 items
            """);
        assertThat(stderr.toString()).isEqualTo("""
            A: FAILED: Exception type = UnknownHostException, message = does.not.exist.dans.knaw.nl: Name or service not known
            B: FAILED: Exception type = UnknownHostException, message = does.not.exist.dans.knaw.nl
            C: FAILED: Exception type = UnknownHostException, message = does.not.exist.dans.knaw.nl
            """); // TODO implement fail fast in BatchProcessor for these type of exceptions?
    }

    @Test
    public void doCall_() throws Exception {

        var metadataKeys = new HashMap<String, String>();

        DataverseHttpResponse<DatasetCreationResult> responseA = Mockito.mock(DataverseHttpResponse.class);
        DataverseHttpResponse<DatasetCreationResult> responseB = Mockito.mock(DataverseHttpResponse.class);
        Mockito.when(responseA.getEnvelopeAsString()).thenReturn("some string");
        Mockito.when(responseB.getEnvelopeAsString()).thenReturn("some other string");

        var api = Mockito.mock(DataverseApi.class);
        Mockito.when(api.createDataset(eq("A"), any())).thenReturn(null);
        Mockito.when(api.createDataset(eq("B"), any())).thenReturn(responseB);
        // TODO the two when's above are not effective, working around as below
        Mockito.when(api.createDataset((String) any(), any())).thenReturn(responseA);

        CollectionCreateDataset cmd = getCmd(
            metadataKeys,
            List.of(
                new Pair<>("A", api),
                new Pair<>("B", api)
            )
        );

        // method under test
        cmd.doCall();

        assertThat(stderr.toString()).isEqualTo("A: OK. B: OK. ");
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 2
            some string
            INFO  Processing item 2 of 2
            some string
            INFO  Finished batch processing of 2 items
            """);

        verify(api, times(2)).createDataset((String) any(), any());
        verifyNoMoreInteractions(api);
    }

    private static CollectionCreateDataset getCmd(HashMap<String, String> metadataKeys, final List<Pair<String, DataverseApi>> pairs) throws NoSuchFieldException, IllegalAccessException {
        var cmd = new CollectionCreateDataset();

        // set private fields

        var datasetField = CollectionCreateDataset.class.getDeclaredField("dataset");
        datasetField.setAccessible(true);
        datasetField.set(cmd, "src/test/resources/debug-etc/config.yml"); // invalid json file

        var metadataKeysField = CollectionCreateDataset.class.getDeclaredField("metadataKeys");
        metadataKeysField.setAccessible(true);
        metadataKeysField.set(cmd, metadataKeys);

        var collectionCmdField = CollectionCreateDataset.class.getDeclaredField("collectionCmd");
        collectionCmdField.setAccessible(true);
        collectionCmdField.set(cmd, new CollectionCmd(new DataverseClient(null)) {

            @Override
            protected List<Pair<String, DataverseApi>> getItems() {
                return pairs;
            }
        });
        return cmd;
    }
}
