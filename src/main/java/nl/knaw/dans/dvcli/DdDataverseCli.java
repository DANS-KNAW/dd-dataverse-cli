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

package nl.knaw.dans.dvcli;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvcli.command.CollectionAssignRole;
import nl.knaw.dans.dvcli.command.CollectionCmd;
import nl.knaw.dans.dvcli.command.CollectionCreateDataset;
import nl.knaw.dans.dvcli.command.CollectionDelete;
import nl.knaw.dans.dvcli.command.CollectionGetContents;
import nl.knaw.dans.dvcli.command.CollectionGetStorageSize;
import nl.knaw.dans.dvcli.command.CollectionImportDataset;
import nl.knaw.dans.dvcli.command.CollectionIsMetadataBlocksRoot;
import nl.knaw.dans.dvcli.command.CollectionListMetadataBlocks;
import nl.knaw.dans.dvcli.command.CollectionListRoleAssignments;
import nl.knaw.dans.dvcli.command.CollectionListRoles;
import nl.knaw.dans.dvcli.command.CollectionPublish;
import nl.knaw.dans.dvcli.command.CollectionSetMetadataBlocksRoot;
import nl.knaw.dans.dvcli.command.CollectionView;
import nl.knaw.dans.dvcli.command.DatasetCmd;
import nl.knaw.dans.dvcli.command.DeleteDraft;
import nl.knaw.dans.dvcli.command.NotificationTruncate;
import nl.knaw.dans.dvcli.config.DdDataverseCliConfig;
import nl.knaw.dans.lib.util.AbstractCommandLineApp;
import nl.knaw.dans.lib.util.PicocliVersionProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "dataverse",
         mixinStandardHelpOptions = true,
         versionProvider = PicocliVersionProvider.class,
         description = "Command-line client for the Dataverse API")
@Slf4j
public class DdDataverseCli extends AbstractCommandLineApp<DdDataverseCliConfig> {
    public static void main(String[] args) throws Exception {
        new DdDataverseCli().run(args);
    }

    public String getName() {
        return "Command-line client for the Dataverse API";
    }

    @Override
    public void configureCommandLine(CommandLine commandLine, DdDataverseCliConfig config) {
        log.debug("Building Dataverse client");
        var dataverseClient = config.getDataverse().build();
        commandLine.addSubcommand(new CommandLine(new CollectionCmd(dataverseClient))
                .addSubcommand(new CollectionAssignRole())
                .addSubcommand(new CollectionCreateDataset())
                .addSubcommand(new CollectionDelete())
                .addSubcommand(new CollectionGetContents())
                .addSubcommand(new CollectionGetStorageSize())
                .addSubcommand(new CollectionImportDataset())
                .addSubcommand(new CollectionIsMetadataBlocksRoot())
                .addSubcommand(new CollectionListMetadataBlocks())
                .addSubcommand(new CollectionListRoleAssignments())
                .addSubcommand(new CollectionListRoles())
                .addSubcommand(new CollectionPublish())
                .addSubcommand(new CollectionSetMetadataBlocksRoot())
                .addSubcommand(new CollectionView()))
            .addSubcommand(new CommandLine(new DatasetCmd(dataverseClient))
                .addSubcommand(new DeleteDraft())
            )
            .addSubcommand(new CommandLine(new NotificationTruncate()));
        log.debug("Configuring command line");
    }
}
