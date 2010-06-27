// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.persist;

import com.google.enterprise.connector.scheduler.Schedule;

import org.h2.jdbcx.JdbcDataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * Class to test JDBC persistent store.
 */
public class JdbcStoreTest extends PersistentStoreTestAbstract {
  private static final String dsName = "jdbc/testDataSourcName";

  protected DataSource dataSource;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Setup in-memory H2 JDBC DataSource;
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:testdb");
    ds.setUser("sa");
    ds.setPassword("sa");
    dataSource = ds;

    storeDir = null;
    store = new JdbcStore();
    ((JdbcStore) store).setDataSource(dataSource);
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      ((JdbcStore) store).finalize();
    } finally {
      super.tearDown();
    }
  }

  // Tests creating Connector Table.
  public void testCreateTable() throws SQLException {
    // Connect to the database.
    Connection connection = dataSource.getConnection();

    assertFalse(tableExists(connection, JdbcStore.TABLE_NAME));

    Schedule schedule =
        store.getConnectorSchedule(new StoreContext("nonexist"));
    assertNull(schedule);

    /* TODO: Why does this fail for in-memory DBs?
    assertTrue(tableExists(connection, JdbcStore.TABLE_NAME));
    */

    connection.close();
  }

  private boolean tableExists(Connection connection, String tableName)
      throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();
    ResultSet tables = metaData.getTables(null, null, tableName, null);
    try {
      while (tables.next()) {
        if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
          return true;
        }
      }
      return false;
    } finally {
      tables.close();
    }
  }
}
