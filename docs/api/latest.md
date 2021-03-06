# API Docs - v2.0.0

## Source

### cdc *<a target="_blank" href="https://wso2.github.io/siddhi/documentation/siddhi-4.0/#source">(Source)</a>*

<p style="word-wrap: break-word">The CDC source receives events when change events (i.e., INSERT, UPDATE, DELETE) are triggered for a database table. Events are received in the 'key-value' format.<br>The key values of the map of a CDC change event are as follows.<br>&nbsp;&nbsp;&nbsp;&nbsp;For insert: Keys are specified as columns of the table.<br>&nbsp;&nbsp;&nbsp;&nbsp;For delete: Keys are followed followed by the specified table columns. This is achieved via 'before_'. e.g., specifying 'before_X' results in the key being added before the column named 'X'.<br>&nbsp;&nbsp;&nbsp;&nbsp;For update: Keys are followed followed by the specified table columns. This is achieved via 'before_'. e.g., specifying 'before_X' results in the key being added before the column named 'X'.<br>For 'polling' mode: Keys are specified as the coloumns of the table.<br>See parameter: mode for supported databases and change events.</p>

<span id="syntax" class="md-typeset" style="display: block; font-weight: bold;">Syntax</span>
```
@source(type="cdc", url="<STRING>", mode="<STRING>", jdbc.driver.name="<STRING>", username="<STRING>", password="<STRING>", pool.properties="<STRING>", datasource.name="<STRING>", table.name="<STRING>", polling.column="<STRING>", polling.interval="<INT>", operation="<STRING>", connector.properties="<STRING>", database.server.id="<STRING>", database.server.name="<STRING>", @map(...)))
```

<span id="query-parameters" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">QUERY PARAMETERS</span>
<table>
    <tr>
        <th>Name</th>
        <th style="min-width: 20em">Description</th>
        <th>Default Value</th>
        <th>Possible Data Types</th>
        <th>Optional</th>
        <th>Dynamic</th>
    </tr>
    <tr>
        <td style="vertical-align: top">url</td>
        <td style="vertical-align: top; word-wrap: break-word">The connection URL to the database.<br>F=The format used is: 'jdbc:mysql://&lt;host&gt;:&lt;port&gt;/&lt;database_name&gt;' </td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">mode</td>
        <td style="vertical-align: top; word-wrap: break-word">Mode to capture the change data. The type of events that can be received, and the required parameters differ based on the mode. The mode can be one of the following:<br>'polling': This mode uses a column named 'polling.column' to monitor the given table. It captures change events of the 'RDBMS', 'INSERT, and 'UPDATE' types.<br>'listening': This mode uses logs to monitor the given table. It currently supports change events only of the 'MySQL', 'INSERT', 'UPDATE', and 'DELETE' types.</td>
        <td style="vertical-align: top">listening</td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">jdbc.driver.name</td>
        <td style="vertical-align: top; word-wrap: break-word">The driver class name for connecting the database. **It is required to specify a value for this parameter when the mode is 'polling'.**</td>
        <td style="vertical-align: top"><Empty_String></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">username</td>
        <td style="vertical-align: top; word-wrap: break-word">The username to be used for accessing the database. This user needs to have the 'SELECT', 'RELOAD', 'SHOW DATABASES', 'REPLICATION SLAVE', and 'REPLICATION CLIENT'privileges for the change data capturing table (specified via the 'table.name' parameter).<br>To operate in the polling mode, the user needs 'SELECT' privileges.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">password</td>
        <td style="vertical-align: top; word-wrap: break-word">The password of the username you specified for accessing the database.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">pool.properties</td>
        <td style="vertical-align: top; word-wrap: break-word">The pool parameters for the database connection can be specified as key-value pairs.</td>
        <td style="vertical-align: top"><Empty_String></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">datasource.name</td>
        <td style="vertical-align: top; word-wrap: break-word">Name of the wso2 datasource to connect to the database. When datasource name is provided, the URL, username and password are not needed. A datasource based connection is given more priority over the URL based connection.<br>&nbsp;This parameter is applicable only when the mode is set to 'polling', and it can be applied only when you use this extension with WSO2 Stream Processor.</td>
        <td style="vertical-align: top"><Empty_String></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">table.name</td>
        <td style="vertical-align: top; word-wrap: break-word">The name of the table that needs to be monitored for data changes.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">polling.column</td>
        <td style="vertical-align: top; word-wrap: break-word">The column name  that is polled to capture the change data. It is recommended to have a TIMESTAMP field as the 'polling.column' in order to capture the inserts and updates.<br>Numeric auto-incremental fields and char fields can also be used as 'polling.column'. However, note that fields of these types only support insert change capturing, and the possibility of using a char field also depends on how the data is input.<br>**It is required to enter a value for this parameter when the mode is 'polling'.**</td>
        <td style="vertical-align: top"><Empty_String></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">polling.interval</td>
        <td style="vertical-align: top; word-wrap: break-word">The time interval (specified in seconds) to poll the given table for changes.<br>This parameter is applicable only when the mode is set to 'polling'.</td>
        <td style="vertical-align: top">1</td>
        <td style="vertical-align: top">INT</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">operation</td>
        <td style="vertical-align: top; word-wrap: break-word">The change event operation you want to carry out. Possible values are 'insert', 'update' or 'delete'. It is required to specify a value when the mode is 'listening'.<br>This parameter is not case sensitive.</td>
        <td style="vertical-align: top"></td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">No</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">connector.properties</td>
        <td style="vertical-align: top; word-wrap: break-word">Here, you can specify Debezium connector properties as a comma-separated string. <br>The properties specified here are given more priority over the parameters. This parameter is applicable only for the 'listening' mode.</td>
        <td style="vertical-align: top">Empty_String</td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">database.server.id</td>
        <td style="vertical-align: top; word-wrap: break-word">An ID to be used when joining MySQL database cluster to read the bin log. This should be a unique integer between 1 to 2^32. This parameter is applicable only when the mode is 'listening'.</td>
        <td style="vertical-align: top">-1</td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
    <tr>
        <td style="vertical-align: top">database.server.name</td>
        <td style="vertical-align: top; word-wrap: break-word">A logical name that identifies and provides a namespace for the database server. This parameter is applicable only when the mode is 'listening'.</td>
        <td style="vertical-align: top">{host}_{port}</td>
        <td style="vertical-align: top">STRING</td>
        <td style="vertical-align: top">Yes</td>
        <td style="vertical-align: top">No</td>
    </tr>
