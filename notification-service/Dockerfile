# --- Stage 1: Build ---
FROM golang:1.24-alpine AS builder
WORKDIR /app

# Install SSL root certificates
RUN apk --no-cache add ca-certificates

COPY go.mod go.sum ./
RUN go mod download

COPY *.go ./
# OPTIMIZATION: -ldflags="-w -s" strips debugging info, making the binary 30% smaller
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o notification-service .

# --- Stage 2: Runtime (Zero Attack Surface) ---
FROM scratch
WORKDIR /

# Import the SSL certs from the builder
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=builder /app/notification-service /notification-service

# Run as non-root (nobody user standard UID)
USER 65534:65534

CMD ["/notification-service"]