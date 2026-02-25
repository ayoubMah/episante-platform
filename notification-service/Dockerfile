# Build Stage
FROM golang:1.24-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY *.go ./
RUN CGO_ENABLED=0 GOOS=linux go build -o notification-service .

# Run Stage
FROM alpine:3.19
WORKDIR /root/
COPY --from=builder /app/notification-service .
CMD ["./notification-service"]