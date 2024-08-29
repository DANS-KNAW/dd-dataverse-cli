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
import nl.knaw.dans.dvcli.action.BatchProcessor;
import nl.knaw.dans.dvcli.action.ConsoleReport;
import nl.knaw.dans.dvcli.action.Database;
import nl.knaw.dans.dvcli.action.Pair;
import nl.knaw.dans.dvcli.action.ThrowingFunction;
import nl.knaw.dans.dvcli.config.DdDataverseDatabaseConfig;

import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "truncate-notifications",
        mixinStandardHelpOptions = true,
        description = "Remove user notifications but keep up to a specified amount.",
        sortOptions = false)
@Slf4j
public class NotificationTruncate extends AbstractCmd {
    DdDataverseDatabaseConfig dbCfg;
    public NotificationTruncate(DdDataverseDatabaseConfig dbCfg) {
        this.dbCfg = dbCfg;
    }

    protected Database db;
    
    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    UserOptions users;

    static class UserOptions {
        @CommandLine.Option(names = { "--user" }, required = true, 
                description = "The user whose notifications to truncate.")
        private int user; // a number, preventing accidental SQL injection
        // This id is visible in the Dataverse Dashboard for 'superusers' 
        
        @CommandLine.Option(names = { "--all-users" }, required = true, 
                description = "Truncate notifications for all users.")
        private boolean allUsers;
    }
    
    @CommandLine.Parameters(index = "0", paramLabel = "number-of-records-to-keep", 
            description = "The number of notification records to keep.")
    private int numberOfRecordsToKeep;

//    @CommandLine.Option(names = { "-d", "--delay" }, description = "Delay in milliseconds between requests to the server (default: ${DEFAULT-VALUE}).", defaultValue = "" + DEFAULT_DELAY)
//    protected long delay;
    
    private record NotificationTruncateParams(Database db, int userId, int numberOfRecordsToKeep) {
    }

    protected <NotificationTruncateParams> BatchProcessor.BatchProcessorBuilder<NotificationTruncate.NotificationTruncateParams, String> paramsBatchProcessorBuilder() throws IOException {
        return BatchProcessor.<NotificationTruncate.NotificationTruncateParams, String> builder();
                //.delay(delay);
    }
    
    private static class NotificationTruncateAction implements ThrowingFunction<NotificationTruncate.NotificationTruncateParams, String, Exception> {

        @Override
        public String apply(NotificationTruncateParams notificationTruncateParams) throws Exception {
            // Dry-run for now, show what will be deleted
            //List<List<String>> results = notificationTruncateParams.db.query(String.format("SELECT * FROM usernotification WHERE user_id = '%d' AND id NOT IN (SELECT id FROM usernotification WHERE user_id = '%d' ORDER BY senddate DESC LIMIT %d);", 
            //        notificationTruncateParams.userId, notificationTruncateParams.userId, 
            //        notificationTruncateParams.numberOfRecordsToKeep), true
            //);
            //return "Notifications for user " + notificationTruncateParams.userId + " that will be deleted: \n" 
            //        + getResultsAsString(results);

            // Actually delete the notifications
            int rowCount = notificationTruncateParams.db.update(String.format("DELETE FROM usernotification WHERE user_id = '%d' AND id NOT IN (SELECT id FROM usernotification WHERE user_id = '%d' ORDER BY senddate DESC LIMIT %d);",
                    notificationTruncateParams.userId, notificationTruncateParams.userId,
                    notificationTruncateParams.numberOfRecordsToKeep));
            return "Deleted " + rowCount + " record(s) for user with id " + notificationTruncateParams.userId;
        }
    }
    
    @Override
    public void doCall() throws IOException {
        log.debug("Truncating notifications");
        
        // validate input
        if (numberOfRecordsToKeep < 0) {
            System.err.println("Number of records to keep must be a positive integer.");
            return; // or should we throw an exception or exit(2) ?
        }
        
        // Connect to database
        db = new Database(dbCfg);
        db.connect();

        paramsBatchProcessorBuilder()
                .labeledItems(getItems())
                .action(new NotificationTruncate.NotificationTruncateAction())
                .report(new ConsoleReport<>())
                .build()
                .process();

        db.close();
    }
    
    protected List<Pair<String, NotificationTruncateParams>> getItems() throws IOException {
        List<Pair<String, NotificationTruncateParams>> items = new ArrayList<>();

        if (users.allUsers) {
            getUserIds(db).forEach(user_id -> items.add(new Pair<>(Integer.toString(user_id), 
                    new NotificationTruncateParams(db, user_id, numberOfRecordsToKeep))));
        } else {
            // single user
            items.add(new Pair<>(Integer.toString(users.user), 
                    new NotificationTruncateParams(db, users.user, numberOfRecordsToKeep)));
        } 
        
        return items;
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
        System.out.println("Number of users found for notification truncation: " + users.size());
        return users;
    }
    
    private static String getResultsAsString(List<List<String>> results) {
        // Note that the first row could be the column names
        StringBuilder sb = new StringBuilder();
        for (List<String> row : results) {
            for (String cell : row) {
                sb.append(cell).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}