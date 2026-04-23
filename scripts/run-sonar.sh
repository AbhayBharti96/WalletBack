#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICES=(
  "api-gateway"
  "auth-service"
  "eureka-server"
  "notification_service"
  "reward-service"
  "user-service"
  "wallet-service"
)

usage() {
  cat <<'EOF'
Usage:
  scripts/run-sonar.sh all
  scripts/run-sonar.sh <service-name>

Environment:
  SONAR_HOST_URL        SonarQube server URL
  SONAR_TOKEN           SonarQube token
  SONAR_PROJECT_PREFIX  Optional project key prefix (default: loyaltyService)

Examples:
  SONAR_HOST_URL=http://localhost:9000 SONAR_TOKEN=xxxx scripts/run-sonar.sh all
  SONAR_HOST_URL=http://localhost:9000 SONAR_TOKEN=xxxx scripts/run-sonar.sh user-service
EOF
}

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "$name is required" >&2
    exit 1
  fi
}

scan_service() {
  local service="$1"
  local project_key="${SONAR_PROJECT_PREFIX:-loyaltyService}:${service}"
  local mvn_cmd

  echo "Scanning ${service} with project key ${project_key}"

  (
    cd "${ROOT_DIR}/${service}"
    if [[ -s "./mvnw" ]]; then
      mvn_cmd=(sh ./mvnw)
    else
      mvn_cmd=(mvn)
    fi

    "${mvn_cmd[@]}" verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
      -Dsonar.host.url="${SONAR_HOST_URL}" \
      -Dsonar.token="${SONAR_TOKEN}" \
      -Dsonar.projectKey="${project_key}"
  )
}

main() {
  local target="${1:-}"

  if [[ -z "${target}" ]]; then
    usage
    exit 1
  fi

  require_env "SONAR_HOST_URL"
  require_env "SONAR_TOKEN"

  if [[ "${target}" == "all" ]]; then
    for service in "${SERVICES[@]}"; do
      scan_service "${service}"
    done
    exit 0
  fi

  for service in "${SERVICES[@]}"; do
    if [[ "${service}" == "${target}" ]]; then
      scan_service "${service}"
      exit 0
    fi
  done

  echo "Unknown service: ${target}" >&2
  usage
  exit 1
}

main "$@"
