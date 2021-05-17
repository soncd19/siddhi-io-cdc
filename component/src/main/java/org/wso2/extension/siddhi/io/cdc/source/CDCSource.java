/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.extension.siddhi.io.cdc.source;

import io.debezium.embedded.EmbeddedEngine;
import org.apache.log4j.Logger;
import org.wso2.extension.siddhi.io.cdc.source.listening.CDCSourceObjectKeeper;
import org.wso2.extension.siddhi.io.cdc.source.listening.ChangeDataCapture;
import org.wso2.extension.siddhi.io.cdc.source.listening.WrongConfigurationException;
import org.wso2.extension.siddhi.io.cdc.source.polling.CDCPoller;
import org.wso2.extension.siddhi.io.cdc.util.CDCSourceConstants;
import org.wso2.extension.siddhi.io.cdc.util.CDCSourceUtil;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.exception.ConnectionUnavailableException;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.stream.input.source.Source;
import org.wso2.siddhi.core.stream.input.source.SourceEventListener;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.core.util.transport.OptionHolder;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Extension to the siddhi to retrieve Database Changes - implementation of cdc source.
 **/
@Extension(
        name = "cdc",
        namespace = "source",
        description = "The CDC source receives events when change events (i.e., INSERT, UPDATE, DELETE) are triggered" +
                " for a database table. Events are received in the 'key-value' format." +
                "\nThe key values of the map of a CDC change event are as follows." +
                "\n\tFor insert: Keys are specified as columns of the table." +
                "\n\tFor delete: Keys are followed followed by the specified table columns. This is achieved via " +
                "'before_'. e.g., specifying 'before_X' results in the key being added before the column named 'X'." +
                "\n\tFor update: Keys are followed followed by the specified table columns. This is achieved via " +
                "'before_'. e.g., specifying 'before_X' results in the key being added before the column named 'X'." +
                "\nFor 'polling' mode: Keys are specified as the coloumns of the table." +
                "\nSee parameter: mode for supported databases and change events.",
        parameters = {
                @Parameter(name = "url",
                        description = "The connection URL to the database." +
                                "\nF=The format used is: " +
                                "'jdbc:mysql://<host>:<port>/<database_name>' ",
                        type = DataType.STRING
                ),
                @Parameter(
                        name = "mode",
                        description = "Mode to capture the change data. The type of events that can be received, " +
                                "and the required parameters differ based on the mode. The mode can be one of the " +
                                "following:\n" +
                                "'polling': This mode uses a column named 'polling.column' to monitor the given " +
                                "table. It captures change events of the 'RDBMS', 'INSERT, and 'UPDATE' types.\n" +
                                "'listening': This mode uses logs to monitor the given table. It currently supports" +
                                " change events only of the 'MySQL', 'INSERT', 'UPDATE', and 'DELETE' types.",
                        type = DataType.STRING,
                        defaultValue = "listening",
                        optional = true
                ),
                @Parameter(
                        name = "jdbc.driver.name",
                        description = "The driver class name for connecting the database." +
                                " **It is required to specify a value for this parameter when the mode is 'polling'.**",
                        type = DataType.STRING,
                        defaultValue = "<Empty_String>",
                        optional = true
                ),
                @Parameter(
                        name = "username",
                        description = "The username to be used for accessing the database. This user needs to have" +
                                " the 'SELECT', 'RELOAD', 'SHOW DATABASES', 'REPLICATION SLAVE', and " +
                                "'REPLICATION CLIENT'privileges for the change data capturing table (specified via" +
                                " the 'table.name' parameter)." +
                                "\nTo operate in the polling mode, the user needs 'SELECT' privileges.",
                        type = DataType.STRING
                ),
                @Parameter(
                        name = "password",
                        description = "The password of the username you specified for accessing the database.",
                        type = DataType.STRING
                ),
                @Parameter(name = "pool.properties",
                        description = "The pool parameters for the database connection can be specified as key-value" +
                                " pairs.",
                        type = DataType.STRING,
                        optional = true,
                        defaultValue = "<Empty_String>"
                ),
                @Parameter(
                        name = "datasource.name",
                        description = "Name of the wso2 datasource to connect to the database." +
                                " When datasource name is provided, the URL, username and password are not needed. " +
                                "A datasource based connection is given more priority over the URL based connection." +
                                "\n This parameter is applicable only when the mode is set to 'polling', and it can" +
                                " be applied only when you use this extension with WSO2 Stream Processor.",
                        type = DataType.STRING,
                        defaultValue = "<Empty_String>",
                        optional = true
                ),
                @Parameter(
                        name = "table.name",
                        description = "The name of the table that needs to be monitored for data changes.",
                        type = DataType.STRING
                ),
                @Parameter(
                        name = "polling.column",
                        description = "The column name  that is polled to capture the change data. " +
                                "It is recommended to have a TIMESTAMP field as the 'polling.column' in order to" +
                                " capture the inserts and updates." +
                                "\nNumeric auto-incremental fields and char fields can also be" +
                                " used as 'polling.column'. However, note that fields of these types only support" +
                                " insert change capturing, and the possibility of using a char field also depends on" +
                                " how the data is input." +
                                "\n**It is required to enter a value for this parameter when the mode is 'polling'.**"
                        ,
                        type = DataType.STRING,
                        defaultValue = "<Empty_String>",
                        optional = true
                ),
                @Parameter(
                        name = "polling.interval",
                        description = "The time interval (specified in seconds) to poll the given table for changes." +
                                "\nThis parameter is applicable only when the mode is set to 'polling'."
                        ,
                        type = DataType.INT,
                        defaultValue = "1",
                        optional = true
                ),
                @Parameter(
                        name = "operation",
                        description = "The change event operation you want to carry out. Possible values are" +
                                " 'insert', 'update' or 'delete'. It is required to specify a value when the mode is" +
                                " 'listening'." +
                                "\nThis parameter is not case sensitive.",
                        type = DataType.STRING
                ),
                @Parameter(
                        name = "connector.properties",
                        description = "Here, you can specify Debezium connector properties as a comma-separated " +
                                "string. " +
                                "\nThe properties specified here are given more priority over the parameters. This" +
                                " parameter is applicable only for the 'listening' mode.",
                        type = DataType.STRING,
                        optional = true,
                        defaultValue = "Empty_String"
                ),
                @Parameter(name = "database.server.id",
                        description = "An ID to be used when joining MySQL database cluster to read the bin log. " +
                                "This should be a unique integer between 1 to 2^32. This parameter is applicable " +
                                "only when the mode is 'listening'.",
                        type = DataType.STRING,
                        optional = true,
                        defaultValue = "-1"
                ),
                @Parameter(name = "database.server.name",
                        description = "A logical name that identifies and provides a namespace for the database " +
                                "server. This parameter is applicable only when the mode is 'listening'.",
                        defaultValue = "{host}_{port}",
                        optional = true,
                        type = DataType.STRING
                )
        },
        examples = {
                @Example(
                        syntax = "@source(type = 'cdc' , url = 'jdbc:mysql://localhost:3306/SimpleDB', " +
                                "\nusername = 'cdcuser', password = 'pswd4cdc', " +
                                "\ntable.name = 'students', operation = 'insert', " +
                                "\n@map(type='keyvalue', @attributes(id = 'id', name = 'name')))" +
                                "\ndefine stream inputStream (id string, name string);",
                        description = "In this example, the CDC source listens to the row insertions that are made " +
                                "in the 'students' table with the column name, and the ID. This table belongs to the " +
                                "'SimpleDB' MySQL database that can be accessed via the given URL."
                ),
                @Example(
                        syntax = "@source(type = 'cdc' , url = 'jdbc:mysql://localhost:3306/SimpleDB', " +
                                "\nusername = 'cdcuser', password = 'pswd4cdc', " +
                                "\ntable.name = 'students', operation = 'update', " +
                                "\n@map(type='keyvalue', @attributes(id = 'id', name = 'name', " +
                                "\nbefore_id = 'before_id', before_name = 'before_name')))" +
                                "\ndefine stream inputStream (before_id string, id string, " +
                                "\nbefore_name string , name string);",
                        description = "In this example, the CDC source listens to the row updates that are made in " +
                                "the 'students' table. This table belongs to the 'SimpleDB' MySQL database that can" +
                                " be accessed via the given URL."
                ),
                @Example(
                        syntax = "@source(type = 'cdc' , url = 'jdbc:mysql://localhost:3306/SimpleDB', " +
                                "\nusername = 'cdcuser', password = 'pswd4cdc', " +
                                "\ntable.name = 'students', operation = 'delete', " +
                                "\n@map(type='keyvalue', @attributes(before_id = 'before_id'," +
                                " before_name = 'before_name')))" +
                                "\ndefine stream inputStream (before_id string, before_name string);",
                        description = "In this example, the CDC source listens to the row deletions made in the " +
                                "'students' table. This table belongs to the 'SimpleDB' database that can be accessed" +
                                " via the given URL."
                ),
                @Example(
                        syntax = "@source(type = 'cdc', mode='polling', polling.column = 'id', " +
                                "\njdbc.driver.name = 'com.mysql.jdbc.Driver', " +
                                "url = 'jdbc:mysql://localhost:3306/SimpleDB', " +
                                "\nusername = 'cdcuser', password = 'pswd4cdc', " +
                                "\ntable.name = 'students', " +
                                "\n@map(type='keyvalue'), @attributes(id = 'id', name = 'name'))" +
                                "\ndefine stream inputStream (id int, name string);",
                        description = "In this example, the CDC source polls the 'students' table for inserts. 'id'" +
                                " that is specified as the polling colum' is an auto incremental field. The " +
                                "connection to the database is made via the URL, username, password, and the JDBC" +
                                " driver name."
                ),
                @Example(
                        syntax = "@source(type = 'cdc', mode='polling', polling.column = 'id', " +
                                "datasource.name = 'SimpleDB'," +
                                "\ntable.name = 'students', " +
                                "\n@map(type='keyvalue'), @attributes(id = 'id', name = 'name'))" +
                                "\ndefine stream inputStream (id int, name string);",
                        description = "In this example, the CDC source polls the 'students' table for inserts. The" +
                                " given polling column is a char column with the 'S001, S002, ... .' pattern." +
                                " The connection to the database is made via a data source named 'SimpleDB'. Note " +
                                "that the 'datasource.name' parameter works only with the Stream Processor."
                ),
                @Example(
                        syntax = "@source(type = 'cdc', mode='polling', polling.column = 'last_updated', " +
                                "datasource.name = 'SimpleDB'," +
                                "\ntable.name = 'students', " +
                                "\n@map(type='keyvalue'))" +
                                "\ndefine stream inputStream (name string);",
                        description = "In this example, the CDC source polls the 'students' table for inserts " +
                                "and updates. The polling column is a timestamp field."
                ),

        }
)

