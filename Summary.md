## Введение
Java является серверным языком программирования, следовательно нам часто нужно будет обращаться к БД. Для этого и существует модуль JDBC, по сути это стандарт. Представляет собой набор классов для отправки запросов в БД

![alt text](img/jdbc-structure.png "jdbc-structure")

![alt text](img/jdbc-driver.png "jdbc-structure")

## Подключение к БД

Для создания запросов к БД в классе Connection есть методы, связанные с Statement:

- Statement - используется для простых запросов без изменяемых параметров. Обычные DDL операции.
- CallableStatement - вызов хранимых процедур, мало распространено, так как сейчас принято логику размещать на уровне приложения.
- PrepareStatement - запросы с изменяемой частью, наследуется от Statement.

DriverManager.getConnection(url, username, password) внутри себя создаёт объект класса java.util.Properties и делигирует вызов в другой getConnection, который принимает Properties.

Properties - обычный класс основанный на старой реализации Hashmap - HashTable. Ассациативный массив: название свойства - значение.

Далее DriverManager.getConnection пытается из всех зарегистрированных драйверов найти тот, который подходит по url. По url getConnection не может определить какой драйвер подходит для конкретной СУБД, поэтому он проверяет все возможные драйверы.

Если подключиться с помощью очередного драйвера не удалось, то сохраняется exception, если удалось, то выходим из цикла с return connection, если нет, то пробрасывается SQLException.

Для подключения к БД можно создать утилитный класс.

Все **Утилитные классы** должны быть final и иметь private конструктор.

Чтобы не обрабатывать исключение внутри метода, можно бросать RuntimeException, чтобы программа падала в случае исключения.

```java
 try {
            return DriverManager.getConnection(URL,USERNAME,PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
```

До Java 1.8 были проблемы с загрузкой драйверов (postgresql-42.5.4.jar) и они не находились в classpath. Их нужно было загружать дополнительно в статическом блоке инициализации:

```java
 static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
```

Класс загружается в память JVM, после Java 1.8 этап память называется MetaSpace.

Статический блок отрабатывает ровно один раз при загрузке в MetaSpace.

## app;ication.properties

Ручное получение пропертей из файла

```java
public final class PropertiesUtil {
    private static void loadProperties() {
            try(InputStream inputStream =
                        PropertiesUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
                PROPERTIES.load(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    ...
}

public final class ConnectionManager {

    public static Connection open(){
            try {
                return DriverManager.getConnection(
                        PropertiesUtil.get(USERNAME_KEY),
                        PropertiesUtil.get(PASSWORD_KEY),
                        PropertiesUtil.get(URL_KEY)
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
}

```

## Statement DDL операции

В реальном проекте каждый раз не создаётся новое подключение, а переиспользуются существующие. Т.е. создаётся пул соединений. Создание соединения - дорогостоящая операция.

Для выполнения универсального запроса используется метод Statement.execute(String): boolean. 
Возвращает true - если select и данные вернулись. false - любой не select оператор или ddl.

Statement в основном используется для ddl операций.

Есть более информативные разновидности этого метода 
- Statement.executeUpdate(): int для методов INSERT/UPDATE 
- Statement.executeQuery(): ResultSet для SELECT

Statement.executeLargeUpdate(): long - обновляет большое число строк (больше 4 байт или 1 млрд. записей)

Statement.getUpDateCount() - возвращает количество обновлённых строк при вставке в БД.

В одном SQL-запросе можно передавать сразу несколько вставок, удалений, создания таблиц.

Statement.executeUpdate() - чаще встречается на практике, возвращает колличество вставленных, обновленных или удлённых строк.

```java
String updateSql = """
                UPDATE info
                SET date = 'TestTest'
                WHERE id = 5
                RETURNING *
                """;
```

RETERNING - возвращаем строки, которые обновили. По сути получаем ResultSet. Statement.executeUpdate() при этом вернёт ошибку, так как возвращается в него не int, а Statement.execute() вернёт true.

## Result set

\* лучше не использовать в запросе SELECT, а перечислять все колонки, которые нужно получить.

для SELECT запросов используется Statement.executeQuery(), который возвращает ResultSet

ResultSet нужно закрывать.

ResultSet похож на итератор, в его методе next() совмещены методы hasNext() и next() итератора.

bigint в postgres аналог long в Java.

