# 💳 PayPocket — Digital Wallet

Сервис электронного кошелька: регистрация, создание кошельков,
пополнение, снятие, переводы между пользователями
и просмотр истории операций.

> Учебный проект, разработанный в рамках подготовки
> к стажировке на позицию Backend Java-разработчик.

---

## Функциональность

- **Регистрация и авторизация** – создание аккаунта с проверкой уникальности username и e-mail, вход с паролем
- **Мультивалютные кошельки** – создание кошельков в RUB, USD, EUR
- **Пополнение и снятие средств** – внесение и вывод средств на/с выбранного кошелька
- **Переводы** – перевод средств другому пользователю по username с проверкой соответствия валют, достаточности средств и подтверждением операции
- **История операций** – просмотр всех транзакций выбранного кошелька с датой, типом и суммой операции
- **Сохранение данных** – состояние приложение сохраняется в JSON-файл между запусками

---

## Стек технологий

| Технология | Назначение                           |
|------------|--------------------------------------|
| Java 21    | Язык разработки                      |
| Gradle     | Система сборки проекта               |
| Gson       | Сериализация / Десериализация в JSON |
| JUnit 5    | Тестирование (тесты в планах)        |

---

## Архитектура проекта

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
│  InMemory (реализации)          │  Хранение в ConcurrentHashMap
├─────────────────────────────────┤
│  Persistence (JSON файл)        │  Сохранение между запусками
└─────────────────────────────────┘
```

Каждый слой знает только о слое ниже.
Сервисы работают с интерфейсами репозиториев — 
реализацию можно заменить (InMemory → JDBC → JPA)
без изменения бизнес-логики.

---

## Структура проекта

```
paypocket/
├── build.gradle
├── settings.gradle
├── README.md
└── src/main/java/com/paypocket/
    ├── PayPocketApp.java                 — точка входа, сборка зависимостей
    ├── model/                            — доменные сущности
    │   ├── User.java
    │   ├── Wallet.java
    │   ├── Transaction.java
    │   ├── TransactionType.java          — enum: DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT
    │   └── Currency.java                 — enum: RUB, USD, EUR
    ├── repository/                       — интерфейсы хранения
    │   ├── Repository.java               — базовый generic CRUD-интерфейс
    │   ├── UserRepository.java
    │   ├── WalletRepository.java
    │   ├── TransactionRepository.java
    │   └── inmemory/                     — реализации в оперативной памяти
    │       ├── InMemoryUserRepository.java
    │       ├── InMemoryWalletRepository.java
    │       └── InMemoryTransactionRepository.java
    ├── service/                          — бизнес-логика
    │   ├── UserService.java              — регистрация, аутентификация
    │   └── WalletService.java            — кошельки, переводы, история
    ├── dto/                              — объекты передачи данных
    │   └── TransferResult.java
    ├── exception/                        — типизированные исключения
    │   ├── PayPocketException.java       — базовое исключение
    │   ├── UserNotFoundException.java
    │   ├── DuplicateUserException.java
    │   ├── WalletNotFoundException.java
    │   ├── WalletAlreadyExistsException.java
    │   ├── InsufficientFundsException.java
    │   ├── SelfTransferException.java
    │   ├── InvalidAmountException.java
    │   └── CurrencyMismatchException.java
    ├── persistence/                      — сохранение данных
    │   ├── AppData.java
    │   ├── JsonDataPersistence.java
    │   └── LocalDateTimeAdapter.java
    └── ui/                               — пользовательский интерфейс
        └── ConsoleUI.java

```

---

## Как запустить приложение?
**Требования:** Java 17+

```bash
# Клонировать репозиторий
git clone https://github.com/semenov-timur/paypocket.git PayPocket
cd PayPocket

# Собрать проект
./gradlew build

# Запустить
./gradlew run --console=plain
```

При первом запуске данных нет — зарегистрируйте пользователя через меню.
Данные автоматически сохраняются в `data/paypocket.json` при выходе.

---

## Ключевые технические решения
**BigDecimal для денег** — `double` не может точно 
представить десятичные дроби (0.1 + 0.2 ≠ 0.3 в IEEE 754). 
В финансовых приложениях используется BigDecimal 
для точной арифметики.

**Двойная запись (double-entry)** — каждый перевод 
создаёт две записи транзакций: TRANSFER_OUT у отправителя
и TRANSFER_IN у получателя. Это банковский стандарт, 
обеспечивающий сверяемость данных.

**Атомарность переводов** — операция перевода выполняет
списание перед зачислением. При ошибке зачисления средства
возвращаются отправителю (компенсирующая транзакция).

**Паттерны проектирования:**
- Builder — создание Transaction (т.к. много полей, часть опциональна)
- Strategy — интерфейс Repository с взаимозаменяемыми реализациями
- Dependency Injection — сервисы получают зависимости через конструктор

---

## Планы развития

- [x] Консольное приложение на чистой Java (InMemory)
- [x] Мультивалютные кошельки
- [x] Сохранение данных в JSON
- [ ] PostgreSQL + JDBC
- [ ] Spring Framework + Hibernate
- [ ] Spring Boot + REST API + Swagger
- [ ] Docker + docker-compose
- [ ] Конвертация валют (ExchangeRateProvider)

---

## Автор: Семенов Тимур

Разработано в рамках подготовки к стажировке
Backend Java-разработчик .