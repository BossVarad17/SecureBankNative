# SecureBank Mobile - Native Android App (Java)

SecureBank Mobile is a native Android application built with Java that provides a secure and intuitive interface for the SecureBank platform. It connects to the SecureBank Flask REST API to enable real-time banking operations on mobile devices.

## 📱 Features

* **Secure Authentication:** Session-based login with persistent authentication.
* **Consolidated Dashboard:** A high-level view of all linked accounts and total balances.
* **ACID-Compliant Transfers:** Perform secure fund transfers between accounts with real-time validation.
* **Digital Statements:** View paginated transaction history and account details using efficient cursor-based data fetching.
* **Account Management:** Detailed view of account types, IFSC codes, and branch information.

## 🛠️ Tech Stack

* **Language:** Java
* **Environment:** Android Studio
* **Networking:** Native Android Networking (or Retrofit/Volley) for REST API communication
* **UI Components:** Material Design for a modern banking aesthetic
* **Data Format:** JSON (via Backend REST API)

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest version recommended)
* Android SDK 33+
* A running instance of the [SecureBank](https://securebank-ssp4.onrender.com/) (Render)

### Installation & Setup

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/PraharshAgarwal/SecureBank-Android.git
    ```

2.  **Configure API Base URL**
    Navigate to your network configuration file (e.g., `Constants.java` or `ApiClient.java`) and update the `BASE_URL` to point to your live Render backend or local IP:
    ```java
    // Example: Replace with your Render URL
    public static final String BASE_URL = "https://your-securebank-backend.onrender.com/api/";
    ```

3.  **Build the Project**
    * Open the project in Android Studio.
    * Sync project with Gradle files.
    * Click **Run** (Green play button) to install the app on your emulator or physical device.

## 🔌 API Integration

This application communicates with the following SecureBank REST API endpoints:
* `POST /api/login`: Secure user authentication.
* `GET /api/dashboard`: Fetches user accounts and balances.
* `POST /api/transfer`: Executes fund transfers.
* `GET /api/statement/<id>`: Retrieves paginated transaction history.

## 🔐 Security Note

This app uses secure session management. When connecting to a backend hosted on Render or any HTTPS server, all data transmitted between the mobile app and the database is encrypted via SSL/TLS.

## 📝 Credentials for Testing

Use the following demo credentials to explore the application:
* **Email:** `john.doe@email.com`
* **Password:** `SecureBank@123`

## 👨‍💻 Author

**Praharsh Agarwal**
* Full-Stack & Mobile Developer
* [GitHub Profile](https://github.com/PraharshAgarwal)
