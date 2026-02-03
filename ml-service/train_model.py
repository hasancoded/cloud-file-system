"""
ML Model Training Script for Load Prediction
=============================================
Trains a Random Forest Regressor to predict load 30 minutes ahead
Uses features: hour, day_of_week, historical load patterns

Model Performance Target:
- RMSE < 100 requests/hour
- R² > 0.85

Author: CloudFileSystem ML Team
Portfolio: Production-ready ML model training
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
import joblib
from datetime import datetime
import os

def load_training_data(filename='training_data.csv'):
    """Load training data from CSV"""
    print(f"[Model Training] Loading data from {filename}...")
    
    if not os.path.exists(filename):
        raise FileNotFoundError(
            f"Training data not found: {filename}\n"
            "Please run 'python generate_data.py' first to generate training data."
        )
    
    df = pd.read_csv(filename)
    print(f"[Model Training] ✓ Loaded {len(df)} data points")
    
    return df

def prepare_features(df):
    """
    Prepare features and target for model training
    
    Features:
    - hour_of_day (0-23)
    - day_of_week (0-6)
    - month (1-12)
    - is_weekend (0/1)
    - is_business_hours (0/1)
    - load_1h_ago
    - load_24h_ago
    - avg_load_last_7d
    
    Target:
    - current_load (we'll shift this to predict 30min ahead)
    """
    print("[Model Training] Preparing features...")
    
    # Feature columns
    feature_cols = [
        'hour_of_day',
        'day_of_week',
        'month',
        'is_weekend',
        'is_business_hours',
        'load_1h_ago',
        'load_24h_ago',
        'avg_load_last_7d'
    ]
    
    # Create target: load 30 minutes ahead
    # Since we have hourly data, we'll predict 1 hour ahead as approximation
    df['target_load'] = df['current_load'].shift(-1)
    
    # Drop rows with NaN in target
    df = df.dropna(subset=['target_load'])
    
    X = df[feature_cols]
    y = df['target_load']
    
    print(f"[Model Training] ✓ Features shape: {X.shape}")
    print(f"[Model Training] ✓ Target shape: {y.shape}")
    
    return X, y, feature_cols

def train_model(X_train, y_train, n_estimators=100, max_depth=20, random_state=42):
    """
    Train Random Forest Regressor
    
    Args:
        X_train: Training features
        y_train: Training target
        n_estimators: Number of trees in forest
        max_depth: Maximum depth of trees
        random_state: Random seed for reproducibility
    
    Returns:
        Trained model
    """
    print(f"\n[Model Training] Training Random Forest...")
    print(f"  - Estimators: {n_estimators}")
    print(f"  - Max depth: {max_depth}")
    print(f"  - Random state: {random_state}")
    
    model = RandomForestRegressor(
        n_estimators=n_estimators,
        max_depth=max_depth,
        random_state=random_state,
        n_jobs=-1,  # Use all CPU cores
        verbose=1
    )
    
    model.fit(X_train, y_train)
    
    print("[Model Training] ✓ Model training complete")
    
    return model

def evaluate_model(model, X_test, y_test, feature_cols):
    """
    Evaluate model performance
    
    Args:
        model: Trained model
        X_test: Test features
        y_test: Test target
        feature_cols: List of feature names
    
    Returns:
        Dictionary with evaluation metrics
    """
    print("\n[Model Training] Evaluating model performance...")
    
    # Make predictions
    y_pred = model.predict(X_test)
    
    # Calculate metrics
    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)
    
    # Calculate percentage error
    mape = np.mean(np.abs((y_test - y_pred) / y_test)) * 100
    
    metrics = {
        'rmse': rmse,
        'mae': mae,
        'r2': r2,
        'mape': mape
    }
    
    print("\n" + "=" * 60)
    print("MODEL PERFORMANCE METRICS")
    print("=" * 60)
    print(f"RMSE (Root Mean Squared Error): {rmse:.2f} req/hr")
    print(f"MAE (Mean Absolute Error):      {mae:.2f} req/hr")
    print(f"R² Score:                       {r2:.4f}")
    print(f"MAPE (Mean Absolute % Error):   {mape:.2f}%")
    print("=" * 60)
    
    # Check if meets target
    if rmse < 100:
        print("✓ RMSE target met (< 100 req/hr)")
    else:
        print("⚠ RMSE target not met (target: < 100 req/hr)")
    
    if r2 > 0.85:
        print("✓ R² target met (> 0.85)")
    else:
        print("⚠ R² target not met (target: > 0.85)")
    
    # Feature importance
    print("\n" + "=" * 60)
    print("FEATURE IMPORTANCE")
    print("=" * 60)
    
    feature_importance = pd.DataFrame({
        'feature': feature_cols,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)
    
    for idx, row in feature_importance.iterrows():
        print(f"{row['feature']:25s} {row['importance']:.4f} {'█' * int(row['importance'] * 100)}")
    
    print("=" * 60)
    
    return metrics, feature_importance

def save_model(model, scaler, metrics, feature_importance):
    """
    Save trained model and metadata
    
    Args:
        model: Trained model
        scaler: Fitted scaler
        metrics: Performance metrics
        feature_importance: Feature importance DataFrame
    """
    print("\n[Model Training] Saving model artifacts...")
    
    # Create timestamp for versioning
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    # Save model
    model_filename = 'model.pkl'
    joblib.dump(model, model_filename)
    print(f"[Model Training] ✓ Model saved to {model_filename}")
    
    # Save scaler
    scaler_filename = 'scaler.pkl'
    joblib.dump(scaler, scaler_filename)
    print(f"[Model Training] ✓ Scaler saved to {scaler_filename}")
    
    # Save versioned copy
    versioned_model = f'model_{timestamp}.pkl'
    joblib.dump(model, versioned_model)
    print(f"[Model Training] ✓ Versioned model saved to {versioned_model}")
    
    # Save metadata
    metadata = {
        'timestamp': timestamp,
        'metrics': metrics,
        'feature_importance': feature_importance.to_dict('records')
    }
    
    import json
    with open('model_metadata.json', 'w') as f:
        # Convert numpy types to Python types for JSON serialization
        metadata_json = {
            'timestamp': timestamp,
            'metrics': {k: float(v) for k, v in metrics.items()},
            'feature_importance': [
                {'feature': row['feature'], 'importance': float(row['importance'])}
                for _, row in feature_importance.iterrows()
            ]
        }
        json.dump(metadata_json, f, indent=2)
    
    print(f"[Model Training] ✓ Metadata saved to model_metadata.json")

def main():
    """Main training pipeline"""
    print("=" * 60)
    print("CloudFileSystem - ML Load Prediction")
    print("Model Training Pipeline")
    print("=" * 60)
    print()
    
    # 1. Load data
    df = load_training_data('training_data.csv')
    
    # 2. Prepare features
    X, y, feature_cols = prepare_features(df)
    
    # 3. Train/test split (80/20)
    print("\n[Model Training] Splitting data (80% train, 20% test)...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, shuffle=False  # Don't shuffle time series
    )
    print(f"[Model Training] ✓ Train size: {len(X_train)}")
    print(f"[Model Training] ✓ Test size: {len(X_test)}")
    
    # 4. Scale features
    print("\n[Model Training] Scaling features...")
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    print("[Model Training] ✓ Feature scaling complete")
    
    # 5. Train model
    model = train_model(X_train_scaled, y_train)
    
    # 6. Evaluate model
    metrics, feature_importance = evaluate_model(
        model, X_test_scaled, y_test, feature_cols
    )
    
    # 7. Save model
    save_model(model, scaler, metrics, feature_importance)
    
    print("\n" + "=" * 60)
    print("✓ Model training complete!")
    print("Next step: Run 'python app.py' to start the Flask API")
    print("=" * 60)

if __name__ == "__main__":
    main()
