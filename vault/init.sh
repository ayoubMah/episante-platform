#!/bin/sh
set -e

echo "🚀 Starting Vault in dev mode..."
vault server -dev \
  -dev-root-token-id="$VAULT_DEV_ROOT_TOKEN_ID" \
  -dev-listen-address=0.0.0.0:8200 &

echo "⏳ Waiting for Vault to be ready..."
until vault status -address=http://localhost:8200 > /dev/null 2>&1; do
  echo "   ... not ready yet, retrying in 2s"
  sleep 2
done

echo "✅ Vault is up. Seeding secrets..."
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=$VAULT_DEV_ROOT_TOKEN_ID

vault kv put secret/episante \
  jwt-secret="$JWT_SECRET" \
  db-password-patient="$DB_PASSWORD_PATIENT" \
  db-password-doctor="$DB_PASSWORD_DOCTOR" \
  db-password-appointment="$DB_PASSWORD_APPOINTMENT" \
  db-password-auth="$DB_PASSWORD_AUTH"

echo "✅ Secrets seeded successfully."
wait