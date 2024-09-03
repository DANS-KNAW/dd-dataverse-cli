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


import java.io.InputStream;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

public class NotificationTruncateTest extends AbstractCapturingTest {

    private final InputStream originalStdin = System.in;

    @Test
    public void failWrongConnection () throws Exception {
        System.setIn(originalStdin);

        var database = Mockito.mock(Database.class);
        //Mockito.when(database.connect()).thenThrow(new SQLException("test exception"));
        doThrow(new SQLException("test database fails to connect")).when(database).connect();
        
        var userOptions = new NotificationTruncate.UserOptions();
        userOptions.user = 13;
        userOptions.allUsers = false;
        
        var cmd = getCmd(database, 1, userOptions);
        
        assertThatThrownBy(cmd::doCall)
               .isInstanceOf(SQLException.class)
               .hasMessage("test database fails to connect");

        
        //System.out.println("stdout: " + stdout.toString());
        //System.out.println("stderr: " + stderr.toString());
        //
        // but nothing in the output
    }

    @Test
    public void testFailWrongNumberOfRecordsToKeep () throws Exception {
        System.setIn(originalStdin);

        var database = Mockito.mock(Database.class);
 
        var userOptions = new NotificationTruncate.UserOptions();
        userOptions.user = 13;
        userOptions.allUsers = false;

        var cmd = getCmd(database, -1, userOptions);

        assertThatThrownBy(cmd::doCall)
                .isInstanceOf(Exception.class)
                .hasMessage("Number of records to keep must be a positive integer, now it was -1.");
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
