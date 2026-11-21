#!/usr/bin/env bash
# Проверка API и сценария использования через API Gateway.
# Данные в запросах написаны латиницей: Git Bash на Windows портит кириллицу в аргументах curl (получается 400).
# Каждый шаг печатает команду, ответ и результат сверки ([OK] / [FAIL]).
# Использование: bash scripts/scenario.sh [BASE_URL]      (по умолчанию http://localhost:8080)
set -u
BASE="${1:-http://localhost:8080}"
JSON='Content-Type: application/json'
FAILS=0
CODE=""
BODY=""

call() { # call METHOD PATH [JSON-тело]
  local method="$1" path="$2" data="${3:-}" resp
  echo
  if [ -n "$data" ]; then
    echo "\$ curl -X $method $BASE$path -H '$JSON' -d '$data'"
    resp=$(curl -s -w '\n%{http_code}' -X "$method" "$BASE$path" -H "$JSON" -d "$data")
  else
    echo "\$ curl -X $method $BASE$path"
    resp=$(curl -s -w '\n%{http_code}' -X "$method" "$BASE$path")
  fi
  CODE=$(printf '%s\n' "$resp" | tail -n1)
  BODY=$(printf '%s\n' "$resp" | sed '$d')
  [ -n "$BODY" ] && echo "$BODY"
  echo "HTTP $CODE"
}

expect() { # expect КОД
  if [ "$CODE" = "$1" ]; then echo "  [OK]   ожидался код $1"
  else echo "  [FAIL] ожидался код $1, получен $CODE"; FAILS=$((FAILS+1)); fi
}

expect_body() { # expect_body ТЕКСТ  (проверка, что тело ответа содержит текст)
  if printf '%s' "$BODY" | grep -q -F "$1"; then echo "  [OK]   в ответе есть $1"
  else echo "  [FAIL] в ответе нет $1"; FAILS=$((FAILS+1)); fi
}

jid() { printf '%s' "$BODY" | sed -n 's/.*"id": *\([0-9][0-9]*\).*/\1/p' | head -n1; }

echo "##### 0. Gateway жив #####"
call GET /health; expect 200

echo; echo "##### 1. CRUD книг (catalog-service через gateway) #####"
call POST /books '{"title":"Temp Book","author":"Temp Author","publishedYear":2000,"copies":1}'; expect 201
TMP_BOOK=$(jid)
call GET /books; expect 200
call GET "/books/$TMP_BOOK"; expect 200
call PUT "/books/$TMP_BOOK" '{"title":"Temp Book v2","author":"Temp Author","publishedYear":2001,"copies":4}'; expect 200; expect_body "Temp Book v2"
call DELETE "/books/$TMP_BOOK"; expect 204
call GET "/books/$TMP_BOOK"; expect 404
call POST /books '{"title":"","author":"","publishedYear":0,"copies":-1}'; expect 400

echo; echo "##### 2. CRUD читателей (reader-service через gateway) #####"
call POST /readers '{"fullName":"Temp Reader","email":"temp@example.com"}'; expect 201
TMP_READER=$(jid)
call GET /readers; expect 200
call GET "/readers/$TMP_READER"; expect 200
call PUT "/readers/$TMP_READER" '{"fullName":"Temp Reader v2","email":"temp2@example.com"}'; expect 200; expect_body "Temp Reader v2"
call DELETE "/readers/$TMP_READER"; expect 204
call GET "/readers/$TMP_READER"; expect 404
call POST /readers '{"fullName":"","email":"not-an-email"}'; expect 400

echo; echo "##### 3. Данные, которые останутся в базе (для проверки после перезапуска) #####"
call POST /books '{"title":"Crime and Punishment","author":"Fyodor Dostoevsky","publishedYear":1866,"copies":2}'; expect 201
BOOK=$(jid)
call POST /readers '{"fullName":"Ivan Petrov","email":"ivan.petrov@example.com"}'; expect 201
READER=$(jid)

echo; echo "##### 4. Сценарий: выдача и возврат (loan-service обращается к соседним сервисам) #####"
echo "--- Было экземпляров у книги $BOOK (ожидается copies=2):"
call GET "/books/$BOOK"; expect 200; expect_body '"copies":2'

echo "--- Выдаём книгу читателю: loan-service проверяет читателя в reader-service и забирает экземпляр в catalog-service"
call POST /loans "{\"bookId\":$BOOK,\"readerId\":$READER}"; expect 201
expect_body "Crime and Punishment"; expect_body "Ivan Petrov"
LOAN_TMP=$(jid)

echo "--- Стало экземпляров (ожидается copies=1, значит loan-service изменил данные в catalog-service):"
call GET "/books/$BOOK"; expect 200; expect_body '"copies":1'

echo "--- Возвращаем книгу:"
call POST "/loans/$LOAN_TMP/return" ; expect 200
echo "--- Снова copies=2:"
call GET "/books/$BOOK"; expect 200; expect_body '"copies":2'

echo "--- Повторный возврат той же выдачи (ожидается 409):"
call POST "/loans/$LOAN_TMP/return"; expect 409

echo "--- Выдача несуществующей книги (ожидается 404):"
call POST /loans "{\"bookId\":999999,\"readerId\":$READER}"; expect 404
echo "--- Выдача несуществующему читателю (ожидается 404):"
call POST /loans "{\"bookId\":$BOOK,\"readerId\":999999}"; expect 404
echo "--- Неверный запрос без полей (ожидается 400):"
call POST /loans '{}'; expect 400

echo "--- Книга с одним экземпляром: вторая выдача должна дать 409"
call POST /books '{"title":"Single Copy","author":"Some Author","publishedYear":1999,"copies":1}'; expect 201
SINGLE=$(jid)
call POST /loans "{\"bookId\":$SINGLE,\"readerId\":$READER}"; expect 201
SINGLE_LOAN=$(jid)
call POST /loans "{\"bookId\":$SINGLE,\"readerId\":$READER}"; expect 409
call POST "/loans/$SINGLE_LOAN/return"; expect 200
call DELETE "/books/$SINGLE"; expect 204

echo; echo "##### 5. Активная выдача, которая останется в базе #####"
call POST /loans "{\"bookId\":$BOOK,\"readerId\":$READER}"; expect 201
call GET /loans; expect 200
call GET /books; expect 200; expect_body '"copies":1'

echo
if [ "$FAILS" -eq 0 ]; then
  echo "ИТОГ: все проверки пройдены"
  exit 0
else
  echo "ИТОГ: не пройдено проверок: $FAILS"
  exit 1
fi