Есть два варианта получить данные из ResultSet:
- ResultSet.getLong(номер колонки) при SELECT с * не стоит полагаться на порядок колонок.
- ResultSet.getLong(имя колонки) 

ResultSet так же позволяет делать INSERT и UPDATE операции.

ResultSet.updateLong(номер колонки/имя колонки, значение) - этот метод у ResultSet позволяет обновить строку в БД. По умолчанию работать не будет, т.к. ResultSet read-only. Данные параметры нужно менять при создании Statement.

## Прокручиваемый ResultSet

Начиная с версии JDBC 2.0 появилась возможность направленной прокрутки набора результата.
Для этого, при создании Statement необходимо указать параметр желаемой прокрутки.

```java
Connection conn = ConnectionPool.init().getConnection();
Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
ResultSet rset = stmt.executeQuery("SELECT t.* FROM warehouses t");
...
rset.close();
stmt.close();
ConnectionPool.init().comebackConnection(conn);
```

- ResultSet.TYPE_FORWARD_ONLY: значение по умолчанию, прокрутка в одном направлении;
- ResultSet.TYPE_SCROLL_INSENSITIVE: прокрутка назад и вперед. При изменении данных в БД, ResultSet не отразит этого.
- ResultSet.TYPE_SCROLL_SENSITIVE: тоже самое что и 2, плюс отражает реальное представление данных в базе данных по мере их изменения.

Для перемещения курсора вперед, традиционно вызываем next(), для перемещения назад — previous(). Также можем вызвать first() или last() для перемещения в начало или конец курсора. Можно перейти на конкретную строку указав ее индекс — absolute(5) или переместиться относительно текущего на порядок — relative(3).

## Редактируемый ResultSet

Вместо создания привычных стейтментов для обновления (вставка, редактирование, удаление) мы можем воспользоваться ранее созданным ResultSet`ом. Но для этого во второй параметр метода по созданию Statement необходимо указать ResultSet.CONCUR_UPDATABLE.

В таком ResultSet вы сможете не только обновлять выбранные записи, но и создавать новые, а также производить удаление.

```java
Connection conn = ConnectionPool.init().getConnection();
Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
ResultSet rset = stmt.executeQuery("SELECT t.* FROM warehouses t"); //Псевдоним тут не случайно

// Обновим строку
rset.absolute(5); //Переходим на 5 строку
rset.updateObject("active_date", new java.sql.Date(new Date().getTime())); //Устанавливаем значение в поле
rset.updateRow(); //Фиксируем данные

//Вставим новую строку
rset.moveToInsertRow(); //Добавляем новую строку и переходим на нее
rset.updateObject("id", 123);
rset.updateObject("name", "Склад №4");
rset.updateObject("active_date", new java.sql.Date(new Date().getTime()));
rset.insertRow();  //Фиксируем данные

//Удалим строку
rset.last(); // Идем на последнюю строку
rset.deleteRow(); // Удаляем ее из БД

rset.close();
stmt.close();
ConnectionPool.init().comebackConnection(conn);
```
Вызов метода cancelRowUpdates() отменит все ожидающие изменения.
Перед вызовом методов updateRow() или insertRow() необходимо проверять, что текущая строка находится в состоянии обновления или создания соответственно. Иначе возникнет исключение.

ResultSet - аналог курсора в БД, но на практике его лучше использовать только для получения данных.

ResultSet можно не закрывать, так как он будет автоматически закрыт, тогда когда будет закрыт Statement, который его создал.

## ResultSet. Generated keys

При вставке данных в таблицу бывает необходимо получить автоматически сгенерированные ключи строк в БД. Это можно сделать вызвав метод Statement.getGeneratedKeys().

При этом в методе Statement.executeUpdate(sql_querry, autoGeneratedKeys) нужно указать второй параметр - autoGeneratedKeys, это набор констант, который пошёл ещё с времени, когда в Java не было enum.

Два возможных варианта:
- Statement.RETURN_GENERATED_KEYS
- Statement.NO_GENERATED_KEYS

## SQL Injection

Если значение в поле может быть null, то его лучше получать через ResultSet.getObject("id", Long.class)

```java
public class SqlInjectionEx {
    public static void main(String[] args) throws SQLException {

        String sqlParametr = "2 OR 1 = 1; DROP TABLE info;";
        System.out.println(extracted(sqlParametr));
    }

