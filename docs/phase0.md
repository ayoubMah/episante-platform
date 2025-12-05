# ✅ **0.1 — Define Functional Scope**

## ###  Identify Core Features for MVP

### **1. Doctor Management**

- CRUD operations (create, update, list, delete)

- Doctor profile fields (name, specialty, email, phone, availability)

- Search & filter (optional)

- Integration with appointment system


### **2. Patient Management**

- CRUD operations

- Patient profile fields (name, age, email, phone, medical notes)

- Medical ID / UUID

- Linked appointments


### **3. Appointment Booking**

- Create an appointment (doctor ↔ patient)

- Update/cancel

- View appointments by:

    - doctor

    - patient

    - date

- Prevent overlapping appointments (basic rule)

- Email/sms simulation (optional)


### **4. Wearable Ingestion + Real-Time Alerting**

- Wearable device simulator sends JSON telemetry:

    - heart rate

    - oxygen saturation

    - temperature

    - fall detection

- Ingest Service → Kafka Producer

- Health Alert Engine → Kafka Consumer

- Alert rules:

    - high heart rate

    - low oxygen

    - falls

- Notify doctor UI in real-time (websocket or polling)


### **5. Analytics Dashboard (Minimal Version)**

- Doctor sees analytics:

    - number of appointments

    - active patients

    - alert trends

- Admin sees:

    - system health stats (optional)

- Powered by Gold layer (Postgres DWH) or mocked data in MVP


---

## ###  Define Non-Functional Requirements (NFR)

### **1. Multi-VM Deployment**

- Infrastructure uses distinct VMs:

    - Frontend VM

    - Backend VM

    - Data VM

    - Kafka VM

    - BigData VM

    - Gateway VM

    - Firewall VM (IPFire)

- Each VM runs isolated Docker services


### **2. High Availability (Simulation)**

- Simulate HA components:

    - Kafka cluster (1 node → logical cluster)

    - Spark structured streaming (auto-restart)

    - Nginx reverse proxy ensures routing stability

- No real load balancer needed, but architecture must be HA-ready


### **3. Secure Secrets Storage**

- All secrets stored in HashiCorp Vault:

    - Database credentials

    - JWT secret

    - Kafka credentials

- No secrets inside:

    - Git

    - application.yml

    - Jenkins


### **4. CI/CD Pipeline**

- Jenkins server (CICD VM) handles:

    - Backend builds + tests + Docker image creation

    - Frontend build + deploy

    - Gateway config reload

    - Remote VM deployment via SSH

- Automatic deploy on push to main (simulated production)