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

import nl.knaw.dans.dvcli.action.Pair;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Command
public abstract class AbstractAssignmentRole<CMD extends AbstractSubcommandContainer<?>, API> extends AbstractCmd {

    static class CommandParameter {
        @Parameters(description = "Alias and role assignee (example: @dataverseAdmin=contributor)")
        String assignment = "";

        @Option(names = { "-f",
            "--parameter-file" }, description = "CSV file to read parameters from. The file should have a header row with columns 'PID', 'ASSIGNEE' and 'ROLE'.")
        Path parameterFile;
    }

    @ArgGroup(multiplicity = "1")
    CommandParameter commandParameter;

    private Optional<RoleAssignment> readFromCommandLine() {
        if (!this.commandParameter.assignment.isEmpty() && this.commandParameter.assignment.contains("=")) {
            String[] assigneeRole = this.commandParameter.assignment.split("=");

            RoleAssignment roleAssignment = new RoleAssignment();
            roleAssignment.setAssignee(assigneeRole[0]);
            roleAssignment.setRole(assigneeRole[1]);
            return Optional.of(roleAssignment);
        }
        return Optional.empty();
    }

    private List<Pair<String, RoleAssignmentParams<API>>> readFromFile(CMD cmd) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(commandParameter.parameterFile);
            CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("PID", "ASSIGNEE", "ROLE")
                .setSkipHeaderRecord(true)
                .build())) {

            List<Pair<String, RoleAssignmentParams<API>>> result = new ArrayList<>();

            for (CSVRecord csvRecord : csvParser) {
                var pid = csvRecord.get("PID");
                API api = getItem(pid);
                RoleAssignment roleAssignment = new RoleAssignment();
                roleAssignment.setAssignee(csvRecord.get("ASSIGNEE"));
                roleAssignment.setRole(csvRecord.get("ROLE"));

                RoleAssignmentParams<API> params = new RoleAssignmentParams<>(api, Optional.of(roleAssignment));
                result.add(new Pair<>(pid, params));
            }

            return result;
        }
    }

    protected abstract API getItem(String pid);

    protected List<Pair<String, RoleAssignmentParams<API>>> getRoleAssignmentParams(CMD cmd) throws IOException {
        if (commandParameter.parameterFile != null) {
            return readFromFile(cmd);
        }
        else if (commandParameter.assignment != null) {
            var items = cmd.getItems();
            return items.stream()
                .map(p -> {
                    var second = new RoleAssignmentParams<API>((API) p.getSecond(), readFromCommandLine());
                    return new Pair<>(p.getFirst(), second);
                })
                .toList();
        }
        return List.of();
    }

    protected record RoleAssignmentParams<A>(A pid, Optional<RoleAssignment> roleAssignment) {
    }
}
