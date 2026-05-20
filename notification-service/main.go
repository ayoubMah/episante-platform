package main

import (
	"context"
	"encoding/json"
	"log"
	"os"
	"os/signal"
	"sync"
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

type HealthAlertEvent struct {
	AlertID      string  `json:"alertId"`
	PatientID    string  `json:"patientId"`
	DoctorID     string  `json:"doctorId"`
	AlertType    string  `json:"alertType"`
	Severity     string  `json:"severity"`
	Message      string  `json:"message"`
	ActualValue  float64 `json:"actualValue"`
	ThresholdUsed float64 `json:"thresholdUsed"`
	MetricName   string  `json:"metricName"`
	Timestamp    string  `json:"timestamp"`
}

func main() {
	brokerAddress := os.Getenv("KAFKA_BROKER")
	if brokerAddress == "" {
		brokerAddress = "localhost:9094"
	}

	ctx, cancel := context.WithCancel(context.Background())
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		sig := <-sigChan
		log.Printf("🛑 Received signal: %v. Shutting down...", sig)
		cancel()
	}()

	log.Println("🟢 [NOTIFICATION SERVICE] Started.")

	var wg sync.WaitGroup

	wg.Add(1)
	go func() {
		defer wg.Done()
		consumeAppointments(ctx, brokerAddress)
	}()

	wg.Add(1)
	go func() {
		defer wg.Done()
		consumeHealthAlerts(ctx, brokerAddress, "health.alerts.patient", "patient")
	}()

	wg.Add(1)
	go func() {
		defer wg.Done()
		consumeHealthAlerts(ctx, brokerAddress, "health.alerts.doctor", "doctor")
	}()

	wg.Wait()
	log.Println("✅ All consumers closed. Goodbye.")
}

func consumeAppointments(ctx context.Context, brokerAddress string) {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:     []string{brokerAddress},
		GroupID:     "notification-group-go",
		Topic:       "appointment.created",
		MinBytes:    1,
		MaxBytes:    10e6,
		StartOffset: kafka.FirstOffset,
		MaxWait:     1 * time.Second,
	})

	log.Println("   Listening on topic: appointment.created")

	for {
		message, err := reader.ReadMessage(ctx)
		if err != nil {
			if ctx.Err() != nil {
				log.Println("   [appointment.created] Context cancelled.")
				break
			}
			log.Printf("❌ [appointment.created] Error: %v", err)
			time.Sleep(5 * time.Second)
			continue
		}

		var event AppointmentCreatedEvent
		if err := json.Unmarshal(message.Value, &event); err != nil {
			log.Printf("⚠️ [appointment.created] Bad JSON at partition=%d offset=%d: %v | %s",
				message.Partition, message.Offset, err, string(message.Value))
			continue
		}

		log.Printf("🔔 [appointment.created] patient=%s doctor=%s start=%s",
			event.PatientID, event.DoctorID, event.StartTime)

		log.Printf("📧 [MOCK] Sending email to patient=%s for appointment=%s",
			event.PatientID, event.AppointmentID)
	}

	if err := reader.Close(); err != nil {
		log.Printf("⚠️ [appointment.created] Error closing reader: %v", err)
	}
}

func consumeHealthAlerts(ctx context.Context, brokerAddress string, topic string, audience string) {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:     []string{brokerAddress},
		GroupID:     "notification-group-go",
		Topic:       topic,
		MinBytes:    1,
		MaxBytes:    10e6,
		StartOffset: kafka.FirstOffset,
		MaxWait:     1 * time.Second,
	})

	log.Printf("   Listening on topic: %s (audience=%s)", topic, audience)

	for {
		message, err := reader.ReadMessage(ctx)
		if err != nil {
			if ctx.Err() != nil {
				log.Printf("   [%s] Context cancelled.", topic)
				break
			}
			log.Printf("❌ [%s] Error: %v", topic, err)
			time.Sleep(5 * time.Second)
			continue
		}

		var event HealthAlertEvent
		if err := json.Unmarshal(message.Value, &event); err != nil {
			log.Printf("⚠️ [%s] Bad JSON at partition=%d offset=%d: %v | %s",
				topic, message.Partition, message.Offset, err, string(message.Value))
			continue
		}

		log.Printf("🚨 [%s] %s | patient=%s | %s (value=%.1f, threshold=%.1f)",
			topic, event.Severity, event.PatientID, event.Message, event.ActualValue, event.ThresholdUsed)

		if audience == "patient" {
			log.Printf("📱 [MOCK SMS] Patient %s: %s", event.PatientID, event.Message)
		} else {
			log.Printf("📧 [MOCK EMAIL] Doctor %s: Alert for patient %s - %s",
				event.DoctorID, event.PatientID, event.Message)
		}
	}

	if err := reader.Close(); err != nil {
		log.Printf("⚠️ [%s] Error closing reader: %v", topic, err)
	}
}
