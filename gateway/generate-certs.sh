#!/bin/sh
set -e

CERT_DIR="$(dirname "$0")/certs"
mkdir -p "$CERT_DIR"

if [ -f "$CERT_DIR/server.crt" ] && [ -f "$CERT_DIR/server.key" ]; then
    echo "Certificates already exist in $CERT_DIR — skipping generation."
    exit 0
fi

echo "Generating self-signed TLS certificate..."
openssl req -x509 -nodes -days 365 \
    -newkey rsa:2048 \
    -keyout "$CERT_DIR/server.key" \
    -out "$CERT_DIR/server.crt" \
    -subj "/C=FR/ST=IDF/L=Paris/O=EpiSante/CN=localhost"

echo "Certificates generated:"
echo "  $CERT_DIR/server.crt"
echo "  $CERT_DIR/server.key"
