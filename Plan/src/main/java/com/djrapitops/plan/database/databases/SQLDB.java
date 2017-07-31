package main.java.com.djrapitops.plan.database.databases;

import com.djrapitops.plugin.task.AbsRunnable;
import main.java.com.djrapitops.plan.Log;
import main.java.com.djrapitops.plan.Plan;
import main.java.com.djrapitops.plan.data.KillData;
import main.java.com.djrapitops.plan.data.SessionData;
import main.java.com.djrapitops.plan.data.UserData;
import main.java.com.djrapitops.plan.data.cache.DBCallableProcessor;
import main.java.com.djrapitops.plan.data.time.WorldTimes;
import main.java.com.djrapitops.plan.database.Database;
import main.java.com.djrapitops.plan.database.tables.*;
import main.java.com.djrapitops.plan.utilities.Benchmark;
import main.java.com.djrapitops.plan.utilities.FormatUtils;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class containing main logic for different data related save & load functionality.
 *
 * @author Rsl1122
 * @since 2.0.0
 */
public abstract class SQLDB extends Database {

    private final boolean supportsModification;

    private Connection connection;

    /**
     * @param plugin
     * @param supportsModification
     */
    public SQLDB(Plan plugin, boolean supportsModification) {
        super(plugin);
        this.supportsModification = supportsModification;
        boolean usingMySQL = getName().equals("MySQL");

        usersTable = new UsersTable(this, usingMySQL);
        gmTimesTable = new GMTimesTable(this, usingMySQL);
        sessionsTable = new SessionsTable(this, usingMySQL);
        killsTable = new KillsTable(this, usingMySQL);
        locationsTable = new LocationsTable(this, usingMySQL);
        ipsTable = new IPsTable(this, usingMySQL);
        nicknamesTable = new NicknamesTable(this, usingMySQL);
        commandUseTable = new CommandUseTable(this, usingMySQL);
        versionTable = new VersionTable(this, usingMySQL);
        tpsTable = new TPSTable(this, usingMySQL);
        securityTable = new SecurityTable(this, usingMySQL);
        worldTable = new WorldTable(this, usingMySQL);
        worldTimesTable = new WorldTimesTable(this, usingMySQL);

        startConnectionPingTask();
    }