    private static List<Long> extracted(String sqlParametr) throws SQLException {
        String sqlInj = """
                SELECT id FROM ticket
                WHERE flight_id = %s
                """.formatted(sqlParametr);

        List<Long> result = new ArrayList<>();

        try (Connection connection = ConnectionManager.open();
                Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sqlInj);

            while (resultSet.next()) {
                result.add(resultSet.getObject("id", Long.class));
            }
        }
        return result;
    }
}
```
Через подобные вставки в текст sql-запроса можно внедрять вредоносный код и повредить БД.

Для того, чтобы обезопасить запросы нужно использовать PrepareStatement, который проверяет наличие sql Inоection и не допускает выполнение таких запросов.

## PrepareStatement

```java
private static List<Long> selectIdBetween(LocalDateTime time1, LocalDateTime time2) throws SQLException {

        List<Long> result = new ArrayList<>();

        String sql = """
                SELECT id FROM flight
                WHERE departure_date BETWEEN ? AND ?;
                """;

        try (Connection connection = ConnectionManager.open();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setTimestamp(1, Timestamp.valueOf(time1));
            preparedStatement.setTimestamp(2, Timestamp.valueOf(time2));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                result.add(resultSet.getLong("id"));
            }
        }

        return result;
    }
```

PrepareStatement знает как переводить типы Java в типы БД (LocalDateTime -> TimeStamp и т.д.)

PrepareStatement более быстрый т.к. прекомпилирует запросы, и более безопасный т.к. не позволяет выполнять SQL Injection.

Чтобы измебжать лишнего boxing/unboxing лучше использовать resultSet.getObject().

## Fetch Size

В БД может содержаться огромное колличество записей, и если все их запросить в ResultSet, может возникнуть пореполнение памяти
и приложение упадет с ошибкой OutOfMemmory.

При использовании запроса SELECT с параметром FetchSize=n из выборки будут отправлены первые n строк, затем следующие n и так далее.
Данные отправляются с сервера БД в память приложения. При этом данные из предыдущей пачке очищаются из памяти приложения и их место занимает новая порция данных.
При этом соединение с БД остаётся открытым.

В реальных приложениях FetchSize=50...100, однако можно подбирать свои значения для увеличения Performance (производительности) приложения.

При небольших размерах FetchSize обращения к БД будут частыми, что не очень хорошо, при большом FetchSize может не хватить памяти.

PrepareStatement.setFetchSize(50) - установить FetchSize у PrepareStatement.
PrepareStatement.setQueryTimeout(10) - как долго можно ждать выполнение запроса. Длительный захват соединения может быть не желательным, так как другие пользователи не смогут получить
соединение из пула. И сама установка соединения дорогостоящая операция.
PrepareStatement.setMaxRows(100) - максимальное число строк в результате запроса, так же необходимо, чтобы обезопасить приложение от OutOfMemmory Error.

## Meta Data

Meta Data - это вся информация о БД, ещё таблицах, колонках, типах данных, схемах и так далее.

Connection.getMetaData() - получить метадату.
DatabaseMetaData.getColumns(catalog, schemaPattern, tableNamePattern) - получить колонки и т.д.

Все методы DatabaseMetaData возвращают ResultSet.

"%" - значит вернуть любое значение.

```java
public class MetaDataEx {
    public static void main(String[] args) throws SQLException {
        checkMetaData();
    }

