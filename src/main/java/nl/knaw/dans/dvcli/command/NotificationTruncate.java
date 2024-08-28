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

import nl.knaw.dans.dvcli.action.Database;
import nl.knaw.dans.dvcli.config.DdDataverseDatabaseConfig;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "truncate-notifications",
        mixinStandardHelpOptions = true,
        description = "Remove user notifications but keep up to a specified amount.",
        sortOptions = false)
public class NotificationTruncate extends AbstractCmd {
    // dataverse truncate-notifications {--user <uid>|--all-users } <number-of-records-to-keep>

    DdDataverseDatabaseConfig dbcfg;
    public NotificationTruncate(DdDataverseDatabaseConfig dbcfg) {
        this.dbcfg = dbcfg;
    }

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
        if (numberOfRecordsToKeep < 0) {
            System.err.println("Number of records to keep must be a positive integer.");
            return;
        }
        printInput();
        printConfig();
        
        // Connect to database
        Database db = new Database(dbcfg);
        db.connect();
        
        // do someting with the database
        List<List<String>> results = db.query("SELECT * FROM usernotification;");
        System.out.println("All notifications:");
        printResults(results);
        
        if (!users.allUser) {
            results = db.query(String.format("SELECT * FROM usernotification WHERE user_id = '%s';", users.user));
            System.out.println("Notifications for user " + users.user + ":");
            printResults(results);
            
            // Sort of dry-run, show what will be deleted
            // Note that the subquery is used to get the last n records to keep, we can not sort that in the outer query
            // but the natural order of creation is also the senddate, so not a real problem here
            results = db.query(String.format("SELECT * FROM usernotification WHERE user_id = '%s' AND id NOT IN (SELECT id FROM usernotification WHERE user_id = '%s' ORDER BY senddate DESC LIMIT %d);", users.user, users.user, numberOfRecordsToKeep));
            System.out.println("Notifications for user " + users.user + " that will be deleted:");
            printResults(results);
            
            // Actually delete the notifications
            int rowCount = db.update(String.format("DELETE FROM usernotification WHERE user_id = '%s' AND id NOT IN (SELECT id FROM usernotification WHERE user_id = '%s' ORDER BY senddate DESC LIMIT %d);", users.user, users.user, numberOfRecordsToKeep));
            System.out.println("Deleted " + rowCount + " rows.");
        }
        
        db.close();
    }

    // could also have a function for storing results (columnNames and rows) in csv file ?

    private void printResults(List<List<String>> results) {
        // Note that the first row is actually the column names
        for (List<String> row : results) {
            for (String cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }

    // show commandline input
    private void printInput() {
        System.out.println("Truncating notifications...");
        System.out.println("Number of records to keep: " + numberOfRecordsToKeep);
        System.out.println("User: " + (users.allUser ? "all users" : users.user));
    }
    
    // show database config
    private void printConfig() {
        System.out.println("Database config - host: " + dbcfg.getHost());
        System.out.println("Database config - database: " + dbcfg.getDatabase());
        System.out.println("Database config - user: " + dbcfg.getUser());
        System.out.println("Database config - password: " + dbcfg.getPassword());
    }
}
