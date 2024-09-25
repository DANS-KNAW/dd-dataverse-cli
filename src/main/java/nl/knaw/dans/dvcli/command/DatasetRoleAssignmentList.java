package nl.knaw.dans.dvcli.command;

import nl.knaw.dans.lib.dataverse.DataverseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;

@Command(name = "list",
         mixinStandardHelpOptions = true,
         description = "List role assignments for the specified dataset.")
public class DatasetRoleAssignmentList extends AbstractCmd {
    @ParentCommand
    private DatasetRoleAssignment2 datasetRoleAssignment;

    @Override
    public void doCall() throws IOException, DataverseException {
        datasetRoleAssignment.getDatasetCmd().batchProcessor(d -> d.listRoleAssignments().getEnvelopeAsString()).process();
    }
}