</table>

<span id="examples" class="md-typeset" style="display: block; font-weight: bold;">Examples</span>
<span id="example-1" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 1</span>
```
@source(type = 'cdc' , url = 'jdbc:mysql://localhost:3306/SimpleDB', 
username = 'cdcuser', password = 'pswd4cdc', 
table.name = 'students', operation = 'insert', 
@map(type='keyvalue', @attributes(id = 'id', name = 'name')))
define stream inputStream (id string, name string);
```
<p style="word-wrap: break-word">In this example, the CDC source listens to the row insertions that are made in the 'students' table with the column name, and the ID. This table belongs to the 'SimpleDB' MySQL database that can be accessed via the given URL.</p>

<span id="example-2" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 2</span>
```
@source(type = 'cdc' , url = 'jdbc:mysql://localhost:3306/SimpleDB', 
username = 'cdcuser', password = 'pswd4cdc', 
table.name = 'students', operation = 'update', 
@map(type='keyvalue', @attributes(id = 'id', name = 'name', 
before_id = 'before_id', before_name = 'before_name')))
define stream inputStream (before_id string, id string, 
before_name string , name string);
```
<p style="word-wrap: break-word">In this example, the CDC source listens to the row updates that are made in the 'students' table. This table belongs to the 'SimpleDB' MySQL database that can be accessed via the given URL.</p>

<span id="example-3" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 3</span>
```
@source(type = 'cdc' , url = 'jdbc:mysql://localhost:3306/SimpleDB', 
username = 'cdcuser', password = 'pswd4cdc', 
table.name = 'students', operation = 'delete', 
@map(type='keyvalue', @attributes(before_id = 'before_id', before_name = 'before_name')))
define stream inputStream (before_id string, before_name string);
```
<p style="word-wrap: break-word">In this example, the CDC source listens to the row deletions made in the 'students' table. This table belongs to the 'SimpleDB' database that can be accessed via the given URL.</p>

<span id="example-4" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 4</span>
```
@source(type = 'cdc', mode='polling', polling.column = 'id', 
jdbc.driver.name = 'com.mysql.jdbc.Driver', url = 'jdbc:mysql://localhost:3306/SimpleDB', 
username = 'cdcuser', password = 'pswd4cdc', 
table.name = 'students', 
@map(type='keyvalue'), @attributes(id = 'id', name = 'name'))
define stream inputStream (id int, name string);
```
<p style="word-wrap: break-word">In this example, the CDC source polls the 'students' table for inserts. 'id' that is specified as the polling colum' is an auto incremental field. The connection to the database is made via the URL, username, password, and the JDBC driver name.</p>

<span id="example-5" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 5</span>
```
@source(type = 'cdc', mode='polling', polling.column = 'id', datasource.name = 'SimpleDB',
table.name = 'students', 
@map(type='keyvalue'), @attributes(id = 'id', name = 'name'))
define stream inputStream (id int, name string);
```
<p style="word-wrap: break-word">In this example, the CDC source polls the 'students' table for inserts. The given polling column is a char column with the 'S001, S002, ... .' pattern. The connection to the database is made via a data source named 'SimpleDB'. Note that the 'datasource.name' parameter works only with the Stream Processor.</p>

<span id="example-6" class="md-typeset" style="display: block; color: rgba(0, 0, 0, 0.54); font-size: 12.8px; font-weight: bold;">EXAMPLE 6</span>
```
@source(type = 'cdc', mode='polling', polling.column = 'last_updated', datasource.name = 'SimpleDB',
table.name = 'students', 
@map(type='keyvalue'))
define stream inputStream (name string);
```
<p style="word-wrap: break-word">In this example, the CDC source polls the 'students' table for inserts and updates. The polling column is a timestamp field.</p>

