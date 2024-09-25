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

import nl.knaw.dans.dvcli.action.ConsoleReport;
import nl.knaw.dans.dvcli.action.Pair;
import nl.knaw.dans.dvcli.action.SingleIdOrIdsFile;
import nl.knaw.dans.dvcli.action.ThrowingFunction;
import nl.knaw.dans.dvcli.command.AbstractCmd;
import nl.knaw.dans.lib.dataverse.AdminApi;
import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.util.List;

@Command(name = "validate-files",
         mixinStandardHelpOptions = true,
         description = "Make sure that all files are correctly stored in object storage.")
public class DatasetValidateFiles extends AbstractCmd {
    @ParentCommand
    private DatasetCmd datasetCmd;

    protected List<Pair<String, IdParam>> getIds() throws IOException {
        List<String> pids = new SingleIdOrIdsFile(datasetCmd.getTargets(), SingleIdOrIdsFile.DEFAULT_TARGET_PLACEHOLDER).getPids().toList();
        return pids.stream().map(p -> new Pair<>(p, new IdParam(datasetCmd.getDataverseClient().admin(), p))).toList();
    }

    protected record IdParam(AdminApi admin, String id) {
    }

    private static class ValidateFilesAction implements ThrowingFunction<IdParam, String, Exception> {
        @Override
        public String apply(IdParam idParam) throws IOException, DataverseException {
            var r = idParam.admin().validateDatasetFiles(idParam.id);
            return r.getBodyAsString();
        }
    }

    @Override
    public void doCall() throws IOException, DataverseException {
        datasetCmd.<IdParam> paramsBatchProcessorBuilder()
            .labeledItems(getIds())
            .action(new ValidateFilesAction())
            .report(new ConsoleReport<>())
            .build()
            .process();
    }

}
