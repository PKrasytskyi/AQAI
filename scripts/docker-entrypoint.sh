#!/bin/sh
set -eu

run_demo() {
  mvn --offline --batch-mode --no-transfer-progress exec:java "-Dexec.args=--demo ${AQAI_DEMO}"
}

wait_for_service() {
  name="$1"
  url="$2"
  timeout="${AQAI_KNOWLEDGE_WAIT_TIMEOUT_SECONDS:-120}"
  elapsed=0

  until curl --silent --fail --max-time 3 "$url" >/dev/null 2>&1; do
    if [ "$elapsed" -ge "$timeout" ]; then
      echo "Timed out waiting for ${name} at ${url}" >&2
      exit 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
}

if [ "${KNOWLEDGE_DB_STATUS:-false}" = "true" ]; then
  wait_for_service "Neo4j" "${KNOWLEDGE_GRAPH_NEO4J_URL:-http://neo4j:7474}"
  wait_for_service "Qdrant" "${KNOWLEDGE_VECTOR_QDRANT_URL:-http://qdrant:6333}/healthz"
fi

capture_summary() {
  label="$1"
  summary_dir="${AQAI_ARTIFACTS_DIR:-}"
  [ -n "$summary_dir" ] || return 0
  mkdir -p "$summary_dir"

  for extension in json md; do
    source_file="target/ai-run/quality/build-week-demo-summary.${extension}"
    if [ -f "$source_file" ]; then
      cp "$source_file" "$summary_dir/${label}-summary.${extension}"
    fi
  done
}

case "${AQAI_RUN_SEQUENCE:-single}" in
  single)
    run_demo
    capture_summary "${AQAI_RUN_LABEL:-aqai-demo}"
    ;;
  seed-and-reuse)
    run_demo
    capture_summary "with-db-seed"
    run_demo
    capture_summary "with-db-reuse"
    ;;
  *)
    echo "Unsupported AQAI_RUN_SEQUENCE: ${AQAI_RUN_SEQUENCE}" >&2
    echo "Supported values: single, seed-and-reuse" >&2
    exit 64
    ;;
esac
