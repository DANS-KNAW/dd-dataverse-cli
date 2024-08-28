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
import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;

@Command(name = "assign-role",
         mixinStandardHelpOptions = true,
         description = "Manage role assignments on one or more datasets.")

public class DatasetAssignRole extends AbstractAssignmentRole {
    @ParentCommand
    DatasetCmd datasetCmd;

    private static class RoleAssignmentAction implements ThrowingFunction<RoleAssignmentParams, String, Exception> {
        @Override
        public String apply(RoleAssignmentParams roleAssignmentParams) throws IOException, DataverseException {
            if (roleAssignmentParams.roleAssignment().isPresent()) {
                var r = roleAssignmentParams.pid().assignRole(roleAssignmentParams.roleAssignment().get());
                return r.getEnvelopeAsString();
            }
            return "Nothing to Do";
        }
    }

    @Override
    public void doCall() throws IOException, DataverseException {
        datasetCmd.<RoleAssignmentParams> paramsBatchProcessorBuilder()
            .labeledItems(getRoleAssignmentParams(datasetCmd))
            .action(new RoleAssignmentAction())
            .report(new ConsoleReport<>())
            .build()
            .process();
    }

}