public class CDCSource extends Source {
    private static final Logger log = Logger.getLogger(CDCSource.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int pollingInterval;
    private String mode;
    private Map<byte[], byte[]> offsetData = new HashMap<>();
    private String operation;
    private ChangeDataCapture changeDataCapture;
    private String historyFileDirectory;
    private CDCSourceObjectKeeper cdcSourceObjectKeeper = CDCSourceObjectKeeper.getCdcSourceObjectKeeper();
    private String carbonHome;
    private CDCPoller cdcPoller;


    @Override
    public void init(SourceEventListener sourceEventListener, OptionHolder optionHolder,
                                       String[] requestedTransportPropertyNames, ConfigReader configReader,
                                       SiddhiAppContext siddhiAppContext) {
        //initialize mode
        mode = optionHolder.validateAndGetStaticValue(CDCSourceConstants.MODE, CDCSourceConstants.MODE_LISTENING);

        //initialize common mandatory parameters
        String tableName = optionHolder.validateAndGetOption(CDCSourceConstants.TABLE_NAME).getValue();

        switch (mode) {
            case CDCSourceConstants.MODE_LISTENING:

                String url = optionHolder.validateAndGetOption(CDCSourceConstants.DATABASE_CONNECTION_URL).getValue();
                String username = optionHolder.validateAndGetOption(CDCSourceConstants.USERNAME).getValue();
                String password = optionHolder.validateAndGetOption(CDCSourceConstants.PASSWORD).getValue();

                String siddhiAppName = siddhiAppContext.getName();
                String streamName = sourceEventListener.getStreamDefinition().getId();

                //initialize mandatory parameters
                operation = optionHolder.validateAndGetOption(CDCSourceConstants.OPERATION).getValue();

                //initialize optional parameters
                int serverID;
                serverID = Integer.parseInt(optionHolder.validateAndGetStaticValue(
                        CDCSourceConstants.DATABASE_SERVER_ID, Integer.toString(CDCSourceConstants.DEFAULT_SERVER_ID)));

                String serverName;
                serverName = optionHolder.validateAndGetStaticValue(CDCSourceConstants.DATABASE_SERVER_NAME,
                        CDCSourceConstants.EMPTY_STRING);

                //initialize parameters from connector.properties
                String connectorProperties = optionHolder.validateAndGetStaticValue(
                        CDCSourceConstants.CONNECTOR_PROPERTIES, CDCSourceConstants.EMPTY_STRING);

                //initialize history file directory
                carbonHome = CDCSourceUtil.getCarbonHome();
                historyFileDirectory = carbonHome + File.separator + "cdc" + File.separator + "history"
                        + File.separator + siddhiAppName + File.separator;

                validateListeningModeParameters(optionHolder);

                //send sourceEventListener and preferred operation to changeDataCapture object
                changeDataCapture = new ChangeDataCapture(operation, sourceEventListener);

                //create the folder for history file if not exists
                File directory = new File(historyFileDirectory);
                if (!directory.exists()) {
                    boolean isDirectoryCreated = directory.mkdirs();
                    if (isDirectoryCreated && log.isDebugEnabled()) {
                        log.debug("Directory created for history file.");
                    }
                }

                try {
                    Map<String, Object> configMap = CDCSourceUtil.getConfigMap(username, password, url, tableName,
                            historyFileDirectory, siddhiAppName, streamName, serverID, serverName, connectorProperties,
                            this.hashCode());
                    changeDataCapture.setConfig(configMap);
                } catch (WrongConfigurationException ex) {
                    throw new SiddhiAppCreationException("The cdc source couldn't get started because of invalid" +
                            " configurations. Found configurations: {username='" + username + "', password=******," +
                            " url='" + url + "', tablename='" + tableName + "'," +
                            " connetorProperties='" + connectorProperties + "'}", ex);
                }
                break;
            case CDCSourceConstants.MODE_POLLING:

                String pollingColumn = optionHolder.validateAndGetStaticValue(CDCSourceConstants.POLLING_COLUMN);
                boolean isDatasourceNameAvailable = optionHolder.isOptionExists(CDCSourceConstants.DATASOURCE_NAME);
                boolean isJndiResourceAvailable = optionHolder.isOptionExists(CDCSourceConstants.JNDI_RESOURCE);
                pollingInterval = Integer.parseInt(
                        optionHolder.validateAndGetStaticValue(CDCSourceConstants.POLLING_INTERVAL,
                                Integer.toString(CDCSourceConstants.DEFAULT_POLLING_INTERVAL_SECONDS)));
                validatePollingModeParameters();
                String poolPropertyString = optionHolder.validateAndGetStaticValue(CDCSourceConstants.POOL_PROPERTIES,
                        null);

                if (isDatasourceNameAvailable) {
                    String datasourceName = optionHolder.validateAndGetStaticValue(CDCSourceConstants.DATASOURCE_NAME);
                    cdcPoller = new CDCPoller(null, null, null, tableName, null,
                            datasourceName, null, pollingColumn, pollingInterval,
                            poolPropertyString, sourceEventListener, configReader);
                } else if (isJndiResourceAvailable) {
                    String jndiResource = optionHolder.validateAndGetStaticValue(CDCSourceConstants.JNDI_RESOURCE);
                    cdcPoller = new CDCPoller(null, null, null, tableName, null,
                            null, jndiResource, pollingColumn, pollingInterval, poolPropertyString,
                            sourceEventListener, configReader);
                } else {
                    String driverClassName;
                    try {
                        driverClassName = optionHolder.validateAndGetStaticValue(CDCSourceConstants.JDBC_DRIVER_NAME);
                        url = optionHolder.validateAndGetOption(CDCSourceConstants.DATABASE_CONNECTION_URL).getValue();
                        username = optionHolder.validateAndGetOption(CDCSourceConstants.USERNAME).getValue();
                        password = optionHolder.validateAndGetOption(CDCSourceConstants.PASSWORD).getValue();
                    } catch (SiddhiAppValidationException ex) {
                        throw new SiddhiAppValidationException(ex.getMessage() + " Alternatively, define "
                                + CDCSourceConstants.DATASOURCE_NAME + " or " + CDCSourceConstants.JNDI_RESOURCE +
                                ". Current mode: " + CDCSourceConstants.MODE_POLLING);
                    }
                    cdcPoller = new CDCPoller(url, username, password, tableName, driverClassName,
                            null, null, pollingColumn, pollingInterval, poolPropertyString,
                            sourceEventListener, configReader);
                }
                break;
            default:
                throw new SiddhiAppValidationException("Unsupported " + CDCSourceConstants.MODE + ": " + mode);
        }
    }

    @Override
    public Class[] getOutputEventClasses() {
        return new Class[]{Map.class};
    }


    @Override
    public void connect(ConnectionCallback connectionCallback)
            throws ConnectionUnavailableException {
        switch (mode) {
            case CDCSourceConstants.MODE_LISTENING:
                //keep the object reference in Object keeper
                cdcSourceObjectKeeper.addCdcObject(this);

                //create completion callback to handle the exceptions from debezium engine.
                EmbeddedEngine.CompletionCallback completionCallback = (success, message, error) -> {
                    if (!success) {
                        connectionCallback.onError(new ConnectionUnavailableException(
                                "Connection to the database lost.", error));
                    }
                };

                EmbeddedEngine engine = changeDataCapture.getEngine(completionCallback);
                executorService.execute(engine);
                break;
            case CDCSourceConstants.MODE_POLLING:
                //create a completion callback to handle exceptions from CDCPoller
                CDCPoller.CompletionCallback cdcCompletionCallback = (Throwable error) ->
                {
                    if (error.getClass().equals(SQLException.class)) {
                        connectionCallback.onError(new ConnectionUnavailableException(
                                "Connection to the database lost.", error));
                    } else {
                        destroy();
                        throw new SiddhiAppRuntimeException("CDC Polling mode run failed.", error);
                    }
                };

                cdcPoller.setCompletionCallback(cdcCompletionCallback);
                executorService.execute(cdcPoller);
                break;
            default:
                break; //Never get executed since mode is validated.
        }
    }

    @Override
    public void disconnect() {
        if (mode.equals(CDCSourceConstants.MODE_POLLING)) {
            cdcPoller.pause();
            if (cdcPoller.isLocalDataSource()) {
                cdcPoller.getDataSource().close();
                if (log.isDebugEnabled()) {
                    log.debug("Closing the pool for CDC polling mode.");
                }
            }
        }
    }

    @Override
    public void destroy() {
        this.disconnect();

        if (mode.equals(CDCSourceConstants.MODE_LISTENING)) {
            //Remove this CDCSource object from the CDCObjectKeeper.
            cdcSourceObjectKeeper.removeObject(this.hashCode());
        }
        //shutdown the executor service.
        executorService.shutdown();
    }

    @Override
    public void pause() {
        switch (mode) {
            case CDCSourceConstants.MODE_POLLING:
                cdcPoller.pause();
                break;
            case CDCSourceConstants.MODE_LISTENING:
                changeDataCapture.pause();
                break;
            default:
                break;
        }
    }

    @Override
    public void resume() {
        switch (mode) {
            case CDCSourceConstants.MODE_POLLING:
                cdcPoller.resume();
                break;
            case CDCSourceConstants.MODE_LISTENING:
                changeDataCapture.resume();
                break;
            default:
                break;
        }
    }

    public Map<byte[], byte[]> getOffsetData() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            log.error("Offset data retrieval failed.", e);
        }
        return offsetData;
    }

    public void setOffsetData(Map<byte[], byte[]> offsetData) {
        this.offsetData = offsetData;
    }

    /**
     * Used to Validate the parameters for the mode: listening.
     */
    private void validateListeningModeParameters(OptionHolder optionHolder) {
        //datasource.name should not be accepted for listening mode.
        if (optionHolder.isOptionExists(CDCSourceConstants.DATASOURCE_NAME)) {
            throw new SiddhiAppValidationException("Parameter: " + CDCSourceConstants.DATASOURCE_NAME + " should" +
                    " not be defined for listening mode");
        }

        if (!(operation.equalsIgnoreCase(CDCSourceConstants.INSERT)
                || operation.equalsIgnoreCase(CDCSourceConstants.UPDATE)
                || operation.equalsIgnoreCase(CDCSourceConstants.DELETE))) {
            throw new SiddhiAppValidationException("Unsupported operation: '" + operation + "'." +
                    " operation should be one of 'insert', 'update' or 'delete'");
        }

        if (carbonHome.isEmpty()) {
            throw new SiddhiAppValidationException("Couldn't initialize Carbon Home.");
        } else if (!historyFileDirectory.endsWith(File.separator)) {
            historyFileDirectory = historyFileDirectory + File.separator;
        }
    }

    /**
     * Used to Validate the parameters for the mode: polling.
     */
    private void validatePollingModeParameters() {
        if (pollingInterval < 0) {
            throw new SiddhiAppValidationException(CDCSourceConstants.POLLING_INTERVAL + " should be a " +
                    "non negative integer. Current mode: " + CDCSourceConstants.MODE_POLLING);
        }
    }

    @Override
    public Map<String, Object> currentState() {
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> map) {

    }

