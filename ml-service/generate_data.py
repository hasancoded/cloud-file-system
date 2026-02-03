"""
Synthetic Training Data Generator for Load Prediction
======================================================
Generates 6 months of realistic file operation load data with:
- Business hours peaks (9am-5pm): 500-1000 req/hr
- Lunch hour spike (12pm-1pm): 1200 req/hr
- Night baseline: 50-100 req/hr
- Weekend reduction: 30% of weekday traffic
- Monthly growth: 5% compound increase
- Random viral spikes: 5x normal load
- Noise: ±20% variation

Author: CloudFileSystem ML Team
Portfolio: Interview-ready synthetic data generation
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import random

# Set random seed for reproducibility
np.random.seed(42)
random.seed(42)

def generate_base_load(hour, day_of_week, is_weekend, is_business_hours):
    """
    Generate base load based on time patterns
    
    Args:
        hour: Hour of day (0-23)
        day_of_week: Day of week (0=Monday, 6=Sunday)
        is_weekend: Boolean indicating weekend
        is_business_hours: Boolean indicating 9am-5pm
    
    Returns:
        Base load in requests/hour
    """
    # Night baseline (10pm - 6am)
    if hour < 6 or hour >= 22:
        base = np.random.uniform(50, 100)
    
    # Morning ramp-up (6am - 9am)
    elif hour < 9:
        base = np.random.uniform(200, 400)
    
    # Business hours (9am - 5pm)
    elif is_business_hours:
        # Lunch peak (12pm - 1pm)
        if hour == 12:
            base = np.random.uniform(1100, 1300)
        else:
            base = np.random.uniform(500, 1000)
    
    # Evening decline (5pm - 10pm)
    else:
        base = np.random.uniform(150, 350)
    
    # Weekend reduction (30% of weekday traffic)
    if is_weekend:
        base *= 0.3
    
    return base

def add_noise(value, noise_percent=20):
    """Add random noise to simulate real-world variation"""
    noise = np.random.uniform(-noise_percent, noise_percent) / 100
    return value * (1 + noise)

def should_spike(probability=0.02):
    """Randomly determine if a viral spike should occur"""
    return random.random() < probability

def generate_training_data(months=6, output_file='training_data.csv'):
    """
    Generate synthetic training data
    
    Args:
        months: Number of months of data to generate
        output_file: Output CSV filename
    
    Returns:
        DataFrame with training data
    """
    print(f"[Data Generator] Generating {months} months of synthetic data...")
    
    # Start date: 6 months ago from now
    start_date = datetime.now() - timedelta(days=30 * months)
    
    # Generate hourly data points
    total_hours = 24 * 30 * months
    data = []
    
    current_date = start_date
    month_counter = 0
    
    for i in range(total_hours):
        # Extract time features
        hour = current_date.hour
        day_of_week = current_date.weekday()  # 0=Monday, 6=Sunday
        month = current_date.month
        is_weekend = day_of_week >= 5  # Saturday=5, Sunday=6
        is_business_hours = 9 <= hour < 17
        
        # Calculate monthly growth (5% per month)
        months_elapsed = (current_date - start_date).days / 30
        growth_factor = 1 + (0.05 * months_elapsed)
        
        # Generate base load
        base_load = generate_base_load(hour, day_of_week, is_weekend, is_business_hours)
        
        # Apply monthly growth
        load = base_load * growth_factor
        
        # Add noise
        load = add_noise(load, noise_percent=20)
        
        # Random viral spikes (2% chance per hour)
        if should_spike(probability=0.02):
            load *= random.uniform(3, 5)
            print(f"[Data Generator] Viral spike at {current_date}: {load:.0f} req/hr")
        
        # Ensure non-negative
        load = max(0, load)
        
        # Store data point
        data.append({
            'timestamp': current_date,
            'hour_of_day': hour,
            'day_of_week': day_of_week,
            'month': month,
            'is_weekend': int(is_weekend),
            'is_business_hours': int(is_business_hours),
            'current_load': round(load, 2)
        })
        
        # Move to next hour
        current_date += timedelta(hours=1)
    
    # Create DataFrame
    df = pd.DataFrame(data)
    
    # Add lagged features (historical load)
    print("[Data Generator] Creating lagged features...")
    df['load_1h_ago'] = df['current_load'].shift(1)
    df['load_24h_ago'] = df['current_load'].shift(24)
    
    # Calculate 7-day rolling average
    df['avg_load_last_7d'] = df['current_load'].shift(1).rolling(window=24*7, min_periods=1).mean()
    
    # Drop rows with NaN values (first few rows due to lagging)
    df = df.dropna()
    
    # Save to CSV
    df.to_csv(output_file, index=False)
    print(f"[Data Generator] ✓ Generated {len(df)} data points")
    print(f"[Data Generator] ✓ Saved to {output_file}")
    
    # Print statistics
    print("\n=== Data Statistics ===")
    print(f"Date range: {df['timestamp'].min()} to {df['timestamp'].max()}")
    print(f"Load range: {df['current_load'].min():.0f} - {df['current_load'].max():.0f} req/hr")
    print(f"Average load: {df['current_load'].mean():.0f} req/hr")
    print(f"Std deviation: {df['current_load'].std():.0f} req/hr")
    
    # Business hours vs non-business hours
    business_avg = df[df['is_business_hours'] == 1]['current_load'].mean()
    non_business_avg = df[df['is_business_hours'] == 0]['current_load'].mean()
    print(f"\nBusiness hours avg: {business_avg:.0f} req/hr")
    print(f"Non-business hours avg: {non_business_avg:.0f} req/hr")
    print(f"Peak ratio: {business_avg / non_business_avg:.2f}x")
    
    # Weekend vs weekday
    weekend_avg = df[df['is_weekend'] == 1]['current_load'].mean()
    weekday_avg = df[df['is_weekend'] == 0]['current_load'].mean()
    print(f"\nWeekday avg: {weekday_avg:.0f} req/hr")
    print(f"Weekend avg: {weekend_avg:.0f} req/hr")
    print(f"Weekend reduction: {(1 - weekend_avg / weekday_avg) * 100:.1f}%")
    
    return df

def visualize_data(df, output_file='data_visualization.png'):
    """
    Create visualization of generated data
    
    Args:
        df: DataFrame with training data
        output_file: Output image filename
    """
    import matplotlib.pyplot as plt
    
    print("\n[Data Generator] Creating visualization...")
    
    # Sample 7 days for visualization (too many points otherwise)
    sample_df = df.head(24 * 7)
    
    plt.figure(figsize=(15, 6))
    plt.plot(sample_df['timestamp'], sample_df['current_load'], linewidth=1, alpha=0.8)
    plt.title('Sample: 7 Days of Synthetic Load Data', fontsize=14, fontweight='bold')
    plt.xlabel('Time', fontsize=12)
    plt.ylabel('Load (requests/hour)', fontsize=12)
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(output_file, dpi=150)
    print(f"[Data Generator] ✓ Visualization saved to {output_file}")

if __name__ == "__main__":
    print("=" * 60)
    print("CloudFileSystem - ML Load Prediction")
    print("Synthetic Training Data Generator")
    print("=" * 60)
    print()
    
    # Generate data
    df = generate_training_data(months=6, output_file='training_data.csv')
    
    # Create visualization
    try:
        visualize_data(df, output_file='data_visualization.png')
    except Exception as e:
        print(f"[Data Generator] Warning: Could not create visualization: {e}")
    
    print("\n" + "=" * 60)
    print("✓ Data generation complete!")
    print("Next step: Run 'python train_model.py' to train the model")
    print("=" * 60)
