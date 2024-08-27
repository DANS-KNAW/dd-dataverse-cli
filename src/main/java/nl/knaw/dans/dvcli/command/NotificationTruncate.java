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

import picocli.CommandLine;

@CommandLine.Command(name = "truncate-notifications",
        mixinStandardHelpOptions = true,
        description = "Remove user notifications but keep up to a specified amount.")
public class NotificationTruncate extends AbstractCmd {
    // dataverse truncate-notifications {--user <uid>|--all-users } <number-of-records-to-keep>

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    UserOptions users;

    static class UserOptions {
        @CommandLine.Option(names = { "--user" }, required = true, description = "The user whose notifications to truncate.")
        private String user;
        @CommandLine.Option(names = { "--all-users" }, required = true, description = "Truncate notifications for all users.")
        private boolean allUser;
    }
    
    @CommandLine.Parameters(index = "0", paramLabel = "number-of-records-to-keep", description = "The number of notifications to keep.")
    private int numberOfRecordsToKeep;
    
    @Override
    public void doCall() {
        // show commandline input
        System.out.println("Truncating notifications...");
        System.out.println("Number of records to keep: " + numberOfRecordsToKeep);
        System.out.println("User: " + (users.allUser ? "all users" : users.user));
        
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
