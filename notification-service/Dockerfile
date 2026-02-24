# Build Stage
FROM golang:1.21-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY *.go ./
RUN CGO_ENABLED=0 GOOS=linux go build -o notification-service .

# Run Stage (Extremely lightweight alpine image)
FROM alpine:3.19
WORKDIR /root/
COPY --from=builder /app/notification-service .

CMD ["./notification-service"]