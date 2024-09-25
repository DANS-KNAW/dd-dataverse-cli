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

import nl.knaw.dans.dvcli.action.ConsoleReport;
import nl.knaw.dans.dvcli.action.ThrowingFunction;
import nl.knaw.dans.dvcli.command.collection.CollectionCmd;
import nl.knaw.dans.lib.dataverse.DataverseApi;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.util.Optional;

@Command(name = "role-assignment",
         mixinStandardHelpOptions = true,
         description = "Manage role assignments.",
         subcommands = { CollectionRoleAssignment.CollectionListRoleAssignments.class, CollectionRoleAssignment.CollectionAssignRole.class, CollectionRoleAssignment.CollectionDeleteRole.class })
public class CollectionRoleAssignment extends AbstractCmd {
    @ParentCommand
    static private CollectionCmd collectionCmd;

    @Command(name = "list",
             mixinStandardHelpOptions = true,
             description = "List the role assignments of a Dataverse collection.")
    static class CollectionListRoleAssignments extends AbstractCmd {

        @Override
        public void doCall() throws IOException, DataverseException {
            collectionCmd.batchProcessor(d -> d.listRoleAssignments().getEnvelopeAsString()).process();
        }
    }

    @Command(name = "add",
             mixinStandardHelpOptions = true,
             description = "Assign a role to a user in a Dataverse collection.")
    static class CollectionAssignRole extends AbstractAssignmentRole<CollectionCmd, DataverseApi> {
        @Override
        protected DataverseApi getItem(String pid) {
            return collectionCmd.dataverseClient.dataverse(pid);
        }

        private static class RoleAssignmentAction implements ThrowingFunction<RoleAssignmentParams<DataverseApi>, String, Exception> {
            @Override
            public String apply(RoleAssignmentParams<DataverseApi> roleAssignmentParams) throws IOException, DataverseException {
                if (roleAssignmentParams.roleAssignment().isPresent()) {
                    var r = roleAssignmentParams.pid().assignRole(roleAssignmentParams.roleAssignment().get());
                    return r.getEnvelopeAsString();
                }
                return "There was no assignment-role to assign.";
            }
        }

        @Override
        public void doCall() throws IOException, DataverseException {
            collectionCmd.<RoleAssignmentParams<DataverseApi>> paramsBatchProcessorBuilder()
                .labeledItems(getRoleAssignmentParams(collectionCmd))
                .action(new RoleAssignmentAction())
                .report(new ConsoleReport<>())
                .build()
                .process();
        }
    }

    @Command(name = "remove",
             mixinStandardHelpOptions = true,
             description = "remove a role assignment from the specified dataverse collection")
    static class CollectionDeleteRole extends AbstractAssignmentRole<CollectionCmd, DataverseApi> {

        @Override
        protected DataverseApi getItem(String pid) {
            return collectionCmd.dataverseClient.dataverse(pid);
        }

        private static class RoleAssignmentAction implements ThrowingFunction<RoleAssignmentParams<DataverseApi>, String, Exception> {
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
                }
                return "There was no assignment-role to assign.";
            }
        }

        @Override
        public void doCall() throws IOException, DataverseException {
            collectionCmd.<RoleAssignmentParams<DataverseApi>> paramsBatchProcessorBuilder()
                .labeledItems(getRoleAssignmentParams(collectionCmd))
                .action(new CollectionRoleAssignment.CollectionDeleteRole.RoleAssignmentAction())
                .report(new ConsoleReport<>())
                .build()
                .process();
        }
    }

    @Override
    public void doCall() throws IOException, DataverseException {

    }
}
