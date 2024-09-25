package nl.knaw.dans.dvcli.command;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "role-assignment",
         mixinStandardHelpOptions = true,
         description = "Manage role assignments.")
public class DatasetRoleAssignment2 extends AbstractCmd {
    @ParentCommand
    @Getter
    private DatasetCmd datasetCmd;

    @Override
    public void doCall() {
        // do nothing
    }
}
