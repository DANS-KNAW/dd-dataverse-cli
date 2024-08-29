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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvcli.action.Database;
import nl.knaw.dans.dvcli.config.DdDataverseDatabaseConfig;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "truncate-notifications",
        mixinStandardHelpOptions = true,
        description = "Remove user notifications but keep up to a specified amount.",
        sortOptions = false)
@Slf4j
public class NotificationTruncate extends AbstractCmd {
    DdDataverseDatabaseConfig dbcfg;
    public NotificationTruncate(DdDataverseDatabaseConfig dbcfg) {
        this.dbcfg = dbcfg;
    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    UserOptions users;

    static class UserOptions {
        @CommandLine.Option(names = { "--user" }, required = true, 
                description = "The user whose notifications to truncate.")
        private int user; // a number, preventing accidental SQL injection
        // Or maybe this should have been the user name (as in the GUI) 
        // and not the database id ?
        
        @CommandLine.Option(names = { "--all-users" }, required = true, 
                description = "Truncate notifications for all users.")
        private boolean allUsers;
    }
    
    @CommandLine.Parameters(index = "0", paramLabel = "number-of-records-to-keep", 
            description = "The number of notifications to keep.")
    private int numberOfRecordsToKeep;
    
    @Override
    public void doCall() {
        log.debug("Truncating notifications");
        if (numberOfRecordsToKeep < 0) {
            System.err.println("Number of records to keep must be a positive integer.");
            return; // or should we throw an exception or exit(2) ?
        }
        
        // Connect to database
        Database db = new Database(dbcfg);
        db.connect();
        
        List<List<String>> results = db.query("SELECT * FROM usernotification;", true );
        System.out.println("All notifications:");
        printResults(results);
        
        if (users.allUsers) {
            getUserIds(db).forEach(user_id -> truncateNotifications(db, user_id, numberOfRecordsToKeep));
        } else {
            truncateNotifications(db, users.user, numberOfRecordsToKeep);
        }
        
        db.close();
    }

    // get the user_id for all users that need truncation
    private List<Integer> getUserIds(Database db) {
        List<Integer> users = new ArrayList<Integer>();
        // Could just get all users with notifications
        // String sql = "SELECT DISTINCT user_id FROM usernotification;";
        // Instead we want only users with to many notifications
        String sql = String.format("SELECT user_id FROM usernotification GROUP BY user_id HAVING COUNT(user_id) > %d;", numberOfRecordsToKeep);
        List<List<String>> results = db.query(sql);
        for (List<String> row : results) {
            users.add(Integer.parseInt(row.get(0)));
        }
        System.out.println("Number of users found: " + users.size());
        return users;
    }
    
    private void truncateNotifications(Database db, int user, int numberOfRecordsToKeep) {
        //List<List<String>> results = db.query(String.format("SELECT * FROM usernotification WHERE user_id = '%d';", user), true);
        //System.out.println("Notifications for user " + user + ":");
        //printResults(results);

        // Sort of dry-run, show what will be deleted
        // Note that the subquery is used to get the last n records to keep, we can not sort that in the outer query
        // but the natural order of creation is also the senddate, so not a real problem here
        List<List<String>> results = db.query(String.format("SELECT * FROM usernotification WHERE user_id = '%d' AND id NOT IN (SELECT id FROM usernotification WHERE user_id = '%d' ORDER BY senddate DESC LIMIT %d);", user, user, numberOfRecordsToKeep), true);
        System.out.println("Notifications for user " + user + " that will be deleted:");
        printResults(results);

        // Actually delete the notifications
        int rowCount = db.update(String.format("DELETE FROM usernotification WHERE user_id = '%d' AND id NOT IN (SELECT id FROM usernotification WHERE user_id = '%d' ORDER BY senddate DESC LIMIT %d);", user, user, numberOfRecordsToKeep));
        System.out.println("Deleted " + rowCount + " rows.");
    }
    
    // could also have a function for storing results (columnNames and rows) in csv file ?
    
    private void printResults(List<List<String>> results) {
        // Note that the first row could be the column names
        for (List<String> row : results) {
            for (String cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }
    
}
