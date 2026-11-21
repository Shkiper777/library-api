#!/usr/bin/env bash
# Проверка CRUD: создаёт, читает, меняет и удаляет временную книгу.
# Дополнительно создаёт книгу "Преступление и наказание", которая остаётся в базе для проверки сохранения данных.
# Использование: scripts/crud-check.sh [BASE_URL]
set -u
BASE="${1:-http://localhost:8080}"
JSON='Content-Type: application/json'

run() {
  echo
  echo "\$ curl $*"
  curl -s -w '\nHTTP %{http_code}\n' "$@"
}

echo "=== CREATE ==="
out=$(curl -s -w '\nHTTP %{http_code}' -X POST "$BASE/books" -H "$JSON" \
  -d '{"title":"Мастер и Маргарита","author":"Михаил Булгаков","publishedYear":1967,"copies":3}')
echo "\$ curl -X POST $BASE/books -H '$JSON' -d '{\"title\":\"Мастер и Маргарита\",...}'"
echo "$out"
ID=$(echo "$out" | sed -n 's/.*"id": *\([0-9][0-9]*\).*/\1/p' | head -n1)
if [ -z "$ID" ]; then echo "Не удалось получить id созданной книги" >&2; exit 1; fi

echo; echo "=== READ (list) ==="
run "$BASE/books"

echo; echo "=== READ (one) ==="
run "$BASE/books/$ID"

echo; echo "=== UPDATE ==="
run -X PUT "$BASE/books/$ID" -H "$JSON" \
  -d '{"title":"Мастер и Маргарита","author":"Михаил Булгаков","publishedYear":1967,"copies":5}'

echo; echo "=== DELETE ==="
run -X DELETE "$BASE/books/$ID"

echo; echo "=== READ after delete (ожидается 404) ==="
run "$BASE/books/$ID"

echo; echo "=== Валидация (ожидается 400) ==="
run -X POST "$BASE/books" -H "$JSON" -d '{"title":"","author":"","publishedYear":0,"copies":-1}'

echo; echo "=== CREATE книги, которая останется в базе ==="
run -X POST "$BASE/books" -H "$JSON" \
  -d '{"title":"Преступление и наказание","author":"Фёдор Достоевский","publishedYear":1866,"copies":2}'
