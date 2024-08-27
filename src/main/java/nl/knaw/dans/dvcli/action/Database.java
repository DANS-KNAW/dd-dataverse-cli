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
package nl.knaw.dans.dvcli.action;

import nl.knaw.dans.dvcli.config.DdDataverseDatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides access to the Dataverse Database (Postgres).
 * Some actions are not supported by the Dataverse API (yet) 
 * and must be done by direct access to the database.
 */
public class Database {
    
    public Database(DdDataverseDatabaseConfig config) {
        this.host = config.getHost();
        this.database = config.getDatabase();
        this.user = config.getUser();
        this.password = config.getPassword();
    }
    
    Connection connection = null;

    String port = "5432"; // Fixed port for Postgres
    // TODO should come from config
    String host = "localhost";
    String database = "dvndb";
    String user = "dvnapp";
    String password = "secret";
    
    public void connect() {
        try {
            if (connection == null) {
                connection = DriverManager
                        .getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database,
                                user,
                                password);
            }
            if (connection != null) {
                System.out.println("Connected to the database!");
            } else {
                System.out.println("Failed to make connection!");
            }
        } catch (SQLException e) {
            System.err.println( "Database error: " + e.getClass().getName() + " " + e.getMessage() );
        }
    }
    
    public void close() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            System.err.println( "Database error: " + e.getClass().getName() + " " + e.getMessage() );
        }
    }
}
