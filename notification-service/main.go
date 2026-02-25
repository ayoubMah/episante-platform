package main

import (
	"context"
	"encoding/json"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/segmentio/kafka-go"
)

type AppointmentCreatedEvent struct {
	AppointmentID string `json:"appointmentId"`
	DoctorID      string `json:"doctorId"`
	PatientID     string `json:"patientId"`
	StartTime     string `json:"startTime"`
	EndTime       string `json:"endTime"`
	Status        string `json:"status"`
}

func main() {
	brokerAddress := os.Getenv("KAFKA_BROKER")
	if brokerAddress == "" {
		brokerAddress = "localhost:9094"
	}

	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:  []string{brokerAddress},
		GroupID:  "notification-group-go",
		Topic:    "appointment.created",
		MinBytes: 1,
		MaxBytes: 10e6,
		StartOffset: kafka.FirstOffset,
		MaxWait:  1 * time.Second,
	})

	// Graceful shutdown — listen for SIGINT (Ctrl+C) or SIGTERM (Docker stop)
	// When the OS sends a stop signal, we cleanly close the Kafka reader
	// This lets Kafka rebalance the consumer group immediately
	// instead of waiting for a session timeout (can be 30+ seconds)
	ctx, cancel := context.WithCancel(context.Background())
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		sig := <-sigChan
		log.Printf("🛑 Received signal: %v. Shutting down...", sig)
		cancel()
	}()

	log.Println("🟢 [NOTIFICATION SERVICE] Started. Listening on topic: appointment.created")

	for {
		message, err := reader.ReadMessage(ctx)
		if err != nil {
			// context.Canceled means we triggered shutdown intentionally — not an error
			if ctx.Err() != nil {
				log.Println("🛑 Context cancelled. Exiting consumer loop.")
				break
			}
			// Any other error is transient — log and continue, never crash
			log.Printf("❌ Error reading message: %v", err)
			continue
		}

		var event AppointmentCreatedEvent
		if err := json.Unmarshal(message.Value, &event); err != nil {
			// Bad message — log it with full payload for debugging, skip it
			// Never block the consumer for one malformed message
			log.Printf("⚠️ Failed to parse JSON at partition=%d offset=%d: %v | payload=%s",
				message.Partition, message.Offset, err, string(message.Value))
			continue
		}

		log.Printf("🔔 Event received | partition=%d offset=%d", message.Partition, message.Offset)
		log.Printf("   patientId=%s doctorId=%s startTime=%s", event.PatientID, event.DoctorID, event.StartTime)

		// Business logic isolated in its own function
		// When you add real email sending, it goes here — not inline in the loop
		handleNotification(event)
	}

	// Now reachable — called after clean shutdown
	if err := reader.Close(); err != nil {
		log.Printf("⚠️ Error closing Kafka reader: %v", err)
	}
	log.Println("✅ Kafka reader closed. Goodbye.")
}

func handleNotification(event AppointmentCreatedEvent) {
	// Today: mock. Tomorrow: call SendGrid, Twilio, etc.
	log.Printf("📧 [MOCK] Sending email to patient=%s for appointment=%s",
		event.PatientID, event.AppointmentID)
}