    private static void checkMetaData() throws SQLException {
        try (Connection connection = ConnectionManager.open()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet catalogs = metaData.getCatalogs();
            while (catalogs.next()) {
                String catalog = catalogs.getString(1);
                ResultSet schemas = metaData.getSchemas();
                while (schemas.next()) {
                    String schema = schemas.getString("TABLE_SCHEM");
                    ResultSet tables = metaData.getTables(catalog, schema, "%", null);
                    if (schema.equals("public")) {
                        while (tables.next()) {
                            System.out.println(tables.getString("TABLE_NAME"));
                        }
                    }
                }
            }
        }
    }
}
```

## Транзакции и блокировки

Транзакция - единица работы в рамках соединения с БД.
- выполняется полностью (commit)
- полностью откатывается (rollback), если возникла ошибка в результате одного из запросов.

до этого наши запросы выполнялись в autocommit mode. Открывалось столько транзакций, сколько было запросов.
READ COMMITED - уровень изоляции по умолчанию в PostgreSQL (поэтому видны только закомиченные изменения)

Отключать Connection.setAutoCommit(false) нужно до выполнения любых запросов.

После выполнения всех запросов вызываем Connection.commit()

Нельзя вызывать методы Connection.commit() или Connection.rollback() если Connection.setAutoCommit() стоит true - будет исключение.

В случае ConnectionPull необходимо возвращать Connection.setAutoCommit() в true.

```java
private static void deleteFromFlight(Long flightId) throws SQLException {

        String deleteFromTicketSql = "DELETE FROM ticket WHERE flight_id = ?";
        String deleteFromFlightSql = "DELETE FROM flight WHERE id = ?";

        Connection connection = null;
        PreparedStatement deleteTicketStatement = null;
        PreparedStatement deleteFlightStatement = null;

        try {
            connection = ConnectionManager.open();
            deleteTicketStatement = connection.prepareStatement(deleteFromTicketSql);
            deleteFlightStatement = connection.prepareStatement(deleteFromFlightSql);

            connection.setAutoCommit(false);

            deleteTicketStatement.setLong(1, flightId);
            deleteFlightStatement.setLong(1, flightId);

            deleteTicketStatement.executeUpdate();

            if (true) {
                throw new RuntimeException("Ooops");
            }

            deleteFlightStatement.executeUpdate();

            connection.commit();

        } catch (Exception e) {
           if (connection != null) {
               connection.rollback();
           }
           throw e;
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (deleteFlightStatement != null) {
                deleteFlightStatement.close();
            }
            if (deleteTicketStatement != null) {
                deleteTicketStatement.close();
            }
        }
    }
```

## Batch запросы

При выполнении Statement.execute() происходит открытие транзакции и отправка запроса в БД по протоколу TCP/IP. Затем возвращается ответ. Транзакция закрывается.
И так далее для всех запросов. 

Открытие каждый раз транзакции ресурсоёмкая операция, если нужно выполнить несколько запросов их можно упаковать в batch-запрос и тем самым сэкономить ресурсы и время выполнения.

Statement.executeBatch возвращает массив типа int - результатов выполнения запросов.

С помощью batch имеет смысл делать операции типа DELETE/INSERT/UPDATE или DDL-операции.

batch-запрос выполняется в рамках одной транзации, то есть если один запрос вызывает ошибку, то все не выполняются.

## Blob и Clob

Blob - binary large object (можно положить всё, что представимо в виде байт: картинки, видео, аудио, Word)

Clob - character large object.

Не во всех СУБД присутствуют эти типы данных. 

В Postgres Blob - bytea, clob - TEXT.

Для Oracle (в Posttgres нет) 
- connection.createClob(); - только ASCII символы
- connection.createNClob(); - любые символы

Для передачи Blob и Clob в БД нужно открывать транзакцию. В Postgres это происходит автоматически.

```java
public class BlobEx {
    public static void main(String[] args) throws SQLException, IOException {

        getImage();
//        saveImage();
    }

    private static void getImage() throws SQLException, IOException {
        String sql = """
                   SELECT image 
                   FROM aircraft
                   WHERE id = ?
                   """;

        try (Connection connection = ConnectionManager.open();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, 1);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                byte[] image = resultSet.getBytes("image");
                Files.write(Path.of("C:\\Java\\jdbc-course\\jdbc-starter\\src\\main\\resources\\", "boeingFromDb.jpg"),
                        image, StandardOpenOption.CREATE);
            }
        }
    }

    private static void saveImage() throws SQLException, IOException {

        String sql = """
                   UPDATE aircraft 
                   SET image = ?
                   WHERE id = 1
                   """;

        try (Connection connection = ConnectionManager.open();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {


            preparedStatement.setBytes(1, Files.readAllBytes(
                    Path.of("C:\\Java\\jdbc-course\\jdbc-starter\\src\\main\\resources\\", "boeing.jpg")));
            preparedStatement.executeUpdate();
        }
    }
}
```

На практике не стоит загружать картинки в БД, так как это замедляет работу БД, нужно хранить ссылки на файлы в облачном хранилище.

## Connection pool

В современных приложениях всегда используется connection pool, а не создаётся новое соединение, когда нам необходимо выполнить запрос из java приложения.

Connection pool - представляет собой коллекцию (список, очередь), которая сразу инициализирует несколько соединений.

При моделировании Connection pool есть два способа реализовать возвращение соединения в коллекцию:

1. Создать wrapper.
```java
class WrapperConnection implements Connection {

    private final Connection connection;
    private final BlockingQueue<Connection> pool;

