-- Ролевая модель безопасности.
-- Добавляем колонку role. Все существующие и будущие пользователи по умолчанию
-- получают роль USER — функционал для них не меняется.
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Сид-администратор. Создаётся один раз при накатывании миграции.
-- Пароль 'admin1234' хранится BCrypt-хэшем ($2y$10$..., совместим со Spring BCrypt).
-- ВАЖНО: смените пароль администратора после первого запуска в реальной среде.
INSERT INTO users (id, username, email, password, role, created_at)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@paypocket.local',
    '$2y$10$CdrXjvPRqBrjssLB2hEQg..xvaTh/iKV7rYrWiYVztReSQvipGe/a',
    'ADMIN',
    NOW()
);
