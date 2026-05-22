package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
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
	AlertID       string  `json:"alertId"`
	PatientID     string  `json:"patientId"`
	DoctorID      string  `json:"doctorId"`
	AlertType     string  `json:"alertType"`
	Severity      string  `json:"severity"`
	Message       string  `json:"message"`
	ActualValue   float64 `json:"actualValue"`
	ThresholdUsed float64 `json:"thresholdUsed"`
	MetricName    string  `json:"metricName"`
	Timestamp     string  `json:"timestamp"`
}

type emailConfig struct {
	apiKey      string
	fromEmail   string
	doctorEmail string
}

func main() {
	brokerAddress := os.Getenv("KAFKA_BROKER")
	if brokerAddress == "" {
		brokerAddress = "localhost:9094"
	}

	cfg := emailConfig{
		apiKey:      os.Getenv("SENDGRID_API_KEY"),
		fromEmail:   os.Getenv("FROM_EMAIL"),
		doctorEmail: os.Getenv("DEMO_DOCTOR_EMAIL"),
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
	if cfg.apiKey != "" {
		log.Printf("   📧 SendGrid enabled → doctor alerts sent to %s", cfg.doctorEmail)
	} else {
		log.Println("   📧 SendGrid disabled (no SENDGRID_API_KEY) — mock mode")
	}

	var wg sync.WaitGroup

	wg.Add(1)
	go func() {
		defer wg.Done()
		consumeAppointments(ctx, brokerAddress)
	}()

	wg.Add(1)
	go func() {
		defer wg.Done()
		consumeHealthAlerts(ctx, brokerAddress, "health.alerts.patient", "patient", cfg)
	}()

	wg.Add(1)
	go func() {
		defer wg.Done()
		consumeHealthAlerts(ctx, brokerAddress, "health.alerts.doctor", "doctor", cfg)
	}()

	wg.Wait()
	log.Println("✅ All consumers closed. Goodbye.")
}

func sendEmail(cfg emailConfig, to, subject, body string) {
	if cfg.apiKey == "" || to == "" {
		log.Printf("📧 [MOCK EMAIL] To: %s | %s", to, subject)
		return
	}

	payload := map[string]interface{}{
		"personalizations": []map[string]interface{}{
			{"to": []map[string]string{{"email": to}}},
		},
		"from":    map[string]string{"email": cfg.fromEmail},
		"subject": subject,
		"content": []map[string]string{
			{"type": "text/plain", "value": body},
		},
	}

	data, _ := json.Marshal(payload)
	req, err := http.NewRequest("POST", "https://api.sendgrid.com/v3/mail/send", bytes.NewBuffer(data))
	if err != nil {
		log.Printf("⚠️ SendGrid request error: %v", err)
		return
	}
	req.Header.Set("Authorization", "Bearer "+cfg.apiKey)
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		log.Printf("⚠️ SendGrid send error: %v", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		log.Printf("⚠️ SendGrid HTTP %d for subject: %s", resp.StatusCode, subject)
	} else {
		log.Printf("✅ Email sent to %s: %s", to, subject)
	}
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
			log.Printf("⚠️ [appointment.created] Bad JSON: %v | %s", err, string(message.Value))
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

func consumeHealthAlerts(ctx context.Context, brokerAddress string, topic string, audience string, cfg emailConfig) {
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
			log.Printf("⚠️ [%s] Bad JSON: %v | %s", topic, err, string(message.Value))
			continue
		}

		log.Printf("🚨 [%s] %s | patient=%s | %s (value=%.1f, threshold=%.1f)",
			topic, event.Severity, event.PatientID, event.Message, event.ActualValue, event.ThresholdUsed)

		if audience == "patient" {
			log.Printf("📱 [MOCK SMS] Patient %s: %s", event.PatientID, event.Message)
		} else {
			subject := fmt.Sprintf("[EpiSante] %s Alert: %s", event.Severity, event.AlertType)
			body := fmt.Sprintf(
				"Patient ID: %s\n\nAlert: %s\nSeverity: %s\n\n%s\n\nActual: %.1f | Threshold: %.1f\nTimestamp: %s",
				event.PatientID, event.AlertType, event.Severity,
				event.Message, event.ActualValue, event.ThresholdUsed, event.Timestamp,
			)
			sendEmail(cfg, cfg.doctorEmail, subject, body)
		}
	}

	if err := reader.Close(); err != nil {
		log.Printf("⚠️ [%s] Error closing reader: %v", topic, err)
	}
}
