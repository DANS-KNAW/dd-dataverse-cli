package nl.knaw.dans.dvcli.command;

import nl.knaw.dans.dvcli.action.ConsoleReport;
import nl.knaw.dans.dvcli.action.ThrowingFunction;
import nl.knaw.dans.dvcli.command.AbstractAssignmentRole.RoleAssignmentParams;
import nl.knaw.dans.lib.dataverse.DatasetApi;
import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;

@Command(name = "add",
         mixinStandardHelpOptions = true,
         description = "Assign a role to a user in a dataset.")
public class DatasetRoleAssignmentAdd extends AbstractCmd {
    @ParentCommand
    private DatasetRoleAssignment2 datasetRoleAssignment;

    private static class RoleAssignmentAction implements ThrowingFunction<RoleAssignmentParams<DatasetApi>, String, Exception> {
        @Override
        public String apply(RoleAssignmentParams<DatasetApi> roleAssignmentParams) throws IOException, DataverseException {
            if (roleAssignmentParams.roleAssignment().isPresent()) {
                var r = roleAssignmentParams.pid().assignRole(roleAssignmentParams.roleAssignment().get());
                return r.getEnvelopeAsString();
            }
            return "There was no role to assign.";
        }
    }

    @Override
    public void doCall() throws IOException, DataverseException {
        datasetRoleAssignment.getDatasetCmd().<RoleAssignmentParams<DatasetApi>> paramsBatchProcessorBuilder()
            .labeledItems(datasetRoleAssignment.getDatasetCmd().getItems())
            .action(new RoleAssignmentAction())
            .report(new ConsoleReport<>())
            .build()
            .process();
    }
}