    //implement all Connection methods

    @Override
    public void close() {
        pool.add(this);
    }
}
```

2. Proxy-объект

Каждый класс знает о ClassLoader который загрузил его в JVM.
```java
public final class ConnectionPoolManager {
    private static final String USERNAME_KEY = "db.username";
    private static final String PASSWORD_KEY = "db.password";
    private static final String URL_KEY = "db.url";
    private static final String POOL_SIZE_KEY = "db.pool.size";
    private static final Integer DEFAULT_POOL_SIZE = 10;
    private static BlockingQueue<Connection> pool;
    private static List<Connection> sourceConnection;

    static {
        loadDriver();
        initConnectionPool();
    }

    private ConnectionPoolManager() {
    }

    private static void initConnectionPool() {

        String poolSize = PropertiesUtil.get(POOL_SIZE_KEY);
        int size = poolSize == null ? DEFAULT_POOL_SIZE : Integer.parseInt(poolSize);
        pool = new ArrayBlockingQueue<>(size);
        sourceConnection = new ArrayList<>();
        for(int i = 0; i < size; i++) {
            Connection connection = open();
            Connection proxyConnection = (Connection)Proxy.newProxyInstance(ConnectionManager.class.getClassLoader(), new Class[]{Connection.class},
                    (proxy, method, args) -> method.getName().equals("close")
                            ? pool.add((Connection) proxy)
                            : method.invoke(connection, args));
            pool.add(proxyConnection);
            sourceConnection.add(connection);
        }

    }

