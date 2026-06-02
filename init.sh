#!/usr/bin/env bash
# inicializacao do projeto - mysql + api
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

MYSQL_PORT="${MYSQL_PORT:-3309}"
API_PORT="${SERVER_PORT:-8080}"
CONTAINER="banco-digital-db"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"

detect_runtime() {
  if command -v docker >/dev/null && docker info >/dev/null 2>&1; then
    echo docker
  elif command -v podman >/dev/null && podman info >/dev/null 2>&1; then
    echo podman
  else
    echo ""
  fi
}

force_remove_container() {
  local rt="$1"
  $rt stop -t 5 "$CONTAINER" 2>/dev/null || true
  $rt kill -s KILL "$CONTAINER" 2>/dev/null || true
  sleep 2
  local pid
  pid=$($rt inspect -f '{{.State.Pid}}' "$CONTAINER" 2>/dev/null || echo 0)
  if [ -n "$pid" ] && [ "$pid" != "0" ]; then
    kill -9 "$pid" 2>/dev/null || true
    sleep 1
  fi
  $rt rm -f "$CONTAINER" 2>/dev/null || true
}

mysql_host_ok() {
  mysql -h127.0.0.1 -P"${MYSQL_PORT}" -ubanco -pbanco123 -e "SELECT 1" >/dev/null 2>&1
}

wait_mysql() {
  local rt="$1"
  echo "aguardando mysql..."
  for i in $(seq 1 60); do
    if mysql_host_ok; then
      echo "mysql ok"
      return 0
    fi
    sleep 2
  done
  echo "mysql nao respondeu a tempo"
  return 1
}

start_mysql() {
  local rt="$1"
  local status

  if $rt ps -a --format '{{.Names}}' 2>/dev/null | grep -q "^${CONTAINER}$"; then
    status=$($rt inspect -f '{{.State.Status}}' "$CONTAINER" 2>/dev/null || echo "missing")

    if [ "$status" = "running" ]; then
      if mysql_host_ok; then
        echo "mysql ok"
        return 0
      fi
      echo "mysql corrompido, recriando..."
      force_remove_container "$rt"
    elif [ "$status" = "stopping" ]; then
      echo "container travado, forçando parada..."
      $rt kill -s KILL "$CONTAINER" 2>/dev/null || true
      sleep 2
      status=$($rt inspect -f '{{.State.Status}}' "$CONTAINER" 2>/dev/null || echo "missing")
    fi

    if [ "$status" = "exited" ] || [ "$status" = "created" ] || [ "$status" = "paused" ]; then
      echo "container parado, subindo..."
      if $rt start "$CONTAINER" 2>/dev/null; then
        if wait_mysql "$rt"; then
          return 0
        fi
      fi
      echo "container parado nao subiu direito, recriando..."
      force_remove_container "$rt"
    elif [ "$status" != "missing" ]; then
      echo "recriando container..."
      force_remove_container "$rt"
    fi
  fi

  echo "criando mysql na porta ${MYSQL_PORT}..."
  if [ "$rt" = "podman" ]; then
    $rt run -d --replace --name "$CONTAINER" \
      -e MYSQL_ROOT_PASSWORD=root123 \
      -e MYSQL_DATABASE=banco_digital \
      -e MYSQL_USER=banco \
      -e MYSQL_PASSWORD=banco123 \
      -p "${MYSQL_PORT}:3306" \
      docker.io/library/mysql:8.0
  else
    $rt run -d --name "$CONTAINER" \
      -e MYSQL_ROOT_PASSWORD=root123 \
      -e MYSQL_DATABASE=banco_digital \
      -e MYSQL_USER=banco \
      -e MYSQL_PASSWORD=banco123 \
      -p "${MYSQL_PORT}:3306" \
      docker.io/library/mysql:8.0
  fi

  wait_mysql "$rt"
}

