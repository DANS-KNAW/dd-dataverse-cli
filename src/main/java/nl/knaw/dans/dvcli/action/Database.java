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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.dvcli.config.DdDataverseDatabaseConfig;

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
 * <p> 
 * Note that the sql input strings are not filtered in any way, 
 * so don't put user input in there!
 */
@Slf4j
public class Database {
    
    public Database(DdDataverseDatabaseConfig config) {
        this.host = config.getHost();
        this.database = config.getDatabase();
        this.user = config.getUser();
        this.password = config.getPassword();
    }
    
    Connection connection = null;

    String port = "5432"; // Fixed port for Postgres
    
    String host;
    String database;
    String user;
    String password;
    
    public void connect() throws ClassNotFoundException, SQLException {
            Class.forName("org.postgresql.Driver");
            if (connection == null) {
                log.debug("Starting connecting to database");
                connection = DriverManager
                        .getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database,
                                user,
                                password);
            }

    }
    
    public void close() {
        try {
            if (connection != null) {
                log.debug("Close connection to database");
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println( "Database error: " + e.getClass().getName() + " " + e.getMessage() );
        } finally {
            connection = null;
        }
    }

    public List<List<String>> query(String sql) throws SQLException {
        return query(sql, false);
    }
    
    public List<List<String>> query(String sql, Boolean startResultWithColumnNames) throws SQLException {
        log.debug("Querying database with: {}", sql);
        
        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            return extractResult(rs, startResultWithColumnNames);
        }
    }

    List<List<String>> extractResult(ResultSet rs, Boolean startResultWithColumnNames) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        // get column names
        int numColumns = rs.getMetaData().getColumnCount();
        if (startResultWithColumnNames) {
            List<String> columnNames = new ArrayList<String>();
            for (int i = 1; i <= numColumns; i++) {
                columnNames.add(rs.getMetaData().getColumnName(i));
            }
            // make it the first row, for simplicity, a bit like with a csv file
            rows.add(columnNames);
        }

        // get the data rows
        while (rs.next()) {
            List<String> row = new ArrayList<String>();
            for (int i = 1; i <= numColumns; i++) {
                row.add(rs.getString(i));
            }
            rows.add(row);
        }
        return rows;
    }

    public int update(String sql) throws SQLException {
        log.debug("Updating database with: {}", sql);

        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }
}
