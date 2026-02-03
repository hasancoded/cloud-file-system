"""
Flask REST API for ML Load Prediction
======================================
Serves predictions from trained Random Forest model

Endpoints:
- POST /predict: Get load prediction
- GET /health: Check API health
- GET /metrics: Get prediction metrics

Author: CloudFileSystem ML Team
Portfolio: Production-ready ML API service
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
import os
import json

app = Flask(__name__)
CORS(app)  # Enable CORS for Java client

# Global variables for model and metrics
model = None
scaler = None
model_metadata = None
prediction_count = 0
prediction_errors = []

def load_model():
    """Load trained model and scaler on startup"""
    global model, scaler, model_metadata
    
    print("[ML API] Loading model artifacts...")
    
    try:
        # Load model
        if not os.path.exists('model.pkl'):
            raise FileNotFoundError(
                "Model not found. Please run 'python train_model.py' first."
            )
        
        model = joblib.load('model.pkl')
        print("[ML API] ✓ Model loaded successfully")
        
        # Load scaler
        if not os.path.exists('scaler.pkl'):
            raise FileNotFoundError(
                "Scaler not found. Please run 'python train_model.py' first."
            )
        
        scaler = joblib.load('scaler.pkl')
        print("[ML API] ✓ Scaler loaded successfully")
        
        # Load metadata
        if os.path.exists('model_metadata.json'):
            with open('model_metadata.json', 'r') as f:
                model_metadata = json.load(f)
            print(f"[ML API] ✓ Model metadata loaded (RMSE: {model_metadata['metrics']['rmse']:.2f})")
        else:
            model_metadata = {'metrics': {'rmse': 0, 'mae': 0, 'r2': 0}}
            print("[ML API] ⚠ Model metadata not found")
        
        return True
    
    except Exception as e:
        print(f"[ML API] ✗ Error loading model: {e}")
        return False

def extract_features(current_time, current_load, historical_loads):
    """
    Extract features from request data
    
    Args:
        current_time: ISO format timestamp string
        current_load: Current load value
        historical_loads: List of historical load values (last 4 hours)
    
    Returns:
        Feature array ready for prediction
    """
    # Parse timestamp
    dt = datetime.fromisoformat(current_time.replace('Z', '+00:00'))
    
    # Extract time features
    hour_of_day = dt.hour
    day_of_week = dt.weekday()
    month = dt.month
    is_weekend = 1 if day_of_week >= 5 else 0
    is_business_hours = 1 if 9 <= hour_of_day < 17 else 0
    
    # Historical features
    load_1h_ago = historical_loads[-1] if len(historical_loads) >= 1 else current_load
    load_24h_ago = current_load  # Approximation (we don't have 24h history in request)
    avg_load_last_7d = np.mean(historical_loads) if historical_loads else current_load
    
    # Create feature array (must match training order)
    features = [
        hour_of_day,
        day_of_week,
        month,
        is_weekend,
        is_business_hours,
        load_1h_ago,
        load_24h_ago,
        avg_load_last_7d
    ]
    
    return np.array(features).reshape(1, -1)

def calculate_confidence_interval(prediction, confidence=0.95):
    """
    Calculate confidence interval for prediction
    
    Uses model RMSE to estimate prediction uncertainty
    
    Args:
        prediction: Predicted load value
        confidence: Confidence level (default 95%)
    
    Returns:
        Tuple of (lower_bound, upper_bound)
    """
    # Use RMSE as standard error estimate
    rmse = model_metadata['metrics']['rmse'] if model_metadata else 50
    
    # For 95% confidence, use ~2 standard deviations
    margin = 2 * rmse if confidence >= 0.95 else 1.5 * rmse
    
    lower = max(0, prediction - margin)  # Load can't be negative
    upper = prediction + margin
    
    return lower, upper

@app.route('/health', methods=['GET'])
def health_check():
    """
    Health check endpoint
    
    Returns:
        JSON with API status and model loaded status
    """
    return jsonify({
        'status': 'healthy' if model is not None else 'unhealthy',
        'model_loaded': model is not None,
        'scaler_loaded': scaler is not None,
        'timestamp': datetime.now().isoformat()
    })

@app.route('/metrics', methods=['GET'])
def get_metrics():
    """
    Get prediction metrics
    
    Returns:
        JSON with prediction statistics
    """
    global prediction_count, prediction_errors
    
    # Calculate average error
    avg_error = np.mean(prediction_errors) if prediction_errors else 0
    
    return jsonify({
        'predictions_served': prediction_count,
        'avg_error': round(avg_error, 2),
        'model_version': model_metadata.get('timestamp', 'v1.0') if model_metadata else 'v1.0',
        'model_rmse': model_metadata['metrics']['rmse'] if model_metadata else 0,
        'model_r2': model_metadata['metrics']['r2'] if model_metadata else 0,
        'timestamp': datetime.now().isoformat()
    })

@app.route('/predict', methods=['POST'])
def predict():
    """
    Predict load 30 minutes ahead
    
    Request JSON:
    {
        "current_time": "2024-01-07T14:30:00",
        "current_load": 750,
        "historical_loads": [650, 700, 720, 745]
    }
    
    Response JSON:
    {
        "predicted_load": 820,
        "confidence_lower": 780,
        "confidence_upper": 860,
        "prediction_horizon": "30_minutes",
        "model_accuracy": 0.89
    }
    """
    global prediction_count
    
    try:
        # Validate model is loaded
        if model is None or scaler is None:
            return jsonify({
                'error': 'Model not loaded',
                'message': 'Please run train_model.py first'
            }), 500
        
        # Parse request
        data = request.get_json()
        
        # Validate required fields
        required_fields = ['current_time', 'current_load', 'historical_loads']
        for field in required_fields:
            if field not in data:
                return jsonify({
                    'error': f'Missing required field: {field}'
                }), 400
        
        # Extract features
        features = extract_features(
            data['current_time'],
            data['current_load'],
            data['historical_loads']
        )
        
        # Scale features
        features_scaled = scaler.transform(features)
        
        # Make prediction
        predicted_load = model.predict(features_scaled)[0]
        
        # Calculate confidence interval
        confidence_lower, confidence_upper = calculate_confidence_interval(predicted_load)
        
        # Increment prediction count
        prediction_count += 1
        
        # Log prediction
        print(f"[ML API] Prediction #{prediction_count}: {predicted_load:.0f} req/hr "
              f"(current: {data['current_load']:.0f})")
        
        # Return prediction
        response = {
            'predicted_load': round(predicted_load, 2),
            'confidence_lower': round(confidence_lower, 2),
            'confidence_upper': round(confidence_upper, 2),
            'prediction_horizon': '30_minutes',
            'model_accuracy': round(model_metadata['metrics']['r2'], 4) if model_metadata else 0.89,
            'timestamp': datetime.now().isoformat()
        }
        
        return jsonify(response)
    
    except Exception as e:
        print(f"[ML API] ✗ Prediction error: {e}")
        return jsonify({
            'error': 'Prediction failed',
            'message': str(e)
        }), 500

@app.route('/record_actual', methods=['POST'])
def record_actual():
    """
    Record actual load for accuracy tracking
    
    Request JSON:
    {
        "predicted_load": 820,
        "actual_load": 805
    }
    """
    global prediction_errors
    
    try:
        data = request.get_json()
        
        predicted = data.get('predicted_load')
        actual = data.get('actual_load')
        
        if predicted is None or actual is None:
            return jsonify({'error': 'Missing predicted_load or actual_load'}), 400
        
        # Calculate error
        error = abs(predicted - actual)
        prediction_errors.append(error)
        
        # Keep only last 1000 errors
        if len(prediction_errors) > 1000:
            prediction_errors = prediction_errors[-1000:]
        
        print(f"[ML API] Recorded prediction error: {error:.2f} req/hr")
        
        return jsonify({
            'status': 'recorded',
            'error': round(error, 2)
        })
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print("=" * 60)
    print("CloudFileSystem - ML Load Prediction API")
    print("=" * 60)
    print()
    
    # Load model on startup
    if load_model():
        print("\n[ML API] Starting Flask server on http://localhost:5000")
        print("[ML API] Endpoints:")
        print("  - POST /predict       : Get load prediction")
        print("  - GET  /health        : Check API health")
        print("  - GET  /metrics       : Get prediction metrics")
        print("  - POST /record_actual : Record actual load for tracking")
        print()
        
        app.run(host='0.0.0.0', port=5000, debug=True)
    else:
        print("\n[ML API] ✗ Failed to load model. Exiting.")
        print("Please run 'python train_model.py' first to train the model.")
