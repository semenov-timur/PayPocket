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
- **Атомарные транзакции** — переводы выполняются через @Transactional с блокировкой строк (@Lock PESSIMISTIC_WRITE)
- **История операций** — постраничный просмотр транзакций с датой, типом и суммой
- **Веб-интерфейс** — дашборд, формы пополнения и перевода, история в браузере
- **REST API** — полноценный API с валидацией, обработкой ошибок и Swagger-документацией

---

## Стек технологий

| Технология      | Назначение                             |
|-----------------|----------------------------------------|
| Java 17         | Язык разработки                        |
| Spring Boot 3.3 | Фреймворк, автоконфигурация            |
| Spring Data JPA | ORM, репозитории без реализации         |
| Spring MVC      | Веб-интерфейс (Thymeleaf)              |
| Hibernate       | JPA-реализация, dirty checking          |
| PostgreSQL      | Реляционная база данных                |
| HikariCP        | Пул соединений (авто)                  |
| Flyway          | Автоматические миграции БД              |
| Swagger/OpenAPI | Документация REST API                  |
| Gradle          | Система сборки                         |
| SLF4J    | Логирование                            |

---

## Архитектура

Слоистая архитектура (Layered Architecture) с разделением ответственности:

```
┌─────────────────────────────────┐
│  Web UI (Thymeleaf Controllers) │  HTML-страницы в браузере
│  REST API (RestControllers)     │  JSON-ответы для клиентов
├─────────────────────────────────┤
│  Service (UserService,          │  Бизнес-логика, валидация,
│           WalletService)        │  @Transactional
├─────────────────────────────────┤
│  Repository (Spring Data JPA)   │  Интерфейсы без реализации
├─────────────────────────────────┤
│  Hibernate + PostgreSQL         │  ORM, SQL-генерация, пул соединений
└─────────────────────────────────┘
```

---

## Структура проекта

```
paypocket/
├── build.gradle
├── settings.gradle
├── Dockerfile
├── docker-compose.yml
├── README.md
└── src/main/
    ├── java/com/paypocket/
    │   ├── PayPocketApplication.java          — @SpringBootApplication
    │   ├── model/                             — JPA Entity
    │   │   ├── User.java
    │   │   ├── Wallet.java
    │   │   ├── Transaction.java
    │   │   ├── TransactionType.java
    │   │   └── Currency.java
    │   ├── repository/                        — Spring Data JPA
    │   │   ├── UserRepository.java
    │   │   ├── WalletRepository.java
    │   │   └── TransactionRepository.java
    │   ├── service/                           — бизнес-логика
    │   │   ├── UserService.java
    │   │   └── WalletService.java
    │   ├── controller/                        — веб-интерфейс (Thymeleaf)
    │   │   ├── AuthController.java
    │   │   ├── WalletController.java
    │   │   └── api/                           — REST API
    │   │       ├── UserApiController.java
    │   │       ├── WalletApiController.java
    │   │       └── GlobalExceptionHandler.java
    │   ├── config/                            — конфигурация
    │   │   └── OpenApiConfig.java
    │   ├── dto/                               — запросы и ответы API
    │   │   ├── CreateUserRequest.java
    │   │   ├── CreateWalletRequest.java
    │   │   ├── DepositRequest.java
    │   │   ├── TransferRequest.java
    │   │   ├── TransferResult.java
    │   │   ├── UserResponse.java
    │   │   ├── WalletResponse.java
    │   │   ├── TransactionResponse.java
    │   │   └── ErrorResponse.java
    │   └── exception/                         — типизированные исключения
    │       ├── PayPocketException.java
    │       ├── UserNotFoundException.java
    │       ├── DuplicateUserException.java
    │       ├── WalletNotFoundException.java
    │       ├── WalletAlreadyExistsException.java
    │       ├── InsufficientFundsException.java
    │       ├── SelfTransferException.java
    │       ├── InvalidAmountException.java
    │       └── CurrencyMismatchException.java
    └── resources/
        ├── application.yml                    — конфигурация Spring Boot
        ├── templates/                         — HTML-шаблоны Thymeleaf
        │   ├── login.html
        │   ├── register.html
        │   ├── dashboard.html
        │   ├── deposit.html
        │   ├── transfer.html
        │   └── history.html
        ├── static/css/
        │   └── style.css
        └── db/migration/
            └── V1__create_tables.sql          — Flyway-миграция
```

---

## Как запустить

### Вариант 1: Docker Compose (рекомендуется)

```bash
git clone https://github.com/semenov-timur/paypocket.git
cd paypocket
docker-compose up --build -d
```

### Вариант 2: Локальная разработка

