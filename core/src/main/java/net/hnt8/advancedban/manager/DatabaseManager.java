package net.hnt8.advancedban.manager;

import com.zaxxer.hikari.HikariDataSource;
import net.hnt8.advancedban.MethodInterface;
import net.hnt8.advancedban.Universal;
import net.hnt8.advancedban.utils.DynamicDataSource;
import net.hnt8.advancedban.utils.SQLQuery;
import net.hnt8.advancedban.utils.Punishment;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The Database Manager is used to interact directly with the database is use.<br>
 * Will automatically direct the requests to either MySQL or HSQLDB.
 * <br><br>
 * Looking to request {@link Punishment Punishments} from the Database?
 * Use {@link PunishmentManager#getPunishments(SQLQuery, Object...)} or
 * {@link PunishmentManager#getPunishmentFromResultSet(ResultSet)} for already parsed data.
 */
public class DatabaseManager {

    private HikariDataSource dataSource;
    private boolean useMySQL;

    private RowSetFactory factory;
    
    private static DatabaseManager instance = null;

    /**
     * Get the instance of the command manager
     *
     * @return the database manager instance
     */
    public static synchronized DatabaseManager get() {
        return instance == null ? instance = new DatabaseManager() : instance;
    }

    /**
     * Initially connects to the database and sets up the required tables of they don't already exist.
     *
     * @param useMySQLServer whether to preferably use MySQL (uses HSQLDB as fallback)
     */
    public void setup(boolean useMySQLServer) {
        MethodInterface mi = Universal.get().getMethods();
        File storageFile = new File(mi.getDataFolder(), ".storage");
        
        // Read previous storage type
        boolean previousUseMySQL = false;
        if (storageFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(storageFile.toPath())).trim();
                previousUseMySQL = "mysql".equalsIgnoreCase(content);
            } catch (IOException ex) {
                Universal.get().getLogger().warning("Failed to read storage type file: " + ex.getMessage());
            }
        }
        
        useMySQL = useMySQLServer;

        try {
            dataSource = new DynamicDataSource(useMySQL).generateDataSource();
        } catch (ClassNotFoundException ex) {
            Universal.get().getLogger().severe("ERROR: Failed to configure data source!");
            Universal.get().getLogger().severe("MySQL driver not found. Please ensure MySQL connector is available.");
            Universal.get().getLogger().fine(ex.getMessage());
            Universal.get().debugException(ex);
            return;
        } catch (Exception ex) {
            Universal.get().getLogger().severe("ERROR: Failed to configure data source!");
            Universal.get().getLogger().severe("Exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            Universal.get().debugException(ex);
            return;
        }

        // Test the connection before trying to use it
        try (Connection testConnection = dataSource.getConnection()) {
            if (!testConnection.isValid(3)) {
                Universal.get().getLogger().severe("ERROR: Database connection is not valid!");
                if (useMySQL) {
                    Universal.get().getLogger().severe("Please check your MySQL configuration in config.yml or MySQL.yml");
                    Universal.get().getLogger().severe("Verify: IP, Port, Database Name, Username, Password, and that MySQL server is running");
                }
                return;
            }
        } catch (SQLException ex) {
            Universal.get().getLogger().severe("ERROR: Failed to connect to database!");
            if (useMySQL) {
                Universal.get().getLogger().severe("MySQL Connection Error: " + ex.getMessage());
                Universal.get().getLogger().severe("Please check your MySQL configuration in config.yml or MySQL.yml");
                Universal.get().getLogger().severe("Verify: IP, Port, Database Name, Username, Password, and that MySQL server is running");
            } else {
                Universal.get().getLogger().severe("HSQLDB Connection Error: " + ex.getMessage());
            }
            Universal.get().debugSqlException(ex);
            return;
        }

        executeStatement(SQLQuery.CREATE_TABLE_PUNISHMENT);
        executeStatement(SQLQuery.CREATE_TABLE_PUNISHMENT_HISTORY);
        
        // Check if we're switching from HSQLDB to MySQL and migrate if needed
        if (useMySQL && !previousUseMySQL) {
            Universal.get().getLogger().info("Detected storage change from HSQLDB to MySQL. Checking if migration is needed...");
            migrateFromHSQLDBToMySQL(mi);
        }
        
        // Save current storage type
        try {
            if (!storageFile.exists()) {
                storageFile.createNewFile();
            }
            try (FileWriter writer = new FileWriter(storageFile)) {
                writer.write(useMySQL ? "mysql" : "hsqldb");
            }
        } catch (IOException ex) {
            Universal.get().getLogger().warning("Failed to save storage type file: " + ex.getMessage());
        }
    }

    /**
     * Migrates data from HSQLDB to MySQL when switching storage types.
     * Only runs if MySQL is empty and HSQLDB has data.
     *
     * @param mi the method interface
     */
    private void migrateFromHSQLDBToMySQL(MethodInterface mi) {
        try {
            // First, check if MySQL already has data
            boolean mysqlHasData = false;
            try (ResultSet rs = executeResultStatement(SQLQuery.SELECT_ALL_PUNISHMENTS)) {
                if (rs != null && rs.next()) {
                    mysqlHasData = true;
                }
            } catch (SQLException ex) {
                // Table might not exist yet, that's okay
            }
            
            if (mysqlHasData) {
                Universal.get().getLogger().info("MySQL database already contains data. Skipping migration.");
                return;
            }
            
            // Check if HSQLDB file exists
            File hsqldbFile = new File(mi.getDataFolder(), "data/storage.script");
            if (!hsqldbFile.exists()) {
                Universal.get().getLogger().info("No HSQLDB data found. Nothing to migrate.");
                return;
            }
            
            Universal.get().getLogger().info("Starting migration from HSQLDB to MySQL...");
            
            // Create temporary HSQLDB connection
            HikariDataSource hsqldbSource;
            try {
                DynamicDataSource hsqldbDataSource = new DynamicDataSource(false);
                hsqldbSource = hsqldbDataSource.generateDataSource();
            } catch (Exception ex) {
                Universal.get().getLogger().severe("Failed to connect to HSQLDB for migration: " + ex.getMessage());
                Universal.get().debugException(ex);
                return;
            }
            
            int migratedPunishments = 0;
            int migratedHistory = 0;
            
            try (Connection hsqldbConn = hsqldbSource.getConnection();
                 Connection mysqlConn = dataSource.getConnection()) {
                
                // Migrate active punishments
                try (PreparedStatement selectStmt = hsqldbConn.prepareStatement("SELECT * FROM Punishments");
                     ResultSet rs = selectStmt.executeQuery();
                     PreparedStatement insertStmt = mysqlConn.prepareStatement(
                         "INSERT INTO `Punishments` (`name`, `uuid`, `reason`, `operator`, `punishmentType`, `start`, `end`, `calculation`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    
                    while (rs.next()) {
                        insertStmt.setString(1, rs.getString("name"));
                        insertStmt.setString(2, rs.getString("uuid"));
                        insertStmt.setString(3, rs.getString("reason"));
                        insertStmt.setString(4, rs.getString("operator"));
                        insertStmt.setString(5, rs.getString("punishmentType"));
                        insertStmt.setLong(6, rs.getLong("start"));
                        insertStmt.setLong(7, rs.getLong("end"));
                        insertStmt.setString(8, rs.getString("calculation"));
                        insertStmt.executeUpdate();
                        migratedPunishments++;
                    }
                }
                
                // Migrate history
                try (PreparedStatement selectStmt = hsqldbConn.prepareStatement("SELECT * FROM PunishmentHistory");
                     ResultSet rs = selectStmt.executeQuery();
                     PreparedStatement insertStmt = mysqlConn.prepareStatement(
                         "INSERT INTO `PunishmentHistory` (`name`, `uuid`, `reason`, `operator`, `punishmentType`, `start`, `end`, `calculation`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    
                    while (rs.next()) {
                        insertStmt.setString(1, rs.getString("name"));
                        insertStmt.setString(2, rs.getString("uuid"));
                        insertStmt.setString(3, rs.getString("reason"));
                        insertStmt.setString(4, rs.getString("operator"));
                        insertStmt.setString(5, rs.getString("punishmentType"));
                        insertStmt.setLong(6, rs.getLong("start"));
                        insertStmt.setLong(7, rs.getLong("end"));
                        insertStmt.setString(8, rs.getString("calculation"));
                        insertStmt.executeUpdate();
                        migratedHistory++;
                    }
                }
                
            } catch (SQLException ex) {
                Universal.get().getLogger().severe("Error during migration: " + ex.getMessage());
                Universal.get().debugSqlException(ex);
                return;
            } finally {
                hsqldbSource.close();
            }
            
            Universal.get().getLogger().info("Migration completed successfully!");
            Universal.get().getLogger().info("Migrated " + migratedPunishments + " active punishments and " + migratedHistory + " history entries.");
            
        } catch (Exception ex) {
            Universal.get().getLogger().severe("Failed to migrate data from HSQLDB to MySQL: " + ex.getMessage());
            Universal.get().debugException(ex);
        }
    }

    /**
     * Shuts down the HSQLDB if used.
     */
    public void shutdown() {
        if (!useMySQL) {
            try(Connection connection = dataSource.getConnection(); final PreparedStatement statement = connection.prepareStatement("SHUTDOWN")){
                statement.execute();
            }catch (SQLException | NullPointerException exc){
                Universal.get().getLogger().warning("An unexpected error has occurred turning off the database");
                Universal.get().debugException(exc);
            }
        }

        dataSource.close();
    }
    
    private CachedRowSet createCachedRowSet() throws SQLException {
    	if (factory == null) {
    		factory = RowSetProvider.newFactory();
    	}
    	return factory.createCachedRowSet();
    }

    /**
     * Execute a sql statement without any results.
     *
     * @param sql        the sql statement
     * @param parameters the parameters
     */
    public void executeStatement(SQLQuery sql, Object... parameters) {
        executeStatement(sql, false, parameters);
    }

    /**
     * Execute a sql statement.
     *
     * @param sql        the sql statement
     * @param parameters the parameters
     * @return the result set
     */
    public ResultSet executeResultStatement(SQLQuery sql, Object... parameters) {
        return executeStatement(sql, true, parameters);
    }

    private ResultSet executeStatement(SQLQuery sql, boolean result, Object... parameters) {
        return executeStatement(sql.toString(), result, parameters);
    }

    private synchronized ResultSet executeStatement(String sql, boolean result, Object... parameters) {
    	if (dataSource == null) {
    		Universal.get().getLogger().severe("ERROR: DataSource is null! Database was not initialized properly.");
    		return null;
    	}
    	
    	try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

    		for (int i = 0; i < parameters.length; i++) {
    			statement.setObject(i + 1, parameters[i]);
    		}

    		if (result) {
    			CachedRowSet results = createCachedRowSet();
    			results.populate(statement.executeQuery());
    			return results;
    		}
   			statement.execute();
    	} catch (SQLException ex) {
    		Universal.get().getLogger().severe(
   					"An unexpected error has occurred executing a statement in the database\n"
   							+ "SQL Error: " + ex.getMessage() + "\n"
   							+ "SQL State: " + ex.getSQLState() + "\n"
   							+ "Error Code: " + ex.getErrorCode()
    				);
    		if (useMySQL) {
    			Universal.get().getLogger().severe("Please verify your MySQL connection settings in config.yml or MySQL.yml");
    		}
    		Universal.get().getLogger().fine("Query: \n" + sql);
    		Universal.get().debugSqlException(ex);
       	} catch (NullPointerException ex) {
            Universal.get().getLogger().severe(
                    "An unexpected error has occurred connecting to the database\n"
                            + "The database connection pool may not be initialized properly.\n"
                            + "Check if your MySQL data is correct and if your MySQL-Server is online"
            );
            if (useMySQL) {
            	Universal.get().getLogger().severe("Please verify your MySQL connection settings in config.yml or MySQL.yml");
            }
            Universal.get().debugException(ex);
        }
        return null;
    }

    /**
     * Check whether there is a valid connection to the database.
     *
     * @return whether there is a valid connection
     */
    public boolean isConnectionValid() {
        return dataSource.isRunning();
    }

    /**
     * Check whether MySQL is actually used.
     *
     * @return whether MySQL is used
     */
    public boolean isUseMySQL() {
        return useMySQL;
    }
}