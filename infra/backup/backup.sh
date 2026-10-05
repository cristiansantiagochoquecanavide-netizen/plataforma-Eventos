#!/bin/sh
set -eu

backup_file="/backups/eventos-$(date -u +%Y%m%dT%H%M%SZ).dump"
pg_dump --format=custom --no-owner --no-acl --file="$backup_file"
pg_restore --list "$backup_file" >/dev/null
(cd /backups && sha256sum "$(basename "$backup_file")" > "$(basename "$backup_file").sha256")
printf 'Backup validado: %s\n' "$backup_file"