Требования: Java 17+, PostgreSQL

```bash
git clone https://github.com/semenov-timur/paypocket.git
cd paypocket

# Запустить БД в контейнере или локально
docker-compose up -d db # запуск в контейнере
# или локально: psql -U postgres -c "CREATE DATABASE paypocket;"

# Запустить приложение
./gradlew bootRun
```

### Доступ

| Интерфейс      | URL                                   |
|----------------|---------------------------------------|
| Веб-приложение | http://localhost:8080                 |
| REST API       | http://localhost:8080/api/v1/...      |
| Swagger UI     | http://localhost:8080/swagger-ui.html |

---

## REST API

Интерактивная документация: http://localhost:8080/swagger-ui.html

| Метод | URL                               | Описание                 |
|-------|-----------------------------------|--------------------------|
| POST  | /api/v1/users                     | Регистрация пользователя |
| GET   | /api/v1/users/{id}                | Получить пользователя    |
| GET   | /api/v1/users                     | Список пользователей     |
| POST  | /api/v1/wallets?userId=...        | Создать кошелёк          |
| GET   | /api/v1/wallets?userId=...        | Кошельки пользователя    |
| GET   | /api/v1/wallets/{id}              | Информация о кошельке    |
| POST  | /api/v1/wallets/{id}/deposit      | Пополнение               |
| POST  | /api/v1/wallets/{id}/transfer     | Перевод средств          |
| GET   | /api/v1/wallets/{id}/transactions | История операций         |

---

## Ключевые технические решения

**BigDecimal для денег** — `double` не может точно
представить десятичные дроби (0.1 + 0.2 ≠ 0.3 в IEEE 754).
В финансовых приложениях используется BigDecimal для точной арифметики.

**Двойная запись (double-entry)** — каждый перевод
создаёт две записи транзакций: TRANSFER_OUT у отправителя
и TRANSFER_IN у получателя. Банковский стандарт, обеспечивающий
сверяемость данных.

**Атомарность переводов** — @Transactional с @Lock(PESSIMISTIC_WRITE).
Блокировки упорядочены по UUID для предотвращения deadlock.
При любой ошибке — автоматический ROLLBACK.

**Dirty Checking** — Hibernate отслеживает изменения managed-объектов
внутри @Transactional и автоматически генерирует UPDATE при commit.

**Spring Data JPA** — репозитории без реализации. Spring генерирует
SQL по имени метода (findByUsernameIgnoreCase → SELECT ... WHERE LOWER(username) = LOWER(?)).

**Глобальная обработка ошибок** — @RestControllerAdvice перехватывает
исключения и возвращает структурированный JSON с HTTP-статусом
(404, 409, 400) вместо 500 со стектрейсом.

**Паттерны проектирования:**
- Builder — создание Transaction (много полей, часть опциональна)
- Strategy — интерфейс Repository с взаимозаменяемыми реализациями
- Dependency Injection — Spring управляет зависимостями через конструкторы
- MVC — разделение контроллеров, сервисов и представлений
- DTO — разделение внутренних Entity и внешнего API

---

## Эволюция проекта

Проект прошёл через три этапа, каждый доступен через Git-теги:

| Версия | Стек                              | Описание                                             |
|--------|-----------------------------------|------------------------------------------------------|
| v1.0   | Java, InMemory, JSON              | Консольное приложение, хранение в памяти и файле     |
| v2.0   | JDBC, PostgreSQL, HikariCP        | Реальная БД, атомарные транзакции, SELECT FOR UPDATE |
| v3.0   | Spring Boot, JPA, Thymeleaf, REST | Веб-интерфейс, REST API, Swagger, Flyway             |

```bash
# Посмотреть конкретную версию
git checkout v1.0
git checkout v2.0
git checkout v3.0
```

---

## Планы развития

- [x] Консольное приложение на чистой Java (InMemory)
- [x] Мультивалютные кошельки
- [x] PostgreSQL + JDBC + HikariCP
- [x] Атомарные транзакции с SELECT FOR UPDATE
- [x] Логирование (SLF4J)
- [x] Spring Boot + Spring Data JPA
- [x] Веб-интерфейс (Thymeleaf)
- [x] REST API + Swagger/OpenAPI
- [x] Flyway миграции
- [x] Глобальная обработка ошибок
- [x] DTO с валидацией (Jakarta Validation)
- [x] Docker-контейнер для приложения
- [ ] Юнит-тесты (JUnit 5 + Mockito)
- [ ] Конвертация валют

---

## Автор: Семенов Тимур

Разработано в рамках подготовки к стажировке
Backend Java-разработчик.