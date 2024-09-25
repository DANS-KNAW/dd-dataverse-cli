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
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.util.Optional;

@Command(name = "remove",
         mixinStandardHelpOptions = true,
         description = "remove role assignment from specified dataset(s)")
public class DatasetRoleAssignmentRemove extends AbstractRoleAssignmentSubcommand<DatasetCmd, DatasetApi> {
    @ParentCommand
    protected DatasetRoleAssignment datasetRoleAssignment;

    @Override
    protected DatasetApi getItem(String pid) {
        return datasetRoleAssignment.getDatasetCmd().getDataverseClient().dataset(pid);
    }

    private static class RemoveAssignmentAction implements ThrowingFunction<RoleAssignmentParams<DatasetApi>, String, Exception> {
        @Override
        public String apply(RoleAssignmentParams<DatasetApi> roleAssignmentParams) throws IOException, DataverseException {
            if (roleAssignmentParams.roleAssignment().isPresent()) {
                RoleAssignment roleAssignment = roleAssignmentParams.roleAssignment().get();
                Optional<RoleAssignmentReadOnly> role = roleAssignmentParams.pid().listRoleAssignments().getData().stream()
                    .filter(r -> r.get_roleAlias().equals(roleAssignment.getRole()) && r.getAssignee().equals(roleAssignment.getAssignee())).findFirst();
                if (role.isPresent()) {
                    var r = roleAssignmentParams.pid().deleteRoleAssignment(role.get().getId());
                    return r.getEnvelopeAsString();
                }
                else {
                    throw new IllegalArgumentException("Role assignment not found.");
                }
            }
            throw new IllegalArgumentException("No role assignment to remove provided.");
        }
    }

    @Override
    public void doCall() throws IOException, DataverseException {
        datasetRoleAssignment.getDatasetCmd().<RoleAssignmentParams<DatasetApi>> paramsBatchProcessorBuilder()
            .labeledItems(getRoleAssignmentParams(datasetRoleAssignment.getDatasetCmd()))
            .action(new RemoveAssignmentAction())
            .report(new ConsoleReport<>())
            .build()
            .process();
    }
}