    /**
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    public void startConnectionPingTask() throws IllegalArgumentException, IllegalStateException {
        // Maintains Connection.
        plugin.getRunnableFactory().createNew(new AbsRunnable("DBConnectionPingTask " + getName()) {
            @Override
            public void run() {
                Statement statement = null;
                try {
                    if (connection != null && !connection.isClosed()) {
                        statement = connection.createStatement();
                        statement.execute("/* ping */ SELECT 1");
                    }
                } catch (SQLException e) {
                    connection = getNewConnection();
                } finally {
                    if (statement != null) {
                        try {
                            statement.close();
                        } catch (SQLException e) {
                            Log.error("Error at closing statement");
                        }
                    }
                }
            }
        }).runTaskTimerAsynchronously(60 * 20, 60 * 20);
    }

    /**
     * @return
     */
    @Override
    public boolean init() {
        super.init();
        setStatus("Init");
        Benchmark.start("Database: Init " + getConfigName());
        try {
            if (!checkConnection()) {
                return false;
            }
            convertBukkitDataToDB();
            clean();
            return true;
        } catch (SQLException e) {
            Log.toLog(this.getClass().getName(), e);
            return false;
        } finally {
            Benchmark.stop("Database: Init " + getConfigName());
        }
    }

    /**
     * @return @throws SQLException
     */
    public boolean checkConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = getNewConnection();

            if (connection == null || connection.isClosed()) {
                return false;
            }

            boolean newDatabase = true;
            try {
                getVersion();
                newDatabase = false;
            } catch (Exception ignored) {

            }
            if (!versionTable.createTable()) {
                Log.error("Failed to create table: " + versionTable.getTableName());
                return false;
            }

            if (newDatabase) {
                Log.info("New Database created.");
                setVersion(8);
            }

            Benchmark.start("Database: Create tables");

            for (Table table : getAllTables()) {
                if (!table.createTable()) {
                    Log.error("Failed to create table: " + table.getTableName());
                    return false;
                }
            }

            if (!securityTable.createTable()) {
                Log.error("Failed to create table: " + securityTable.getTableName());
                return false;
            }

            Benchmark.stop("Database: Create tables");

            if (!newDatabase && getVersion() < 8) {
                setVersion(8);
            }
        }
        return true;
    }

    /**
     *
     */
    public void convertBukkitDataToDB() {
        plugin.getRunnableFactory().createNew(new AbsRunnable("BukkitDataConversionTask") {
            @Override
            public void run() {
                try {
                    Benchmark.start("Database: Convert Bukkitdata to DB data");
                    Set<UUID> uuids = usersTable.getSavedUUIDs();
                    uuids.removeAll(usersTable.getContainsBukkitData(uuids));
                    if (uuids.isEmpty()) {
                        Log.debug("No conversion necessary.");
                        return;
                    }
                    setStatus("Bukkit Data Conversion");
                    Log.info("Beginning Bukkit Data -> DB Conversion for " + uuids.size() + " players");
                    int id = plugin.getBootAnalysisTaskID();
                    if (id != -1) {
                        Log.info("Analysis | Cancelled Boot Analysis Due to conversion.");
                        plugin.getServer().getScheduler().cancelTask(id);
                    }
                    saveMultipleUserData(getUserDataForUUIDS(uuids));
                    Log.info("Conversion complete, took: " + FormatUtils.formatTimeAmount(Benchmark.stop("Database: Convert Bukkitdata to DB data")) + " ms");
                } catch (SQLException ex) {
                    Log.toLog(this.getClass().getName(), ex);
                } finally {
                    setAvailable();
                    this.cancel();
                }
            }
        }).runTaskAsynchronously();
    }

    /**
     * @return
     */
    public Table[] getAllTables() {
        return new Table[]{
                usersTable, gmTimesTable, ipsTable,
                nicknamesTable, sessionsTable, killsTable,
                commandUseTable, tpsTable, worldTable,
                worldTimesTable};
    }

    /**
     * @return
     */
    public Table[] getAllTablesInRemoveOrder() {
        return new Table[]{
                locationsTable, gmTimesTable, ipsTable,
                nicknamesTable, sessionsTable, killsTable,
                worldTimesTable, worldTable, usersTable,
                commandUseTable, tpsTable};
    }

    /**
     * @return
     */
    public abstract Connection getNewConnection();

    /**
     * @throws SQLException
     */
    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
        setStatus("Closed");
    }

    /**
     * @return @throws SQLException
     */
    @Override
    public int getVersion() throws SQLException {
        return versionTable.getVersion();
    }

    /**
     * @param version
     * @throws SQLException
     */
    @Override
    public void setVersion(int version) throws SQLException {
        versionTable.setVersion(version);
    }

    /**
     * @param uuid
     * @return
     */
    @Override
    public boolean wasSeenBefore(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        setStatus("User exist check");
        try {
            return usersTable.getUserId(uuid.toString()) != -1;
        } catch (SQLException e) {
            Log.toLog(this.getClass().getName(), e);
            return false;
        } finally {
            setAvailable();
        }
    }

    /**
     * @param uuid
     * @return
     * @throws SQLException
     */
    @Override
    public boolean removeAccount(String uuid) throws SQLException {
        if (uuid == null || uuid.isEmpty()) {
            return false;
        }
        try {
            setStatus("Remove account " + uuid);
            Benchmark.start("Database: Remove Account");
            Log.debug("Removing Account: " + uuid);
            try {
                checkConnection();
            } catch (Exception e) {
                Log.toLog(this.getClass().getName(), e);
                return false;
            }
            int userId = usersTable.getUserId(uuid);
            return userId != -1
                    && locationsTable.removeUserLocations(userId)
                    && ipsTable.removeUserIps(userId)
                    && nicknamesTable.removeUserNicknames(userId)
                    && gmTimesTable.removeUserGMTimes(userId)
                    && sessionsTable.removeUserSessions(userId)
                    && killsTable.removeUserKillsAndVictims(userId)
                    && worldTimesTable.removeUserWorldTimes(userId)
                    && usersTable.removeUser(uuid);
        } finally {
            Benchmark.stop("Database: Remove Account");
            setAvailable();
        }
    }

    /**
     * @param uuid
     * @param processors
     * @throws SQLException
     */
    @Override
    public void giveUserDataToProcessors(UUID uuid, Collection<DBCallableProcessor> processors) throws SQLException {
        Benchmark.start("Database: Give userdata to processors");
        try {
            checkConnection();
        } catch (Exception e) {
            Log.toLog(this.getClass().getName(), e);
            return;
        }
        // Check if user is in the database
        if (!wasSeenBefore(uuid)) {
            return;
        }
        setStatus("Get single userdata for " + uuid);
        // Get the data
        UserData data = usersTable.getUserData(uuid);

        int userId = usersTable.getUserId(uuid);

        List<String> nicknames = nicknamesTable.getNicknames(userId);
        data.addNicknames(nicknames);
        if (!nicknames.isEmpty()) {
            data.setLastNick(nicknames.get(nicknames.size() - 1));
        }

        List<InetAddress> ips = ipsTable.getIPAddresses(userId);
        data.addIpAddresses(ips);

        Map<String, Long> gmTimes = gmTimesTable.getGMTimes(userId);
        data.getGmTimes().setTimes(gmTimes);
        Map<String, Long> worldTimes = worldTimesTable.getWorldTimes(userId);
        data.getWorldTimes().setTimes(worldTimes);

        List<SessionData> sessions = sessionsTable.getSessionData(userId);
        data.addSessions(sessions);
        data.setPlayerKills(killsTable.getPlayerKills(userId));
        processors.forEach(processor -> processor.process(data));
        Benchmark.stop("Database: Give userdata to processors");
        setAvailable();
    }

    /**
     * @param uuidsCol
     * @return
     * @throws SQLException
     */
    @Override
    public List<UserData> getUserDataForUUIDS(Collection<UUID> uuidsCol) throws SQLException {
        if (uuidsCol == null || uuidsCol.isEmpty()) {
            return new ArrayList<>();
        }
        setStatus("Get userdata (multiple) for: " + uuidsCol.size());
        Benchmark.start("Database: Get UserData for " + uuidsCol.size());
        Map<UUID, Integer> userIds = usersTable.getAllUserIds();
        Set<UUID> remove = uuidsCol.stream()
                .filter(uuid -> !userIds.containsKey(uuid))
                .collect(Collectors.toSet());
        List<UUID> uuids = new ArrayList<>(uuidsCol);
        Log.debug("Data not found for: " + remove.size());
        uuids.removeAll(remove);
        Benchmark.start("Database: Create UserData objects for " + userIds.size());
        List<UserData> data = usersTable.getUserData(new ArrayList<>(uuids));
        Benchmark.stop("Database: Create UserData objects for " + userIds.size());
        if (data.isEmpty()) {
            return data;
        }
        Map<Integer, UUID> idUuidRel = userIds.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        List<Integer> ids = userIds.entrySet().stream().filter(e -> uuids.contains(e.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
        Log.debug("Ids: " + ids.size());
        Map<Integer, List<String>> nicknames = nicknamesTable.getNicknames(ids);
        Map<Integer, Set<InetAddress>> ipList = ipsTable.getIPList(ids);
        Map<Integer, List<KillData>> playerKills = killsTable.getPlayerKills(ids, idUuidRel);
        Map<Integer, List<SessionData>> sessionData = sessionsTable.getSessionData(ids);
        Map<Integer, Map<String, Long>> gmTimes = gmTimesTable.getGMTimes(ids);
        Map<Integer, Map<String, Long>> worldTimes = worldTimesTable.getWorldTimes(ids);
        Log.debug("Sizes: UUID:" + uuids.size() + " DATA:" + data.size() + " ID:" + userIds.size() + " N:" + nicknames.size() + " I:" + ipList.size() + " K:" + playerKills.size() + " S:" + sessionData.size());

        for (UserData uData : data) {
            UUID uuid = uData.getUuid();
            Integer id = userIds.get(uuid);
            uData.addIpAddresses(ipList.get(id));
            List<String> nickNames = nicknames.get(id);
            uData.addNicknames(nickNames);
            uData.setLastNick(nickNames.get(nickNames.size()-1));
            uData.addSessions(sessionData.get(id));
            uData.setPlayerKills(playerKills.get(id));
            uData.getGmTimes().setTimes(gmTimes.get(id));
            uData.getWorldTimes().setTimes(worldTimes.get(id));
        }

        Benchmark.stop("Database: Get UserData for " + uuidsCol.size());
        setAvailable();
        return data;
    }

    /**
     * @param data
     * @throws SQLException
     */
    @Override
    public void saveMultipleUserData(Collection<UserData> data) throws SQLException {
        Benchmark.start("Database: Save multiple Userdata");
        checkConnection();
        if (data.isEmpty()) {
            return;
        }
        setStatus("Save userdata (multiple) for " + data.size());
        usersTable.saveUserDataInformationBatch(data);

        // Transform to map
        Map<UUID, UserData> userDatas = data.stream().collect(Collectors.toMap(UserData::getUuid, Function.identity()));
        // Get UserIDs
        Map<UUID, Integer> userIds = usersTable.getAllUserIds();
        // Empty dataset
        Map<Integer, Set<String>> nicknames = new HashMap<>();
        Map<Integer, String> lastNicks = new HashMap<>();
        Map<Integer, Set<InetAddress>> ips = new HashMap<>();
        Map<Integer, List<KillData>> kills = new HashMap<>();
        Map<Integer, UUID> uuids = userIds.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Map<Integer, List<SessionData>> sessions = new HashMap<>();
        Map<Integer, Map<String, Long>> gmTimes = new HashMap<>();
        Map<Integer, Map<String, Long>> worldTimes = new HashMap<>();
        // Put to dataset
        List<String> worldNames = data.stream()
                .map(UserData::getWorldTimes)
                .map(WorldTimes::getTimes)
                .map(Map::keySet)
                .flatMap(keySet -> keySet.stream())
                .distinct()
                .collect(Collectors.toList());
        for (Map.Entry<UUID, UserData> entrySet : userDatas.entrySet()) {
            UUID uuid = entrySet.getKey();
            UserData uData = entrySet.getValue();
            Integer id = userIds.get(uuid);

            if (id == -1) {
                Log.debug("User not seen before, saving last: " + uuid);
                continue;
            }

            uData.access();
            nicknames.put(id, new HashSet<>(uData.getNicknames()));
            lastNicks.put(id, uData.getLastNick());
            ips.put(id, new HashSet<>(uData.getIps()));
            kills.put(id, new ArrayList<>(uData.getPlayerKills()));
            sessions.put(id, new ArrayList<>(uData.getSessions()));
            gmTimes.put(id, new HashMap<>(uData.getGmTimes().getTimes()));
            worldTimes.put(id, new HashMap<>(uData.getWorldTimes().getTimes()));
        }
        // Save
        nicknamesTable.saveNickLists(nicknames, lastNicks);
        ipsTable.saveIPList(ips);
        killsTable.savePlayerKills(kills, uuids);
        sessionsTable.saveSessionData(sessions);
        gmTimesTable.saveGMTimes(gmTimes);
        worldTable.saveWorlds(worldNames);
        worldTimesTable.saveWorldTimes(worldTimes);
        userDatas.values().stream()
                .filter(Objects::nonNull)
                .filter(UserData::isAccessed)
                .forEach(UserData::stopAccessing);
        Benchmark.stop("Database: Save multiple Userdata");
        setAvailable();
    }

    /**
     * @param data
     * @throws SQLException
     */
    @Override
    public void saveUserData(UserData data) throws SQLException {
        if (data == null) {
            return;
        }
        UUID uuid = data.getUuid();
        if (uuid == null) {
            return;
        }
        setStatus("Save userdata: " + uuid);
        checkConnection();
        Log.debug("DB_Save: " + data);
        data.access();
        usersTable.saveUserDataInformation(data);
        int userId = usersTable.getUserId(uuid.toString());
        sessionsTable.saveSessionData(userId, new ArrayList<>(data.getSessions()));
        nicknamesTable.saveNickList(userId, new HashSet<>(data.getNicknames()), data.getLastNick());
        ipsTable.saveIPList(userId, new HashSet<>(data.getIps()));
        killsTable.savePlayerKills(userId, new ArrayList<>(data.getPlayerKills()));
        gmTimesTable.saveGMTimes(userId, data.getGmTimes().getTimes());
        worldTable.saveWorlds(new HashSet<>(data.getWorldTimes().getTimes().keySet()));
        worldTimesTable.saveWorldTimes(userId, data.getWorldTimes().getTimes());
        data.stopAccessing();
        setAvailable();
    }

    /**
     *
     */
    @Override
    public void clean() {
        Log.info("Cleaning the database.");
        try {
            checkConnection();
            tpsTable.clean();
            locationsTable.removeAllData();
            Log.info("Clean complete.");
        } catch (SQLException e) {
            Log.toLog(this.getClass().getName(), e);
        }
    }

    /**
     * @return
     */
    @Override
    public boolean removeAllData() {
        setStatus("Clearing all data");
        for (Table table : getAllTablesInRemoveOrder()) {
            if (!table.removeAllData()) {
                return false;
            }
        }
        setAvailable();
        return true;
    }

    /**
     * @return
     */
    public boolean supportsModification() {
        return supportsModification;
    }

    /**
     * @return
     */
    public Connection getConnection() {
        return connection;
    }

    private void setStatus(String status) {
        plugin.processStatus().setStatus("DB-" + getName(), status);
    }

    private void setAvailable() {
        setStatus("Running");
    }
}
