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

import nl.knaw.dans.dvcli.command.AbstractCmd;
import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;

@Command(name = "delete-draft",
         mixinStandardHelpOptions = true,
         description = "Delete the draft version of a dataset.")
public class DatasetDeleteDraft extends AbstractCmd {
    @ParentCommand
    private DatasetCmd datasetCmd;

    @Override
    public void doCall() throws IOException, DataverseException {
        datasetCmd.batchProcessor(d -> d.deleteDraft().getEnvelopeAsString()).process();
    }
}
