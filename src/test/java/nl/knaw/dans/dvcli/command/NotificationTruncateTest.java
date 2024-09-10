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

import nl.knaw.dans.dvcli.AbstractCapturingTest;
import nl.knaw.dans.dvcli.action.Database;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


public class NotificationTruncateTest extends AbstractCapturingTest {

    @Test
    public void doCall_with_wrong_database_connection_fails() throws Exception {

        var database = Mockito.mock(Database.class);
        doThrow(new SQLException("test database fails to connect")).when(database).connect();
        
        var userOptions = new NotificationTruncate.UserOptions();
        userOptions.user = 13;
        userOptions.allUsers = false;
        
        var cmd = getCmd(database, 1, userOptions);
        
        assertThatThrownBy(cmd::doCall)
               .isInstanceOf(SQLException.class)
               .hasMessage("test database fails to connect");
    }

    @Test
    public void doCall_with_negative_numberOfRecordsToKeep_fails() throws Exception {
        var database = Mockito.mock(Database.class);
 
        var userOptions = new NotificationTruncate.UserOptions();
        userOptions.user = 13;
        userOptions.allUsers = false;

        var cmd = getCmd(database, -1, userOptions);

        assertThatThrownBy(cmd::doCall)
                .isInstanceOf(Exception.class)
                .hasMessage("Number of records to keep must be a positive integer, now it was -1.");
    }
    
    @Test
    public void doCall_with_several_users_to_truncate_notifications_works() throws Exception {
        var database = Mockito.mock(Database.class);
        
        Mockito.doNothing().when(database).connect();
        Mockito.doNothing().when(database).close();
        
        var fakeQueryOutput = List.of(
                List.of("1", "user1-dontcare"),
                List.of("2", "user2-dontcare"),
                List.of("3", "user3-dontcare")
        );
        Mockito.when(database.query(anyString())).thenReturn( fakeQueryOutput );
        Mockito.when(database.update(anyString())).thenReturn( 3,2,1);
        
        var userOptions = new NotificationTruncate.UserOptions();
        userOptions.user = 0;
        userOptions.allUsers = true;
        
        var cmd = getCmd(database, 1, userOptions);
        cmd.doCall();
        
        assertThat(stderr.toString()).isEqualTo("1: OK. 2: OK. 3: OK. ");
        assertThat(stdout.toString()).isEqualTo("""
            INFO  Starting batch processing
            INFO  Processing item 1 of 3
            INFO  Deleting notifications for user with id 1
            Deleted 3 record(s) for user with id 1
            DEBUG Sleeping for 10 ms
            INFO  Processing item 2 of 3
            INFO  Deleting notifications for user with id 2
            Deleted 2 record(s) for user with id 2
            DEBUG Sleeping for 10 ms
            INFO  Processing item 3 of 3
            INFO  Deleting notifications for user with id 3
            Deleted 1 record(s) for user with id 3
            INFO  Finished batch processing of 3 items
            """);
        
        verify(database, times(1)).connect();
        verify(database, times(1)).query(any());
        verify(database, times(3)).update(any());
        verify(database, times(1)).close();
        verifyNoMoreInteractions(database);
    }
    
    private static NotificationTruncate getCmd(Database database, int numberOfRecordsToKeep, NotificationTruncate.UserOptions userOptions ) throws NoSuchFieldException, IllegalAccessException {
        var cmd = new NotificationTruncate(database);

        // set private fields with reflection
        
        var numberOfRecordsToKeepField = NotificationTruncate.class.getDeclaredField("numberOfRecordsToKeep");
        numberOfRecordsToKeepField.setAccessible(true);
        numberOfRecordsToKeepField.set(cmd, numberOfRecordsToKeep);

        var usersField = NotificationTruncate.class.getDeclaredField("users");
        usersField.setAccessible(true);
        usersField.set(cmd, userOptions);
        return cmd;
    }
     
}
