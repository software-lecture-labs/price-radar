#!/usr/bin/env bash
# Starts the backend against the database configured in .env.
# macOS/Linux counterpart of tools/run-backend.ps1.
#
#   ./tools/run-backend.sh
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
env_file="$root/.env"
if [ ! -f "$env_file" ]; then
	echo "No .env file at $env_file" >&2
	exit 1
fi

# The file is read, not sourced: values such as "Price Radar" contain spaces.
env_value() {
	tr -d '\r' < "$env_file" \
		| sed -n "s/^[[:space:]]*$1[[:space:]]*=\(.*\)$/\1/p" \
		| tail -n 1 \
		| sed 's/^[[:space:]]*//; s/[[:space:]]*$//'
}

url_decode() {
	printf '%b' "${1//%/\\x}"
}

url="$(env_value POSTGRES_URL)"
[ -n "$url" ] || url="$(env_value POSTGRES_DB)"
user="$(env_value POSTGRES_USER)"
password="$(env_value POSTGRES_PASSWORD)"

case "$url" in
	jdbc:postgres://* | jdbc:postgresql://* | postgres://* | postgresql://*) ;;
	*)
		echo "Set POSTGRES_URL, or POSTGRES_DB, in .env to a PostgreSQL URL." >&2
		exit 1
		;;
esac

rest="${url#jdbc:}"
rest="${rest#*://}"
authority="${rest%%/*}"
path_and_query="${rest#"$authority"}"
path="${path_and_query%%\?*}"
query="${path_and_query#"$path"}"

# Credentials in the URL win over POSTGRES_USER and POSTGRES_PASSWORD.
if [[ "$authority" == *@* ]]; then
	credentials="${authority%@*}"
	authority="${authority##*@}"
	user="$(url_decode "${credentials%%:*}")"
	if [[ "$credentials" == *:* ]]; then
		password="$(url_decode "${credentials#*:}")"
	fi
fi

host="${authority%%:*}"
port=5432
if [[ "$authority" == *:* ]]; then
	port="${authority#*:}"
fi
if [ -z "$host" ] || [ "${#path}" -le 1 ]; then
	echo "The PostgreSQL URL in .env needs a host and a database name." >&2
	exit 1
fi

# JDBC calls libpq's channel_binding option channelBinding.
query="$(printf '%s' "$query" | sed 's/\([?&]\)channel_binding=/\1channelBinding=/g')"

export POSTGRES_URL="jdbc:postgresql://$host:$port$path$query"
export POSTGRES_USER="$user"
export POSTGRES_PASSWORD="$password"

cd "$root/backend"
exec ./mvnw spring-boot:run
