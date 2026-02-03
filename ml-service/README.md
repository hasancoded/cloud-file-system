# ML Predictor Service

Python-based Machine Learning service for load prediction in CloudFileSystem.

## Overview

This service uses a Random Forest Regressor to predict file operation load 30 minutes ahead, enabling proactive container scaling.

**Model Performance:**

- RMSE: < 100 requests/hour
- R² Score: > 0.85
- Prediction Horizon: 30 minutes

## Prerequisites

- Python 3.9 or higher
- pip (Python package manager)

## Installation

### 1. Create Virtual Environment (Recommended)

```bash
# Windows
python -m venv venv
venv\Scripts\activate

# Linux/Mac
python3 -m venv venv
source venv/bin/activate
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

## Usage

### Step 1: Generate Training Data

Generate 6 months of synthetic traffic data with realistic patterns:

```bash
python generate_data.py
```

**Output:**

- `training_data.csv` - Training dataset (~4,300 data points)
- `data_visualization.png` - 7-day sample visualization

**Expected Console Output:**

```
[Data Generator] Generating 6 months of synthetic data...
[Data Generator] ✓ Generated 4320 data points
[Data Generator] ✓ Saved to training_data.csv

=== Data Statistics ===
Date range: 2025-07-07 to 2026-01-07
Load range: 15 - 6543 req/hr
Average load: 487 req/hr

Business hours avg: 756 req/hr
Non-business hours avg: 218 req/hr
Peak ratio: 3.47x
```

### Step 2: Train the Model

Train the Random Forest model on generated data:

```bash
python train_model.py
```

**Output:**

- `model.pkl` - Trained model
- `scaler.pkl` - Feature scaler
- `model_metadata.json` - Performance metrics
- `model_YYYYMMDD_HHMMSS.pkl` - Versioned model copy

**Expected Console Output:**

```
[Model Training] Loading data from training_data.csv...
[Model Training] ✓ Loaded 4320 data points
[Model Training] Training Random Forest...
[Model Training] ✓ Model training complete

MODEL PERFORMANCE METRICS
RMSE: 45.23 req/hr
MAE: 38.15 req/hr
R² Score: 0.8912
✓ RMSE target met (< 100 req/hr)
✓ R² target met (> 0.85)

FEATURE IMPORTANCE
avg_load_last_7d         0.3245 ████████████████████████████████
hour_of_day              0.2156 █████████████████████
load_1h_ago              0.1987 ███████████████████
is_business_hours        0.1234 ████████████
```

### Step 3: Start the Flask API

Run the prediction API server:

```bash
python app.py
```

**Expected Console Output:**

```
[ML API] Loading model artifacts...
[ML API] ✓ Model loaded successfully
[ML API] ✓ Scaler loaded successfully
[ML API] ✓ Model metadata loaded (RMSE: 45.23)

[ML API] Starting Flask server on http://localhost:5000
[ML API] Endpoints:
  - POST /predict       : Get load prediction
  - GET  /health        : Check API health
  - GET  /metrics       : Get prediction metrics
```

## API Endpoints

### POST /predict

Get load prediction 30 minutes ahead.

**Request:**

```json
{
  "current_time": "2024-01-07T14:30:00",
  "current_load": 750,
  "historical_loads": [650, 700, 720, 745]
}
```

**Response:**

```json
{
  "predicted_load": 820.45,
  "confidence_lower": 780.23,
  "confidence_upper": 860.67,
  "prediction_horizon": "30_minutes",
  "model_accuracy": 0.8912,
  "timestamp": "2024-01-07T14:30:05.123456"
}
```

**cURL Example:**

```bash
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "current_time": "2024-01-07T14:30:00",
    "current_load": 750,
    "historical_loads": [650, 700, 720, 745]
  }'
```

### GET /health

Check API health status.

**Response:**

```json
{
  "status": "healthy",
  "model_loaded": true,
  "scaler_loaded": true,
  "timestamp": "2024-01-07T14:30:00.123456"
}
```

### GET /metrics

Get prediction statistics.

**Response:**

```json
{
  "predictions_served": 1523,
  "avg_error": 45.2,
  "model_version": "20240107_143000",
  "model_rmse": 45.23,
  "model_r2": 0.8912,
  "timestamp": "2024-01-07T14:30:00.123456"
}
```

### POST /record_actual

Record actual load for accuracy tracking (optional).

**Request:**

```json
{
  "predicted_load": 820,
  "actual_load": 805
}
```

**Response:**

```json
{
  "status": "recorded",
  "error": 15.0
}
```

## Testing

### Quick Test

```bash
# Terminal 1: Start API
python app.py

# Terminal 2: Test health
curl http://localhost:5000/health

# Terminal 2: Test prediction
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{"current_time": "2024-01-07T14:30:00", "current_load": 750, "historical_loads": [650, 700, 720, 745]}'
```

## Troubleshooting

### Error: "Model not found"

**Solution:** Run `python train_model.py` first to train the model.

### Error: "Training data not found"

**Solution:** Run `python generate_data.py` first to generate training data.

### Error: "ModuleNotFoundError"

**Solution:** Install dependencies with `pip install -r requirements.txt`

### Port 5000 already in use

**Solution:** Change port in `app.py` (line: `app.run(port=5000)`) or kill the process using port 5000.

## File Structure

```
ml_predictor_service/
├── requirements.txt          # Python dependencies
├── generate_data.py          # Synthetic data generator
├── train_model.py            # Model training script
├── app.py                    # Flask REST API
├── README.md                 # This file
├── training_data.csv         # Generated training data
├── model.pkl                 # Trained model
├── scaler.pkl                # Feature scaler
├── model_metadata.json       # Performance metrics
└── model_YYYYMMDD_HHMMSS.pkl # Versioned model
```

## Next Steps

After starting the Flask API:

1. **Java Integration**: Implement `LoadPredictionService.java` to consume predictions
2. **Dashboard**: Create JavaFX visualization showing predicted vs actual load
3. **Scaling**: Integrate with `ScalingService.java` for proactive scaling

## Model Retraining

To retrain the model with new data:

```bash
# Generate fresh data
python generate_data.py

# Retrain model
python train_model.py

# Restart API (model auto-reloads)
python app.py
```

## Production Deployment

For production use:

1. Use a production WSGI server (e.g., Gunicorn):

   ```bash
   pip install gunicorn
   gunicorn -w 4 -b 0.0.0.0:5000 app:app
   ```

2. Set up model monitoring and retraining pipeline

3. Implement authentication for API endpoints

4. Use environment variables for configuration

## Support

For issues or questions, refer to the main CloudFileSystem documentation.