    private static void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection get() {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private static Connection open(){
        try {
            return DriverManager.getConnection(
                    PropertiesUtil.get(URL_KEY),
                    PropertiesUtil.get(USERNAME_KEY),
                    PropertiesUtil.get(PASSWORD_KEY)
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closePool() {
        try {
            for (Connection connection : sourceConnection) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
```

## DAO теория

DAO - Data Access Object, паттерн проектирования. Объект доступа к данным. Обычный Java-объект, нужен для взаимодействия БД с сущностями.

Создаётся необходимое колличество DAO объектов, по одному на каждую таблицу в БД. DAO-объекты хранятся в одной папке (dao) и являются синглтонами.

Синглтон - воздается ровно один объект данного класса.

Объект класса Entity по сути представляет собой строку в БД.

Так как приходится часто работать с БД с помощью DAO и Entity были изобретены ORM-фреймворки
- Hibernate
- JOOQ
- MyBatis

ORM - Object Relative Mapping

Под капотом используется JDBC, но API более удобный.

![alt text](img/javaAppStructure.png "jdbc-structure")

Dao объект не хранит никакое состояние, поэтому он потокобезопасен.

```java
public class TicketDao {

    private static final TicketDao INSTANCE = new TicketDao();

    private TicketDao() {

    }

    public static TicketDao getInstance() {
        return INSTANCE;
    }
}

```

Класс не стоит делать final, потому что многие фреймворки создают Proxy на наши классы.

## CRUD

CRUD - Create, Read, Update, Delete - основные операции с таблицами.

В CRUD методах возможны исключения, мы должны создавать свои исключения в этих ситуациях.

В Create-методах обычно возвращают либо id вставленного объекта, либо сам объект с установленным id.

PrepareStatement.setLong предполагает, что long у нас есть, если таблица может иметь поле null, то нужно использовать PrepareStatement.setObject, чтобы избежать NPE.

```java
PreparedStatement preparedStatement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS))
```
Метод Update очень похож на Save и многие фреймворки объединяют эти методы.

Hibernate обязательно требует наличие конструктора без параметра.

В методах, где возможно возвращение null нужно возвращать Optional. В случае коллекции возвращаем пустую коллекцию.

**Shift + F6** - переименование.

## Batch-select с фильтрацией.

Select-запрос может вернуть большое колличество строк и их нужно ограничивать с помощью 
limit (колличество товаров на странице) и offset (переход на очередную вкладку пагинации), по сути batch-select.

DTO - шаблон проектирования, содержит объекты, в которых есть перечень полей. Проще чем DAO, служит для передачи из одного слоя в другой.

```java
public record TicketFilter(int limit,
                           int offset) {
}
```
record, появился в Java 14, автоматически генерирует конструктор для полей, указанных в скобках, геттеры, toString(), Equals(), HashCode()
сеттеры недопустимы, так как это immutable-объект.

```java
 List<Object> parametrs = new ArrayList<>();
        List<String> whereSql = new ArrayList<>();
        if (filter.seatNo() != null) {
            whereSql.add("seat_no LIKE ?");
            parametrs.add("%" + filter.seatNo() + "%");
        }
        if (filter.passengerName() != null) {
            whereSql.add("passenger_name = ?");
            parametrs.add(filter.passengerName());
        }
        parametrs.add(filter.limit());
        parametrs.add(filter.offset());

        String where = whereSql.stream()
                .collect(joining(" AND ", " WHERE ", " LIMIT ? OFFSET ? "));

        String sql = FIND_ALL_SQL + where;
```

вместо if можно подключить библиотеку Querydsl. Она может работать с Hibernate или только с схемой БД, которая генерует классы с помощью которых можно динамически строить where-условия.

Предположим у нас есть таблица с 100000 записями и нам нужно последовательно выбирать 20 записей.

select * from test limit 20 offset 100
select * from test limit 20 offset 1000
select * from test limit 20 offset 10000

Каждый раз время очередного запроса будет увеличиваться. Т.к. нужно будет прочитать первые n элементов в условии offset.

Для ускорения работы с batch-запросами нужно запоминать последний возвращенный id, и вместо OFFSET использовать условие WHERE id > n, чтобы пропустить первые n строк.
select * from test where id > 10000 limit 20;

## Сложный entity mapping

Возможны два варианта:
1. Сделать одним запросом с помощью JOIN и из результирующей таблицы сбилдить сущность, которая входит в другую.
- плюс - один запрос к БД
- минус - в Dao одной сущности нужно знать, как создаётся другая<br>
в случае, если в таблице есть другие внешние ключи, то мехнизм разрастается ещё больше.

2. Сделать отдельный Dao и с его помощью сбилдить объект, который передаётся в конструктор другого объекта.
- плюс - более понятная структура
- минус - дополнительные запросы к БД, более медленная работа.

Hibernate предоставляет оба этих варианта: Lazy - предоставить зависимые сущности по запросу, либо предоставить всё.


Так же при запросе с помощью отдельного Dao из ConnectionPool берется очередное соединение, которого может и не быть, тогда нам придётся ждать пока другой запрос вернет Connection. Либо, что ещё хуже может возникнуть DeadLock если кто-то одновременно вызывает FindById и ожидает пока появятся соединения в Pool.

В реальных приложениях Connection открывают на уровне сервиса и передают его на уровень Dao, с помощью AOP, или ThreadLocal переменных или FindByID(Long id, Connection connection)

```java
 @Override
    public Optional<Flight> findById(Long id) {
        try (Connection connection = ConnectionPoolManager.get()) {
            return findById(id, connection);
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }

    public Optional<Flight> findById(Long id, Connection connection) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            preparedStatement.setLong(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();

            Flight flight = null;

            if(resultSet.next()) {
                flight = new Flight(
                        resultSet.getLong("id"),
                        resultSet.getString("flight_no"),
                        resultSet.getTimestamp("departure_date").toLocalDateTime(),
                        resultSet.getString("departure_airport_code"),
                        resultSet.getTimestamp("arrival_date").toLocalDateTime(),
                        resultSet.getString("arrival_airport_code"),
                        resultSet.getInt("aircraft_id"),
                        resultSet.getString("status")
                );
            }

            return Optional.ofNullable(flight);
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }
```

Каждый ResultSet знает о PrepareStatement, который его открыл, а каждый PrepareStatement знает о Connection, который его создал. Поэтому ResultSet не обязательно закрывать.

```java
  ticket.setFlight(flightDao.findById(resultSet.getLong("flight_id"),resultSet.getStatement().getConnection()).orElse(null));
```

Обычно для разных DAO создаётся один интерфейс CRUD-операций, который реализуется в каждом DAO.

```java
public interface Dao<K, E> {

    boolean delete(K id);

    E save(E ticket);

    void update(E ticket);

    Optional<E> findById(K id);

    List<E> findAll();
}
```

В сущностях часто делают коллекции для реализации отношения 1 ко многим. При этом в ResultSet набор расплитывается, т.е. нужно из нескольких строк для одного и того же id создать коллекцию.

Hybernate предоставляет автоматический mapping, часто используется Lazy вариант, когда мы по требованию достаём коллекцию и устанавливаем её в нашу сущность.
















