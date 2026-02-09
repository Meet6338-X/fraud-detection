# Fraud Detection System

A real-time Java-based fraud detection system for financial transactions. Analyze transaction data and identify suspicious activities with an interactive web dashboard.

![Dashboard Preview](docs/images/dashboard-preview.png)

## Features

- **Real-time Transaction Analysis** - Process transactions as they occur
- **Configurable Rule Engine** - Adjustable fraud detection rules
- **Interactive Dashboard** - Comprehensive web-based UI with charts
- **Alert Management** - Automated fraud alerts with severity levels
- **Multi-threaded Processing** - High-performance concurrent processing
- **ML-Ready Architecture** - Prepared for machine learning integration

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+ (optional, for dependency management)

### Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/fraud-detection.git
cd fraud-detection

# Compile the Java code
javac -d target/classes src/main/java/fraud/*.java

# Run the application
java -cp target/classes fraud.FraudDetectionApplication 8080
```

### Access the Dashboard

Open your browser and navigate to:
```
http://localhost:8080
```

---

## Manual Run (Step by Step)

If you want to run the application manually on any laptop:

### Step 1: Install Java

**Windows:**
```
1. Download Java 17+ from https://www.oracle.com/java/technologies/downloads/
2. Run the installer
3. Set JAVA_HOME environment variable
4. Add Java to PATH
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

**macOS:**
```bash
brew install openjdk@17
```

### Step 2: Run the Application

```bash
# Navigate to project folder
cd fraud-detection

# Compile Java code
javac -d target/classes src/main/java/fraud/*.java

# Run the server on port 8080
java -cp target/classes fraud.FraudDetectionApplication 8080
```

### Step 3: Access Dashboard

1. Open your web browser
2. Go to: **http://localhost:8080**

---

## Project Structure

```
fraud-detection/
├── src/
│   └── main/
│       ├── java/
│       │   └── fraud/
│       │       ├── FraudDetectionApplication.java    # Main entry point
│       │       ├── FraudHttpHandler.java             # Static file handler
│       │       ├── ApiHttpHandler.java               # API endpoints
│       │       ├── RuleEngine.java                   # Fraud detection logic
│       │       ├── Transaction.java                  # Transaction model
│       │       ├── TransactionStorage.java            # Transaction CRUD
│       │       ├── FraudAlert.java                   # Alert model
│       │       ├── AlertStorage.java                # Alert management
│       │       ├── FraudDecision.java                # Decision result
│       │       ├── Location.java                    # Location model
│       │       └── JsonHelper.java                  # JSON serialization
│       └── resources/
│           └── static/
│               ├── index.html                       # Dashboard UI
│               ├── styles.css                       # Styles
│               └── app.js                           # Dashboard logic
├── docs/
│   ├── PROJECT_DOCUMENTATION.md                     # Full documentation
│   └── architecture.md                               # Architecture details
├── docker/                                          # Docker configuration
├── pom.xml                                          # Maven configuration
└── README.md                                        # This file
```

## API Endpoints

### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions` | Get all transactions |
| POST | `/api/transactions` | Create new transaction |
| POST | `/api/transactions/analyze` | Analyze transaction |
| GET | `/api/transactions/{id}` | Get transaction by ID |
| DELETE | `/api/transactions/{id}` | Delete transaction |

### Alerts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/alerts` | Get all alerts |
| DELETE | `/api/alerts/{id}` | Delete alert |

### Statistics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stats` | System statistics |
| GET | `/api/stats/patterns` | Fraud patterns |
| GET | `/api/stats/geography` | Geographic distribution |

### Rules

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/rules` | Get all rules |
| PUT | `/api/rules` | Update rule status |

### Example Request

```bash
curl -X POST http://localhost:8080/api/transactions/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "amount": 1500.00,
    "currency": "USD",
    "merchantId": "MRC-001",
    "location": {
      "city": "New York",
      "country": "US"
    }
  }'
```

## Dashboard Pages

1. **Dashboard** - Overview with summary cards and trends
2. **Transactions** - View, add, edit, delete transactions
3. **Alerts** - Manage fraud alerts with severity levels
4. **Patterns** - Hourly and amount distribution charts
5. **Geography** - Location-based fraud visualization
6. **Rules** - Configure detection rules
7. **ML Models** - ML model performance metrics

## Detection Rules

| Rule | Risk Score | Description |
|------|------------|-------------|
| Amount Rule | 30 | Flags transactions exceeding threshold |
| Velocity Rule | 25 | Detects rapid successive transactions |
| Location Rule | 35 | Identifies unusual geographic patterns |
| New Account Rule | 20 | Monitors new account activities |

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17+ |
| HTTP Server | Built-in HttpServer |
| Frontend | HTML5, CSS3, JavaScript |
| Charts | Chart.js |
| JSON Processing | Custom JsonHelper |
| Storage | In-memory ConcurrentHashMap |

## Docker Deployment

### Option 1: Using Docker Compose (Recommended)

```bash
# Build and run the container
docker-compose up --build

# Run in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop the container
docker-compose down
```

### Option 2: Using Docker Directly

```bash
# Build the image
docker build -t fraud-detection .

# Run the container
docker run -p 8080:8080 fraud-detection

# Run in detached mode
docker run -d -p 8080:8080 --name fraud-app fraud-detection

# View logs
docker logs -f fraud-app

# Stop and remove
docker stop fraud-app && docker rm fraud-app
```

### Verify Docker Deployment

Once the container is running, access the dashboard at:
```
http://localhost:8080
```

### Docker Health Check

The container includes a health check that verifies the application is responding:

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with pure Java - no external dependencies
- Inspired by real-world fraud detection systems
- Dashboard UI inspired by modern admin templates

---

**Note**: The Fraud Detection System is a demonstration application. For production use, integrate with proper ML models, databases, and security measures.