start_api_local() {
  export JAVA_HOME
  export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:${MYSQL_PORT}/banco_digital?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
  export SPRING_DATASOURCE_USERNAME=banco
  export SPRING_DATASOURCE_PASSWORD=banco123
  export SERVER_PORT="$API_PORT"

  echo ""
  echo "Subindo API (aguarde aparecer Started BancoDigitalApplication)..."
  echo ""
  echo "Depois acesse:"
  echo "  http://localhost:${API_PORT}/swagger-ui.html"
  echo ""
  echo "Em OUTRO terminal: ./init.sh test"
  echo "Para parar: Ctrl+C ou ./init.sh stop"
  echo ""

  mvn spring-boot:run
}

start_docker_compose() {
  local rt
  rt=$(detect_runtime)
  if [ -z "$rt" ]; then
    echo ""
    echo "ERRO: docker/podman nao disponivel."
    echo "Use: ./init.sh"
    echo ""
    exit 1
  fi

  if [ "$rt" = "podman" ]; then
    systemctl --user start podman.socket 2>/dev/null || true
  fi

  if ! $rt compose up --build "$@"; then
    echo ""
    echo "========================================"
    echo " compose falhou."
    echo ""
    echo " Use este comando (funciona com podman):"
    echo "   ./init.sh"
    echo "========================================"
    echo ""
    exit 1
  fi
}

reset_db() {
  local rt
  rt=$(detect_runtime)
  if [ -z "$rt" ]; then
    echo "docker/podman nao disponivel"
    exit 1
  fi

  echo "resetando banco..."
  pkill -f "com.banco.digital.BancoDigitalApplication" 2>/dev/null || true
  force_remove_container "$rt"
  $rt volume rm desafio_mysql_data 2>/dev/null || true
  $rt volume rm banco-digital_mysql_data 2>/dev/null || true
  echo "pronto. roda: ./init.sh"
}

smoke_test() {
  local base="http://localhost:${API_PORT}"
  echo "testando ${base} ..."

  code=$(curl -s -o /dev/null -w "%{http_code}" "$base/api/accounts" || echo "000")
  if [ "$code" != "200" ]; then
    echo ""
    echo "FALHOU (http $code) — a API nao esta rodando."
    echo ""
    echo "Primeiro suba o projeto:"
    echo "  ./init.sh"
    echo ""
    echo "Espere aparecer Started BancoDigitalApplication, depois rode ./init.sh test de novo."
    exit 1
  fi

  code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$base/api/transfers" \
    -H "Content-Type: application/json" \
    -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":1.00}')
  if [ "$code" != "201" ]; then
    echo "FALHOU - transferencia retornou $code"
    exit 1
  fi

  code=$(curl -s -o /dev/null -w "%{http_code}" "$base/swagger-ui/index.html")
  if [ "$code" != "200" ]; then
    echo "FALHOU - swagger $code"
    exit 1
  fi

  echo "ok — api, transferencia e swagger funcionando"
}

stop_all() {
  local rt
  rt=$(detect_runtime)
  pkill -f "com.banco.digital.BancoDigitalApplication" 2>/dev/null || true
  if [ -n "$rt" ]; then
    $rt stop -t 10 "$CONTAINER" 2>/dev/null || true
    $rt compose down 2>/dev/null || true
  fi
  echo "parado"
}

case "${1:-local}" in
  local|"")
    rt=$(detect_runtime)
    if [ -z "$rt" ]; then
      echo ""
      echo "ERRO: precisa de podman ou docker."
      echo "Linux: sudo apt install podman"
      echo "      ou: sudo usermod -aG docker \$USER (logout/login)"
      echo ""
      exit 1
    fi
    start_mysql "$rt"
    start_api_local
    ;;
  docker)
    shift || true
    start_docker_compose "$@"
    ;;
  reset)
    reset_db
    ;;
  test)
    smoke_test
    ;;
  stop)
    stop_all
    ;;
  *)
    echo ""
    echo "COMANDOS:"
    echo "  ./init.sh        <- SOBE O PROJETO (use este)"
    echo "  ./init.sh test   <- TESTA (em outro terminal, com api rodando)"
    echo "  ./init.sh stop   <- PARA TUDO"
    echo "  ./init.sh reset  <- RECRIA O BANCO"
    echo ""
    exit 1
    ;;
esac
