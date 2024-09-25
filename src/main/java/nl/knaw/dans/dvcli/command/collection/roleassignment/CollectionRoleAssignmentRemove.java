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
package nl.knaw.dans.dvcli.command.collection.roleassignment;

import nl.knaw.dans.dvcli.action.ConsoleReport;
import nl.knaw.dans.dvcli.action.ThrowingFunction;
import nl.knaw.dans.dvcli.command.AbstractRoleAssignmentSubcommand;
import nl.knaw.dans.dvcli.command.collection.CollectionCmd;
import nl.knaw.dans.lib.dataverse.DataverseApi;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.util.Optional;

@Command(name = "remove",
         mixinStandardHelpOptions = true,
         description = "Remove a role assignment from a collection.")
public class CollectionRoleAssignmentRemove extends AbstractRoleAssignmentSubcommand<CollectionCmd, DataverseApi> {
    @ParentCommand
    protected CollectionRoleAssignment collectionRoleAssignment;

    @Override
    protected DataverseApi getItem(String alias) {
        return collectionRoleAssignment.getCollectionCmd().getDataverseClient().dataverse(alias);
    }

    @Override
    public void doCall() throws Exception {
        collectionRoleAssignment.getCollectionCmd().<RoleAssignmentParams<DataverseApi>> paramsBatchProcessorBuilder()
            .labeledItems(getRoleAssignmentParams(collectionRoleAssignment.getCollectionCmd()))
            .action(new RemoveAssignmentAction())
            .report(new ConsoleReport<>())
            .build()
            .process();
    }

    private static class RemoveAssignmentAction implements ThrowingFunction<RoleAssignmentParams<DataverseApi>, String, Exception> {
        @Override
        public String apply(RoleAssignmentParams<DataverseApi> roleAssignmentParams) throws IOException, DataverseException {
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
}
