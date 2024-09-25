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
package nl.knaw.dans.dvcli.command.collection;

import nl.knaw.dans.dvcli.command.AbstractCmd;
import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Command(name = "create-dataset",
         mixinStandardHelpOptions = true,
         description = "Create a dataset in a dataverse collection.")
public class CollectionCreateDataset extends AbstractCmd {
    @ParentCommand
    private CollectionCmd collectionCmd;

    @Parameters(index = "0", paramLabel = "dataset", description = "A JSON file with the dataset description.")
    private String dataset;

    @Option(names = { "-m", "--mdkeys" }, paramLabel = "metadataKeys", description = "Maps the names of the metadata blocks to their 'secret' key values")
    private Map<String, String> metadataKeys = new HashMap<>();

    @Override
    public void doCall() throws IOException, DataverseException {
        collectionCmd.batchProcessor(c ->
            c.createDataset(Files.readString(Path.of(dataset)), metadataKeys).getEnvelopeAsString()).process();
    }
}
