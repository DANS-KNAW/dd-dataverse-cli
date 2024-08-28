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

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
    String user = "dvnuser";
    String password = "dvnsecret";
    
    public void connect() {
        try {
            Class.forName("org.postgresql.Driver");
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
        } catch (Exception e) {
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
    
    public void query(String sql) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery( sql );
            
            // extract data from result set
            // get column names
            List<String> columnNames = new ArrayList<String>();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                columnNames.add(rs.getMetaData().getColumnName(i));
                System.out.print(rs.getMetaData().getColumnName(i) + " ");
            }
            System.out.println("");
            // get the rows
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<String>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.add(rs.getString(i));
                    System.out.print(rs.getString(i) + " ");
                }
                rows.add(row);
                System.out.println("");
            }
            
            // cleanup
            rs.close();
            stmt.close();

            // store results (columnNames and rows) in csv ?
        } catch (SQLException e) {
            System.err.println( "Database error: " + e.getClass().getName() + " " + e.getMessage() );
        }
    }
}
