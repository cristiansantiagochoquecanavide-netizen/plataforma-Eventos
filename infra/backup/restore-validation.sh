#!/bin/sh
set -eu

backup_directory=$(dirname "${BACKUP_FILE:-}")
backup_name=$(basename "${BACKUP_FILE:-}")
if [ "$backup_directory" != /backups ] || [ -z "$backup_name" ]; then
    echo 'BACKUP_FILE debe apuntar directamente a un archivo dentro de /backups' >&2
    exit 2
fi
case "$backup_name" in
    *.dump) ;;
    *) echo 'BACKUP_FILE debe usar formato custom con extensión .dump' >&2; exit 2 ;;
esac
case "$backup_name" in
    *..*) echo 'BACKUP_FILE no puede contener segmentos de ruta' >&2; exit 2 ;;
esac

if ! printf '%s' "${RESTORE_TARGET_DATABASE:-}" | grep -Eq '^restore_validation_[A-Za-z0-9_]+$'; then
    echo 'RESTORE_TARGET_DATABASE debe usar el prefijo restore_validation_' >&2
    exit 2
fi

test -f "$BACKUP_FILE"
test -f "$BACKUP_FILE.sha256"
test ! -L "$BACKUP_FILE"
test ! -L "$BACKUP_FILE.sha256"
checksum_fields=$(awk 'NR != 1 || NF != 2 { exit 1 } { print $1; print $2 } END { if (NR != 1) exit 1 }' \
    "$BACKUP_FILE.sha256") || {
    echo 'El sidecar SHA-256 debe contener exactamente un hash y un nombre de archivo.' >&2
    exit 2
}
expected_hash=$(printf '%s\n' "$checksum_fields" | sed -n '1p')
expected_name=$(printf '%s\n' "$checksum_fields" | sed -n '2p')
if [ "$expected_name" != "$backup_name" ] || ! printf '%s' "$expected_hash" | grep -Eq '^[[:xdigit:]]{64}$'; then
    echo 'El checksum no corresponde al archivo de backup solicitado.' >&2
    exit 2
fi
actual_hash=$(sha256sum "$BACKUP_FILE" | awk '{ print $1 }')
if [ "$actual_hash" != "$expected_hash" ]; then
    echo 'El checksum del backup no coincide.' >&2
    exit 2
fi

if psql --no-align --tuples-only \
    --command="SELECT 1 FROM pg_database WHERE datname = '$RESTORE_TARGET_DATABASE'" | grep -q 1; then
    echo 'La base destino ya existe; no se sobrescribirá.' >&2
    exit 3
fi

createdb --owner="$PGUSER" "$RESTORE_TARGET_DATABASE"
pg_restore --exit-on-error --no-owner --no-acl \
    --dbname="$RESTORE_TARGET_DATABASE" "$BACKUP_FILE"

tables=$(psql --no-align --tuples-only --dbname="$RESTORE_TARGET_DATABASE" \
    --command="SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
test "$tables" -gt 0
migration_table=$(psql --no-align --tuples-only --dbname="$RESTORE_TARGET_DATABASE" \
    --command="SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'")
test "$migration_table" -eq 1
successful_migrations=$(psql --no-align --tuples-only --dbname="$RESTORE_TARGET_DATABASE" \
    --command='SELECT count(*) FROM flyway_schema_history WHERE success')
test "$successful_migrations" -gt 0

printf 'Restauración aislada validada: %s (%s tablas, %s migraciones). La base se conserva para inspección.\n' \
    "$RESTORE_TARGET_DATABASE" "$tables" "$successful_migrations"
