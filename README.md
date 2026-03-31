# 💳 PayPocket — Digital Wallet

Сервис электронного кошелька: регистрация, создание кошельков,
пополнение, снятие, переводы между пользователями
и просмотр истории операций.

> Учебный проект, разработанный в рамках подготовки
> к стажировке на позицию Backend Java-разработчик.

---

## Функциональность

- **Регистрация и авторизация** — создание аккаунта с проверкой уникальности username и email, вход с паролем
- **Мультивалютные кошельки** — создание кошельков в RUB, USD, EUR (один кошелёк на валюту)
- **Пополнение и снятие средств** — внесение и вывод средств с валидацией суммы (до 2 знаков после запятой)
- **Переводы** — перевод средств другому пользователю по username с проверкой валют, достаточности средств и подтверждением операции
- **Атомарные транзакции** — переводы выполняются в рамках БД-транзакции с блокировкой строк (SELECT FOR UPDATE)
- **История операций** — постраничный просмотр транзакций с датой, типом и суммой

---

## Стек технологий

| Технология | Назначение                       |
|------------|----------------------------------|
| Java 17    | Язык разработки                  |
| Gradle     | Система сборки проекта           |
| PostgreSQL | Реляционная база данных          |
| JDBC       | Взаимодействие с БД              |
| HikariCP   | Пул соединений                   |
| Docker     | Контейнер для PostgreSQL         |
| SLF4J      | Логирование                      |
| JUnit 5    | Тестирование (в планах)          |

---

## Архитектура

Слоистая архитектура (Layered Architecture) с разделением ответственности:

```
┌─────────────────────────────────┐
│         UI (ConsoleUI)          │  Ввод/вывод, меню, форматирование
├─────────────────────────────────┤
│  Service (UserService,          │  Бизнес-логика, валидация,
│           WalletService)        │  координация операций
├─────────────────────────────────┤
│  Repository (интерфейсы)        │  Контракт хранения данных
├─────────────────────────────────┤
│  JDBC (реализации)              │  Хранение в PostgreSQL
└─────────────────────────────────┘
```

Каждый слой знает только о слое ниже.
Сервисы работают с интерфейсами репозиториев —
реализацию можно заменить без изменения бизнес-логики.

---

## Структура проекта

```
paypocket/
├── build.gradle
├── settings.gradle
├── docker-compose.yml                    — PostgreSQL в Docker
├── README.md
└── src/main/
    ├── java/com/paypocket/
    │   ├── PayPocketApp.java             — точка входа, сборка зависимостей
    │   ├── model/                        — доменные сущности
    │   │   ├── User.java
    │   │   ├── Wallet.java
    │   │   ├── Transaction.java
    │   │   ├── TransactionType.java      — DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT
    │   │   └── Currency.java             — RUB, USD, EUR
    │   ├── repository/                   — интерфейсы и реализации хранения
    │   │   ├── Repository.java           — базовый generic CRUD
    │   │   ├── UserRepository.java
    │   │   ├── WalletRepository.java
    │   │   ├── TransactionRepository.java
    │   │   └── jdbc/                     — JDBC-реализации
    │   │       ├── AbstractJdbcRepository.java
    │   │       ├── JdbcUserRepository.java
    │   │       ├── JdbcWalletRepository.java
    │   │       └── JdbcTransactionRepository.java
    │   ├── service/                      — бизнес-логика
    │   │   ├── UserService.java          — регистрация, аутентификация
    │   │   └── WalletService.java        — кошельки, переводы, история
    │   ├── config/                       — конфигурация
    │   │   ├── AppConfig.java            — чтение application.properties
    │   │   └── DatabaseConnectionManager.java  — HikariCP пул соединений
    │   ├── dto/                          — объекты передачи данных
    │   │   └── TransferResult.java
    │   ├── exception/                    — типизированные исключения
    │   │   ├── PayPocketException.java
    │   │   ├── UserNotFoundException.java
    │   │   ├── DuplicateUserException.java
    │   │   ├── WalletNotFoundException.java
    │   │   ├── WalletAlreadyExistsException.java
    │   │   ├── InsufficientFundsException.java
    │   │   ├── SelfTransferException.java
    │   │   ├── InvalidAmountException.java
    │   │   └── CurrencyMismatchException.java
    │   └── ui/                           — пользовательский интерфейс
    │       └── ConsoleUI.java
    └── resources/
        ├── application.properties        — настройки подключения к БД
        └── db/migration/
            └── V1__create_tables.sql     — SQL-скрипт создания таблиц
```

---

## Как запустить

### Требования
- Java 17+
- Локально установленный PostgreSQL

```bash
# Создать базу данных
psql -U postgres -c "CREATE DATABASE paypocket;"

# Выполнить миграцию
psql -U postgres -d paypocket -f src/main/resources/db/migration/V1__create_tables.sql

# Проверить настройки в src/main/resources/application.properties
# Запустить приложение
./gradlew build run --console=plain
```

При первом запуске зарегистрируйте пользователя через консольное меню.

---

## Ключевые технические решения

**BigDecimal для денег** — `double` не может точно
представить десятичные дроби (0.1 + 0.2 ≠ 0.3 в IEEE 754).
В финансовых приложениях используется BigDecimal для точной арифметики.

**Двойная запись (double-entry)** — каждый перевод
создаёт две записи транзакций: TRANSFER_OUT у отправителя
и TRANSFER_IN у получателя. Это банковский стандарт,
обеспечивающий сверяемость данных.

**Атомарность переводов** — перевод выполняется в единой БД-транзакции
(BEGIN → COMMIT / ROLLBACK). SELECT FOR UPDATE блокирует строки
для предотвращения конкурентных изменений. Блокировки упорядочены
по UUID для предотвращения deadlock.

**Пул соединений (HikariCP)** — переиспользование TCP-соединений
с PostgreSQL вместо открытия нового на каждый запрос.

**Паттерны проектирования:**
- Builder — создание Transaction (много полей, часть опциональна)
- Strategy — интерфейс Repository с взаимозаменяемыми реализациями
- Template Method — AbstractJdbcRepository с общими CRUD-операциями
- Dependency Injection — сервисы получают зависимости через конструктор

---

## Планы развития

- [x] Консольное приложение на чистой Java (InMemory)
- [x] Мультивалютные кошельки
- [x] PostgreSQL + JDBC + HikariCP
- [x] Атомарные транзакции с SELECT FOR UPDATE
- [x] Docker для PostgreSQL
- [x] Логирование (SLF4J)
- [x] Пагинация истории операций
- [ ] Spring Boot + Spring Data JPA
- [ ] REST API + Swagger
- [ ] Веб-интерфейс (Thymeleaf)
- [ ] Docker-контейнер для приложения
- [ ] Конвертация валют (ExchangeRateProvider)

---

## Автор: Семенов Тимур

Разработано в рамках подготовки к стажировке
Backend Java-разработчик.