#!/usr/bin/env bash
# Полная проверка контрольной точки. Результаты пишутся в папку docs/:
#   compose-ps.txt, scenario-check.txt, persistence-check.txt, compose-logs.txt
# Запускать из корня проекта в Git Bash / Linux / macOS / WSL:  bash scripts/verify.sh
set -uo pipefail
cd "$(dirname "$0")/.."
BASE="${BASE_URL:-http://localhost:8080}"
mkdir -p docs

wait_api() {
  echo "Жду запуск всех сервисов (первый запуск может занять несколько минут)..."
  for _ in $(seq 1 120); do
    if curl -s -o /dev/null -f "$BASE/books" && curl -s -o /dev/null -f "$BASE/readers" && curl -s -o /dev/null -f "$BASE/loans"; then
      echo "Все сервисы отвечают"; return 0
    fi
    sleep 2
  done
  echo "Сервисы не поднялись за 240 секунд. Последние логи:" >&2
  docker compose logs --no-color --tail=40 >&2
  exit 1
}

echo ">>> 1. docker compose up"
docker compose up -d --build || { echo "docker compose up завершился с ошибкой" >&2; exit 1; }
wait_api

echo; echo ">>> 2. docker compose ps"
docker compose ps | tee docs/compose-ps.txt

echo; echo ">>> 3. Проверка API и сценария (через gateway)"
bash scripts/scenario.sh "$BASE" | tee docs/scenario-check.txt
SCENARIO_RC=${PIPESTATUS[0]}

echo; echo ">>> 4. Перезапуск: docker compose down, затем up (volume db_data сохраняется)"
{
  echo "\$ docker compose down"
  docker compose down 2>&1
  echo
  echo "\$ docker compose up -d"
  docker compose up -d 2>&1
} | tee docs/persistence-check.txt
wait_api

PERSIST_FAILS=0
check_contains() { # check_contains PATH ТЕКСТ
  local out line
  out=$(curl -s "$BASE$1")
  if printf '%s' "$out" | grep -q -F "$2"; then
    line="  [OK]   данные на месте: $2"
  else
    line="  [FAIL] данных нет: $2"
    PERSIST_FAILS=$((PERSIST_FAILS+1))
  fi
  {
    echo
    echo "\$ curl $BASE$1   (после перезапуска)"
    echo "$out"
    echo "$line"
  } | tee -a docs/persistence-check.txt
}
{
  echo
  echo "\$ docker compose ps"
  docker compose ps
} | tee -a docs/persistence-check.txt
check_contains /books "Crime and Punishment"
check_contains /readers "Ivan Petrov"
check_contains /loans "Ivan Petrov"

echo; echo ">>> 5. docker compose logs"
docker compose logs --no-color > docs/compose-logs.txt
echo "Логи сохранены в docs/compose-logs.txt ($(wc -l < docs/compose-logs.txt) строк)"

echo
if [ "$SCENARIO_RC" -eq 0 ] && [ "$PERSIST_FAILS" -eq 0 ]; then
  echo "ГОТОВО: сценарий пройден, данные сохранились после перезапуска. Подтверждения лежат в docs/"
else
  echo "ВНИМАНИЕ: есть непройденные проверки (сценарий: код $SCENARIO_RC, после перезапуска: $PERSIST_FAILS). Смотрите docs/scenario-check.txt и docs/persistence-check.txt" >&2
  exit 1
fi
