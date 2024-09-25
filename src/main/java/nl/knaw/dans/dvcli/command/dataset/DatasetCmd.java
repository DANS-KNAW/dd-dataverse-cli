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
package nl.knaw.dans.dvcli.command.dataset;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvcli.action.Pair;
import nl.knaw.dans.dvcli.action.SingleDatasetOrDatasetsFile;
import nl.knaw.dans.dvcli.command.AbstractSubcommandContainer;
import nl.knaw.dans.lib.dataverse.DatasetApi;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Command(name = "dataset",
         mixinStandardHelpOptions = true,
         description = "Manage Dataverse datasets")
@Slf4j
public class DatasetCmd extends AbstractSubcommandContainer<DatasetApi> {
    public DatasetCmd(@NonNull DataverseClient dataverseClient) {
        super(dataverseClient);
    }

    @Override
    protected List<Pair<String, DatasetApi>> getItems() throws IOException {
        return new SingleDatasetOrDatasetsFile(getTargets(), dataverseClient).getDatasets().collect(Collectors.toList());
    }
}
