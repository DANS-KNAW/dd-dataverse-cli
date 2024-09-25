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

import nl.knaw.dans.dvcli.command.dataset.DatasetCmd;
import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;

@Command(name = "get-version",
         mixinStandardHelpOptions = true,
         description = "Returns an object containing the dataset version metadata.")
public class DatasetGetVersion extends AbstractCmd {
    @ParentCommand
    private DatasetCmd datasetCmd;

    @ArgGroup(exclusive = true, multiplicity = "1")
    VersionInfo versionInfo;

    static class VersionInfo {
        @Parameters(description = "Specified a version to retrieve.")
        String version = "";
        @Option(names = "--all", description = "Get all versions")
        boolean allVersions;
    }

    @Override
    public void doCall() throws IOException, DataverseException {
        datasetCmd.batchProcessor(d ->
                versionInfo.allVersions ? d.getAllVersions().getEnvelopeAsString() : d.getVersion(versionInfo.version).getEnvelopeAsString()
            )
            .process();
    }
}