//    class CdcState extends State {
//
//        private final String mode;
//
//        private final Map<String, Object> state;
//
//        private CdcState(String mode) {
//            this.mode = mode;
//            state = new HashMap<>();
//        }
//
//        @Override
//        public boolean canDestroy() {
//            return false;
//        }
//
//        @Override
//        public Map<String, Object> snapshot() {
//            switch (mode) {
//                case CDCSourceConstants.MODE_POLLING:
//                    state.put("last.offset", cdcPoller.getLastReadPollingColumnValue());
//                    break;
//                case CDCSourceConstants.MODE_LISTENING:
//                    state.put(CDCSourceConstants.CACHE_OBJECT, offsetData);
//                    break;
//                default:
//                    break;
//            }
//            return state;
//        }
//
//        @Override
//        public void restore(Map<String, Object> map) {
//            switch (mode) {
//                case CDCSourceConstants.MODE_POLLING:
//                    Object lastOffsetObj = map.get("last.offset");
//                    cdcPoller.setLastReadPollingColumnValue((String) lastOffsetObj);
//                    break;
//                case CDCSourceConstants.MODE_LISTENING:
//                    Object cacheObj = map.get(CDCSourceConstants.CACHE_OBJECT);
//                    offsetData = (HashMap<byte[], byte[]>) cacheObj;
//                    break;
//                default:
//                    break;
//            }
//        }
//    }
}
