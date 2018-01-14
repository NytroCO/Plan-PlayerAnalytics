package com.djrapitops.plan.system.database.databases;

import com.djrapitops.plan.api.exceptions.database.DBException;
import com.djrapitops.plan.api.exceptions.database.DBInitException;
import com.djrapitops.plan.system.database.DBSystem;
import com.djrapitops.plan.system.database.databases.operation.BackupOperations;
import com.djrapitops.plan.system.database.databases.operation.CheckOperations;
import com.djrapitops.plan.system.database.databases.operation.FetchOperations;
import com.djrapitops.plan.system.database.databases.operation.RemoveOperations;
import com.djrapitops.plan.utilities.NullCheck;

/**
 * Abstract class representing a Database.
 * <p>
 * All methods should be only called from an asynchronous thread, unless stated
 * otherwise.
 *
 * @author Rsl1122
 */
public abstract class Database {

    private boolean open;

    public static Database getActive() {
        Database database = DBSystem.getInstance().getActiveDatabase();
        NullCheck.check(database, new IllegalStateException("Database was not initialized."));
        return database;
    }

    public abstract void init() throws DBInitException;

    public abstract BackupOperations backup();

    public abstract CheckOperations check();

    public abstract FetchOperations fetch();

    public abstract RemoveOperations remove();

    /**
     * Used to get the name of the database type.
     * <p>
     * Thread safe.
     *
     * @return SQLite/MySQL
     */
    public abstract String getName();

    /**
     * Used to get the config name of the database type.
     * <p>
     * Thread safe.
     *
     * @return sqlite/mysql
     */
    public String getConfigName() {
        return getName().toLowerCase().trim();
    }

    public abstract void close() throws DBException;

    public boolean isOpen() {
        return open;
    }
}