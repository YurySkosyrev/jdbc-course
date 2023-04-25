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

Далее getConnection пытается из всех зарегистрированных драйверов найти тот, который подходит по url. По url getConnection не может определить какой драйвер подходит для конкретной СУБД, поэтому он проверяет все возможные драйверы.

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






