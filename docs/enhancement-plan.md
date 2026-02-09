# Fraud Detection System Enhancement Plan

## Overview
This plan outlines the enhancements to be made to the fraud detection system:
1. Fix Fraud Patterns page charts
2. Add CRUD operations for records
3. Integrate ML model for fraud detection

---

## 1. Fix Fraud Patterns Page

### Current Issue
The Fraud Patterns page has placeholder chart canvases but no data rendering.

### Solution
Update the JavaScript to render charts with real/analyzed transaction data.

**Changes needed in `index.html`:**
- Load analyzed transactions from API
- Render hourly fraud distribution chart
- Render amount range analysis chart
- Add day of week analysis chart

### Implementation Steps:
1. Add API endpoint to get transaction statistics (`/api/stats/patterns`)
2. Update JavaScript to fetch pattern data
3. Initialize Chart.js with real data
4. Add time range filters (7d, 30d, 90d)

---

## 2. CRUD Operations for Records

### Features to Add:
- **Transactions**: Add, Edit, Delete transactions
- **Users**: Create users, update profiles, delete accounts
- **Alerts**: Resolve, delete, export alerts
- **Rules**: Enable/disable, configure rule thresholds

### API Endpoints to Add:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions` | Create new transaction |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |
| POST | `/api/users` | Create new user |
| PUT | `/api/users/{id}` | Update user profile |
| DELETE | `/api/users/{id}` | Delete user |
| PUT | `/api/rules/{name}` | Update rule configuration |

### Java Backend Changes:

1. **TransactionStorage.java** - Add CRUD methods:
   ```java
   public Transaction createTransaction(Transaction txn);
   public boolean updateTransaction(String id, Transaction txn);
   public boolean deleteTransaction(String id);
   ```

2. **UserProfileCache.java** - Add user management:
   ```java
   public UserProfile createUser(String userId);
   public boolean deleteUser(String userId);
   ```

3. **RuleEngine.java** - Add dynamic rule configuration:
   ```java
   public boolean setRuleThreshold(String ruleName, int threshold);
   public boolean enableRule(String ruleName);
   public boolean disableRule(String ruleName);
   ```

### UI Changes:

1. **Transaction Management Modal**:
   - Form to add new transaction
   - Edit button on each row
   - Delete confirmation dialog

2. **User Management Panel**:
   - User creation form
   - Profile editing
   - Account deletion

3. **Rule Configuration Panel**:
   - Toggle rules on/off
   - Adjust thresholds via sliders/inputs
   - Save configuration

---

## 3. ML Model Integration

### Architecture Overview
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Transaction    │────▶│  Rule Engine    │────▶│  ML Model       │
│  Submission     │     │  (Traditional)  │     │  (Optional)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                              │                         │
                              ▼                         ▼
                       ┌─────────────────────────────────────────┐
                       │           Combined Decision Engine        │
                       │  (Rule-based + ML ensemble scoring)       │
                       └─────────────────────────────────────────┘
```

### ML Model Options

**Option A: Simple Java-Based ML (Recommended for pure Java)**
- Use Smile or Tribuo libraries
- Train Random Forest or Decision Tree
- Export model as Java class

**Option B: Python ML Service (More powerful)**
- Train model with scikit-learn/TensorFlow
- Serve via Flask/FastAPI
- Java calls REST API

**Option C: Pre-trained Model Integration**
- ONNX Runtime for Java
- Load pre-trained fraud detection model
- Real-time inference

### Recommended Approach: Option A (Tribuo)

Tribuo is Oracle's Java ML library - perfect for this use case.

### Implementation Steps:

1. **Add ML Dependencies** (pom.xml):
   ```xml
   <dependency>
       <groupId>org.tribuo</groupId>
       <artifactId>tribuo-classification-tree</artifactId>
       <version>4.3.0</version>
   </dependency>
   ```

2. **Create ML Model Class**:
   ```java
   public class FraudDetectionModel {
       private RandomForest model;
       private Scaler scaler;
       
       public double predictFraudProbability(Transaction txn);
       public void trainModel(List<Transaction> trainingData);
       public void saveModel(String path);
       public void loadModel(String path);
   }
   ```

3. **Feature Engineering**:
   - Extract features from transactions:
     - Amount (normalized)
     - Hour of day
     - Day of week
     - Location risk score
     - User velocity
     - Merchant risk

4. **Model Training Pipeline**:
   ```
   Training Data → Feature Extraction → Normalization → Model Training → Validation
   ```

5. **Ensemble Decision**:
   - Combine rule-based score (0-100)
   - Combine ML probability (0.0-1.0)
   - Final score = weighted average
   - Decision thresholds:
     - < 40: APPROVE
     - 40-75: REVIEW
     - > 75: BLOCK

### Feature List for ML:
| Feature | Type | Description |
|---------|------|-------------|
| amount | continuous | Transaction amount |
| hour | categorical | Hour of transaction |
| dayOfWeek | categorical | Day of week |
| countryRisk | continuous | Country risk score |
| userVelocity | continuous | Txns per hour |
| merchantRisk | continuous | Merchant risk score |
| deviceFingerprint | categorical | Device trust score |
| amountZScore | continuous | Amount deviation from user avg |

### Model Training Data Format:
```json
[
  {
    "features": {
      "amount": 15000,
      "hour": 14,
      "dayOfWeek": 3,
      "countryRisk": 0.8,
      "userVelocity": 15,
      "merchantRisk": 0.5,
      "deviceFingerprint": 0.9,
      "amountZScore": 3.5
    },
    "label": "FRAUD",
    "transactionId": "TXN-001"
  }
]
```

---

## Implementation Priority

| Priority | Feature | Effort | Impact |
|----------|---------|--------|--------|
| 1 | Fix Fraud Patterns charts | Low | High |
| 2 | CRUD for transactions | Medium | High |
| 3 | CRUD for users | Medium | Medium |
| 4 | ML Model integration | High | High |
| 5 | Rule configuration UI | Low | Medium |

---

## Files to Modify

### Backend Java:
- `src/main/java/fraud/TransactionStorage.java`
- `src/main/java/fraud/UserProfileCache.java`
- `src/main/java/fraud/RuleEngine.java`
- `src/main/java/fraud/FraudHttpHandler.java`
- `src/main/java/fraud/ml/FraudMLModel.java` (new)
- `src/main/java/fraud/ml/FeatureExtractor.java` (new)

### Frontend:
- `src/main/resources/static/index.html`

### Configuration:
- `pom.xml` (add ML dependencies)

---

## Estimated Timeline

- **Phase 1** (Charts & CRUD): 2-3 days
- **Phase 2** (ML Model): 3-4 days

Total: 5-7 days for full implementation.
