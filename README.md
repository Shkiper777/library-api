# Библиотека книг: API каталога + PostgreSQL в Docker Compose

Контрольная точка: CRUD API на Java (Spring Boot), база PostgreSQL как отдельный сервис Docker Compose, данные хранятся в volume `db_data`.

## Связь с проектной темой

Тема проекта - библиотека книг (каталог, поиск, выдача и возврат). Сущность CRUD - **Book (книга)**: это основа каталога библиотеки, на ней позже строятся выдачи и бронирования.

Поля книги: `id`, `title` (название), `author` (автор), `publishedYear` (год издания), `copies` (количество экземпляров в фонде).

## Состав проекта

```
library-api/
├── src/main/java/com/example/library/   исходный код API
│   ├── BookController.java              REST-эндпоинты
│   ├── BookService.java                 логика CRUD
│   ├── BookRepository.java              доступ к PostgreSQL (Spring Data JPA)
│   ├── Book.java                        сущность (таблица books)
│   └── BookRequest.java                 тело запроса с валидацией
├── src/main/resources/application.yml   настройки, подключение берётся из переменных окружения
├── Dockerfile                           сборка и запуск API
├── docker-compose.yml                   сервисы db и api
├── scripts/crud-check.sh                CRUD-проверка через curl
├── scripts/verify.sh                    полная проверка, пишет результаты в docs/
└── docs/                                сюда verify.sh сохраняет подтверждения
```

## Сервисы docker-compose.yml

| Сервис | Образ | Назначение |
|---|---|---|
| `db` | `postgres:16` | PostgreSQL. База `app`, пользователь `postgres`, пароль `1234` |
| `api` | собирается из `Dockerfile` | Spring Boot API на порту 8080 |

- **Сеть:** оба сервиса в сети `library-net`. API подключается к базе по имени сервиса: `DB_HOST=db` (Docker DNS разрешает `db` в адрес контейнера).
- **Переменные окружения API:** `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. Адрес базы собирается в `application.yml`.
- **Volume:** `db_data` смонтирован в `/var/lib/postgresql/data`, поэтому данные переживают `docker compose down`.
- **depends_on:** `api` ждёт, пока `db` станет `healthy` (healthcheck через `pg_isready`).
- **Схема таблицы** создаётся автоматически при старте API (`ddl-auto: update`).

## Запуск

Нужен Docker Desktop или Docker Engine с Compose.

```bash
docker compose up --build
```

В другом терминале:

```bash
docker compose ps
docker compose logs
```

Остановка (данные сохраняются): `docker compose down`.
Полное удаление вместе с данными: `docker compose down -v`.

## Эндпоинты

| Метод | URL | Результат |
|---|---|---|
| GET | `/books` | список книг, 200 |
| GET | `/books/{id}` | книга, 200 или 404 |
| POST | `/books` | создать книгу, 201 |
| PUT | `/books/{id}` | изменить книгу, 200 или 404 |
| DELETE | `/books/{id}` | удалить книгу, 204 или 404 |

Неверные данные (пустое название или автор, год вне диапазона 1-2100, отрицательное число экземпляров) дают 400.

## Примеры curl

```bash
# Создать
curl -X POST http://localhost:8080/books -H "Content-Type: application/json" \
  -d '{"title":"The Master and Margarita","author":"Mikhail Bulgakov","publishedYear":1967,"copies":3}'

# Прочитать список и одну книгу
curl http://localhost:8080/books
curl http://localhost:8080/books/1

# Изменить
curl -X PUT http://localhost:8080/books/1 -H "Content-Type: application/json" \
  -d '{"title":"The Master and Margarita","author":"Mikhail Bulgakov","publishedYear":1967,"copies":5}'

# Удалить
curl -X DELETE http://localhost:8080/books/1
```

Данные в примерах написаны латиницей: в Git Bash на Windows кириллица в `-d` портится и API отвечает 400.

В Windows PowerShell вместо `curl` используйте `curl.exe`, а JSON в `-d` берите в одинарные кавычки с экранированием двойных (`\"`), либо работайте через Git Bash или Postman.

## Проверка и подтверждения

Автоматически (нужен bash: Linux, macOS, Git Bash или WSL):

```bash
bash scripts/verify.sh
```

Скрипт поднимает систему, выполняет CRUD, делает `docker compose down` и `up`, проверяет, что данные на месте, и сохраняет вывод:

| Файл | Что подтверждает |
|---|---|
| `docs/compose-ps.txt` | сервисы `api` и `db` запущены |
| `docs/crud-check.txt` | Create, Read, Update, Delete, 404 и 400 |
| `docs/persistence-check.txt` | данные сохранились после перезапуска |
| `docs/compose-logs.txt` | вывод `docker compose logs` |

Вручную:

1. `docker compose up -d --build` и `docker compose ps`
2. `bash scripts/crud-check.sh` (создаёт книгу «Crime and Punishment», она останется в базе)
3. `docker compose down`, затем `docker compose up -d`
4. `curl http://localhost:8080/books` - «Crime and Punishment» на месте
5. `docker compose logs`

Посмотреть данные прямо в базе:

```bash
docker exec -it library-db psql -U postgres -d app -c "SELECT * FROM books;"
```

## Заметки для защиты

- **Сущность:** Book, основа каталога библиотеки.
- **Почему PostgreSQL:** реляционные данные (книги, читатели, выдачи связаны между собой), транзакции, запуск отдельным сервисом. SQLite для этой работы не подходит.
- **Как API находит базу:** по имени сервиса `db` в сети `library-net`, параметры передаются через environment.
- **Что хранит volume `db_data`:** файлы PostgreSQL. Пока volume не удалён (`down -v`), данные остаются.
