# CloudFileSystem

CloudFileSystem is a robust, resilient cloud storage simulation platform designed to demonstrate advanced distributed systems concepts. It features a Java Spring Boot backend, a React frontend, and a Python-based Machine Learning service for predictive scaling.

## 🚀 Key Features

- **Distributed File System**: Simulates file storage across multiple nodes with replication and fault tolerance.
- **Predictive Scaling**: Uses an ML model to predict system load and auto-scale resources (simulated).
- **Interactive Dashboard**: A modern React-based UI for managing files, viewing system metrics, and monitoring node status.
- **Role-Based Access Control**: Secure login and permission management using JWT.
- **DevOps Ready**: Includes Docker Compose and Jenkins configurations for CI/CD pipelines.

## 📂 Project Structure

The repository is organized into the following modules:

- **[`backend`](./backend)**: Java Spring Boot application handling core logic, API, and file management.
- **[`frontend`](./frontend)**: React + Vite application providing the user interface.
- **[`ml-service`](./ml-service)**: Python Flask service for load prediction and data analysis.
- **[`ops`](./ops)**: Infrastructure as Code (IaC), Docker Compose files, and Jenkins pipelines.
- **[`docs`](./docs)**: Detailed documentation, setup guides, and architectural decisions.

## 🛠️ Technology Stack

- **Backend**: Java 17, Spring Boot 3.2, JavaFX (for local GUI components), SQLite/MySQL.
- **Frontend**: React, TypeScript, Vite, Tailwind CSS.
- **ML & Data**: Python, Flask, Scikit-learn, Pandas.
- **Tools**: Maven, NPM, Docker, Jenkins.

## 🏁 Getting Started

### Prerequisites

- Java JDK 17+
- Node.js 18+
- Python 3.9+
- Docker & Docker Compose (optional)

### Quick Start

1.  **Backend Setup**:

    ```bash
    cd backend
    ./mvnw clean install
    java -jar target/CloudFileSystem-1.0-SNAPSHOT.jar
    ```

2.  **Frontend Setup**:

    ```bash
    cd frontend
    npm install
    npm run dev
    ```

3.  **ML Service Setup**:

    ```bash
    cd ml-service
    python -m venv venv
    source venv/bin/activate  # or venv\Scripts\activate on Windows
    pip install -r requirements.txt

    # Generate data and train model (Required first time)
    python generate_data.py
    python train_model.py

    python app.py
    ```

For detailed installation instructions, please refer to the [Complete Setup Guide](./docs/COMPLETE_SETUP_GUIDE.md).

## 📖 Documentation

- [Setup Guide](./docs/COMPLETE_SETUP_GUIDE.md)
- [ML System Setup](./docs/ML_SETUP.md)
- [Future Enhancements](./docs/FUTURE_ENHANCEMENTS.md)
- [Implementation Report](./docs/IMPLEMENTATION_REPORT.md)

## 🏗️ Development

### Building the Project

- **Backend**: `cd backend && mvn clean package`
- **Frontend**: `cd frontend && npm run build`

### Running Tests

- **Backend**: `cd backend && mvn test`
- **Frontend**: `cd frontend && npm test` (if configured)

## 🤝 Contribution

1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/amazing-feature`).
3.  Commit your changes (`git commit -m 'Add some amazing feature'`).
4.  Push to the branch (`git push origin feature/amazing-feature`).
5.  Open a Pull Request.

## 📄 License

[MIT License](LICENSE) (Placeholder)
