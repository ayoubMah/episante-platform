#!/bin/sh
# vault/init-vault.sh
# This script populates Vault with EpiSante secrets.
# Run it ONCE after Vault starts for the first time.

export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=episante-root-token  # matches VAULT_ROOT_TOKEN in .env

echo "⏳ Waiting for Vault to be ready..."
sleep 5

echo "🔐 Enabling KV secrets engine..."
vault kv enable-versioning secret/ 2>/dev/null || true

echo "📝 Writing EpiSante secrets..."
vault kv put secret/episante \
  jwt-secret="V5P0gvAl/J4VzUiijelQv79BLiLtm1KIrDqx9aQF2Mw=" \
  db-password-patient="patient_pass" \
  db-password-doctor="doctor_pass" \
  db-password-appointment="appointment_pass" \
  db-password-auth="auth_pass"

echo "✅ Vault seeded successfully."