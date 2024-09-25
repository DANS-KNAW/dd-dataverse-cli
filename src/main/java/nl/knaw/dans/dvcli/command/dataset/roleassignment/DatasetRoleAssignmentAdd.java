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
package nl.knaw.dans.dvcli.command.dataset.roleassignment;

import nl.knaw.dans.dvcli.action.ConsoleReport;
import nl.knaw.dans.dvcli.action.ThrowingFunction;
import nl.knaw.dans.dvcli.command.AbstractRoleAssignmentSubcommand;
import nl.knaw.dans.dvcli.command.dataset.DatasetCmd;
import nl.knaw.dans.lib.dataverse.DatasetApi;
import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;

@Command(name = "add",
         mixinStandardHelpOptions = true,
         description = "Assign a role to a user in a dataset.")
public class DatasetRoleAssignmentAdd extends AbstractRoleAssignmentSubcommand<DatasetCmd, DatasetApi> {
    @ParentCommand
    protected DatasetRoleAssignment datasetRoleAssignment;

    @Override
    protected DatasetApi getItem(String pid) {
        return datasetRoleAssignment.getDatasetCmd().getDataverseClient().dataset(pid);
    }

    private static class AddAssignmentAction implements ThrowingFunction<RoleAssignmentParams<DatasetApi>, String, Exception> {
        @Override
        public String apply(RoleAssignmentParams<DatasetApi> roleAssignmentParams) throws IOException, DataverseException {
            if (roleAssignmentParams.roleAssignment().isPresent()) {
                var r = roleAssignmentParams.pid().assignRole(roleAssignmentParams.roleAssignment().get());
                return r.getEnvelopeAsString();
            }
            throw new IllegalArgumentException("There was no role to assign.");
        }
    }

    @Override
    public void doCall() throws IOException, DataverseException {
        datasetRoleAssignment.getDatasetCmd().<RoleAssignmentParams<DatasetApi>> paramsBatchProcessorBuilder()
            .labeledItems(getRoleAssignmentParams(datasetRoleAssignment.getDatasetCmd()))
            .action(new AddAssignmentAction())
            .report(new ConsoleReport<>())
            .build()
            .process();
    }
}
