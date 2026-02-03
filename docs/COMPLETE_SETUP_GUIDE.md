# CloudFileSystem - Complete Setup Guide

## From Scratch on New Computer/Environment

**Version:** 2.0  
**Last Updated:** January 2026  
**Estimated Setup Time:** 45-60 minutes

---

## 🎯 PART 1: PREREQUISITES INSTALLATION

### Step 1.1: Install Java JDK 17

**Download:**

1. Go to: https://adoptium.net/temurin/releases/
2. Select:
   - Operating System: Windows
   - Architecture: x64
   - Package Type: JDK
   - Version: 17 (LTS)
3. Download `.msi` installer

**Install:**

1. Run the downloaded `.msi` file
2. Click "Next" through the wizard
3. ✅ **IMPORTANT:** Check "Set JAVA_HOME variable"
4. ✅ **IMPORTANT:** Check "Add to PATH"
5. Complete installation
6. Click "Finish"

**Verify:**

```cmd
java -version
```

Expected output: `openjdk version "17.x.x"`

---

### Step 1.2: Install Apache Maven

**Download:**

1. Go to: https://maven.apache.org/download.cgi
2. Download: `apache-maven-x.x.x-bin.zip` (Binary zip archive)

**Install:**

1. Extract ZIP to: `C:\Maven`
   - Final path should be: `C:\Maven\bin\mvn.cmd`
2. Add to System PATH:
   - Press `Windows Key`
   - Type: "Environment Variables"
   - Click: "Edit the system environment variables"
   - Click: "Environment Variables" button
   - Under "System variables", find "Path"
   - Click "Edit"
   - Click "New"
   - Add: `C:\Maven\bin`
   - Click "OK" on all windows
3. **RESTART** Command Prompt (or computer)

**Verify:**

```cmd
mvn -version
```

Expected output: `Apache Maven x.x.x`

---

### Step 1.3: Install Docker Desktop

**Download:**

1. Go to: https://www.docker.com/products/docker-desktop
2. Download: Docker Desktop for Windows

**Install:**

1. Run installer
2. Follow installation wizard
3. **Enable WSL 2** if prompted
4. Restart computer when prompted

**Verify:**

1. Start Docker Desktop from Start Menu
2. Wait for "Docker Desktop is running" message
3. Open Command Prompt:

```cmd
docker --version
docker ps
```

Expected: Version info and empty container list

---

### Step 1.4: Install NetBeans (Optional but Recommended)

**Download:**

1. Go to: https://netbeans.apache.org/download/
2. Download latest version (17 or higher)

**Install:**

1. Run installer
2. Select JDK 17 as Java Platform
3. Complete installation

---

## 🐳 PART 2: DOCKER CONTAINER SETUP

### Step 2.1: Create MySQL Container

Open Command Prompt as Administrator:

```cmd
docker run -d ^
  --name soft40051-mysql ^
  -e MYSQL_ROOT_PASSWORD=root ^
  -e MYSQL_DATABASE=cloudfs ^
  -p 3306:3306 ^
  mysql:8
```

**Wait 30-60 seconds** for MySQL to start.

**Verify:**

```cmd
docker ps | findstr mysql
```

Should show container running.

---

### Step 2.2: Create File Server Container

```cmd
docker run -d ^
  --name soft40051-file-server ^
  ubuntu ^
  tail -f /dev/null
```

**Create files directory:**

```cmd
docker exec soft40051-file-server mkdir -p /files
```

**Verify:**

```cmd
docker ps | findstr file-server
```

---

### Step 2.3: Initialize MySQL Database

**Connect to MySQL:**

```cmd
docker exec -it soft40051-mysql mysql -uroot -proot
```

You'll see: `mysql>`

**Copy and paste each command, press Enter after each:**

```sql
USE cloudfs;

CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50),
  password VARCHAR(255),
  role VARCHAR(10)
);

CREATE TABLE files (
  id INT AUTO_INCREMENT PRIMARY KEY,
  filename VARCHAR(255),
  owner VARCHAR(50),
  version INT DEFAULT 1,
  last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  content_hash VARCHAR(64)
);

CREATE TABLE file_permissions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  file_id INT,
  shared_with VARCHAR(50),
  permission VARCHAR(10),
  FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE
);

CREATE TABLE event_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50),
  event_type VARCHAR(50),
  description TEXT,
  timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sessions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  token VARCHAR(255) NOT NULL,
  timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_username (username),
  INDEX idx_timestamp (timestamp)
);

CREATE TABLE file_chunks (
  id INT AUTO_INCREMENT PRIMARY KEY,
  file_id INT NOT NULL,
  filename VARCHAR(255) NOT NULL,
  chunk_index INT NOT NULL,
  chunk_size INT NOT NULL,
  crc32_checksum BIGINT NOT NULL,
  storage_container VARCHAR(100),
  chunk_path VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
  UNIQUE KEY unique_chunk (file_id, chunk_index),
  INDEX idx_filename (filename)
);

CREATE TABLE file_history (
  id INT AUTO_INCREMENT PRIMARY KEY,
  file_id INT NOT NULL,
  filename VARCHAR(255) NOT NULL,
  version INT NOT NULL,
  modified_by VARCHAR(50),
  content_hash VARCHAR(64),
  archived_content MEDIUMTEXT,
  archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
  INDEX idx_file_version (file_id, version)
);
```

**Exit MySQL:**

```sql
exit
```

**Verify tables created:**

```cmd
docker exec soft40051-mysql mysql -uroot -proot -e "USE cloudfs; SHOW TABLES;"
```

Should show all 8 tables.

---

## 📁 PART 3: PROJECT SETUP

### Step 3.1: Get Project Files

**Option A: Clone from Repository (if available)**

```cmd
cd D:\NetBeansProjects
git clone [YOUR_REPO_URL] CloudFileSystem
cd CloudFileSystem
```

**Option B: Create from Scratch**

```cmd
cd D:\NetBeansProjects
mkdir CloudFileSystem
cd CloudFileSystem
```

---

### Step 3.2: Create Directory Structure

**In Command Prompt:**

```cmd
cd D:\NetBeansProjects\CloudFileSystem
mkdir src\main\java\com\soft40051\app\scaling
mkdir src\main\java\com\soft40051\app\security
mkdir src\main\java\com\soft40051\app\terminal
mkdir src\test\java\com\soft40051\app\auth
mkdir src\test\java\com\soft40051\app\database
mkdir src\test\java\com\soft40051\app\loadbalancer
```

---

### Step 3.3: Add All Java Files

**Using NetBeans:**

1. **Open NetBeans**
2. **File → Open Project**
3. Navigate to: `D:\NetBeansProjects\CloudFileSystem`
4. Click "Open Project"

5. **Add Main Source Files:**

   For each NEW file:

   - Right-click on appropriate package
   - New → Java Class
   - Enter class name (without .java)
   - Copy-paste code from provided artifacts

   **NEW Files to Create:**

   - `database/DatabaseSyncService.java`
   - `files/ConflictResolver.java`
   - `files/FilePartitioner.java`
   - `scaling/ScalingService.java`
   - `security/EncryptionUtil.java`
   - `terminal/TerminalService.java`
   - `terminal/RemoteTerminalService.java`

   **Files to REPLACE (copy over existing):**

   - `database/SQLiteCache.java`
   - `database/DB.java`
   - `files/FileService.java`
   - `loadbalancer/LoadBalancer.java`
   - `concurrency/FileLock.java`
   - `gui/MainApp.java`

6. **Add Test Files:**

   **⚠️ CRITICAL: Test files MUST go in `src/test/java`**

   - Right-click "Test Packages"
   - New → Java Package → `com.soft40051.app.auth`
   - Right-click new package → New → Java Class → `AuthServiceTest`
   - Copy-paste test code

   Repeat for:

   - `com.soft40051.app.loadbalancer/LoadBalancerTest.java`
   - `com.soft40051.app.database/DatabaseSyncServiceTest.java`

---

### Step 3.4: Update pom.xml

1. In NetBeans Project Files, double-click `pom.xml`
2. **REPLACE ENTIRE CONTENT** with the updated pom.xml from artifacts
3. Save (Ctrl+S)

**Key additions in pom.xml:**

- JUnit 5 dependencies
- JSch dependency (SSH)
- Maven Surefire Plugin

---

### Step 3.5: Add Configuration Files

**In Windows Explorer:**
Navigate to: `D:\NetBeansProjects\CloudFileSystem`

**Create these files:**

1. **Jenkinsfile** (no extension)

   - Right-click → New → Text Document
   - Rename to "Jenkinsfile" (remove .txt)
   - Open with Notepad
   - Copy-paste Jenkinsfile content
   - Save

2. **docker-compose-jenkins.yml**
3. **docker-compose-gitea.yml**
4. **JENKINS_SETUP.md**
5. **GITEA_SETUP.md**
6. **IMPLEMENTATION_REPORT.md**

---

## 🔨 PART 4: BUILD AND RUN

### Step 4.1: Clean Build

**In NetBeans:**

1. Right-click project → "Clean and Build"

**OR in Command Prompt:**

```cmd
cd D:\NetBeansProjects\CloudFileSystem
mvn clean install
```

**Expected Output:**

```
[INFO] BUILD SUCCESS
[INFO] Total time: ~2 minutes (first build)
```

**If build fails:**

- Check all test files are in `src/test/java` (NOT `src/main/java`)
- Verify pom.xml was updated correctly
- Ensure internet connection (Maven downloads dependencies)

---

### Step 4.2: Run Tests

```cmd
mvn test
```

**Expected Output:**

```
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### Step 4.3: Run Application

**Method 1: NetBeans**

1. Right-click project
2. "Run"

**Method 2: Maven Command**

```cmd
mvn javafx:run
```

**Expected:**

- Application window opens
- Console shows:

```
========================================
  CloudFileSystem v2.0 - Initializing
========================================
[1/8] Initializing databases...
[2/8] Starting database sync service...
...
========================================
  ✓ CloudFileSystem Ready
========================================
```

---

### Step 4.4: First Login

**Default Admin Credentials:**

- Username: `admin`
- Password: `admin`

**Test Steps:**

1. Login with admin/admin
2. Navigate to "My Files"
3. Create a test file
4. Check "System Status" tab
5. Logout

---

## 🚀 PART 5: OPTIONAL - CI/CD SETUP

### Step 5.1: Start Jenkins (Optional)

```cmd
cd D:\NetBeansProjects\CloudFileSystem
docker-compose -f docker-compose-jenkins.yml up -d
```

Wait 2 minutes, then open: http://localhost:8080

**Credentials:** admin / admin123

See `JENKINS_SETUP.md` for full configuration.

---

### Step 5.2: Start Gitea (Optional)

```cmd
cd D:\NetBeansProjects\CloudFileSystem
docker-compose -f docker-compose-gitea.yml up -d
```

Wait 1 minute, then open: http://localhost:3000

See `GITEA_SETUP.md` for full setup.

---

## ✅ PART 6: VERIFICATION CHECKLIST

### 6.1: Docker Containers

```cmd
docker ps
```

Should show:

- ✅ soft40051-mysql (running)
- ✅ soft40051-file-server (running)
- ✅ soft40051-jenkins (optional)
- ✅ soft40051-gitea (optional)

---

### 6.2: Application Features

Test each feature:

- ✅ Login/Logout
- ✅ File Create/Read/Update/Delete
- ✅ File Sharing
- ✅ System Status Dashboard
- ✅ User Management (admin only)

---

### 6.3: Unit Tests

```cmd
mvn test
```

- ✅ All tests pass

---

## 🐛 TROUBLESHOOTING

### Problem: "package org.junit.jupiter.api does not exist"

**Cause:** Test files in wrong location

**Solution:**

1. Check test files are in `src/test/java` (NOT `src/main/java`)
2. In NetBeans, expand "Test Packages"
3. If tests are under "Source Packages", move them:
   - Create folders in Test Packages
   - Copy files to correct location
   - Delete from Source Packages

---

### Problem: "mvn is not recognized"

**Cause:** Maven not in PATH

**Solution:**

1. Close all Command Prompts
2. Re-add Maven to PATH (see Step 1.2)
3. Restart computer
4. Verify: `mvn -version`

---

### Problem: Docker containers won't start

**Cause:** Docker Desktop not running or port conflict

**Solution:**

1. Start Docker Desktop
2. Wait for "Docker is running"
3. Check port 3306 is free:
   ```cmd
   netstat -ano | findstr :3306
   ```
4. If port in use, stop conflicting service

---

### Problem: "Failed to connect to MySQL"

**Cause:** MySQL container not fully started

**Solution:**

1. Wait 30 more seconds
2. Check container logs:
   ```cmd
   docker logs soft40051-mysql
   ```
3. Look for: "ready for connections"
4. Restart container if needed:
   ```cmd
   docker restart soft40051-mysql
   ```

---

### Problem: Tests fail with database errors

**Cause:** MySQL tables not created

**Solution:**

1. Re-run SQL setup (Part 2, Step 2.3)
2. Verify tables:
   ```cmd
   docker exec soft40051-mysql mysql -uroot -proot -e "USE cloudfs; SHOW TABLES;"
   ```

---

### Problem: Application won't start

**Cause:** JavaFX classpath issues

**Solution:**

- Always use `mvn javafx:run` (NOT `java -jar`)
- Never run JAR directly
- NetBeans "Run" button is correct

---

## 📞 SUPPORT & NEXT STEPS

### If Build Still Fails:

1. **Delete Maven cache:**

   ```cmd
   rmdir /s /q %USERPROFILE%\.m2\repository
   mvn clean install
   ```

2. **Check Java version:**

   ```cmd
   java -version
   mvn -version
   ```

   Both should show Java 17

3. **Verify all files present:**
   - Check FILE_LOCATIONS.txt
   - Ensure no missing files

---

### After Successful Setup:

1. ✅ Take snapshot of working environment
2. ✅ Commit to Git (if using Gitea)
3. ✅ Test all features thoroughly
4. ✅ Review IMPLEMENTATION_REPORT.md for feature details
5. ✅ Practice demo presentation

---

## 📊 FINAL VERIFICATION

Run this complete test:

```cmd
cd D:\NetBeansProjects\CloudFileSystem

# 1. Build
mvn clean install

# 2. Test
mvn test

# 3. Run (in separate window)
mvn javafx:run

# 4. Verify containers
docker ps
```

**All should succeed with no errors.**

---

## 🎓 ACADEMIC USE

This project is for **SOFT40051 - Advanced Software Engineering** coursework.

**Before Submission:**

1. Add your name/ID to all documentation
2. Review code comments
3. Test all features
4. Prepare demonstration
5. Check marking rubric compliance

---

## 📝 SUMMARY OF COMMANDS

```cmd
# Setup
docker-compose -f docker-compose-jenkins.yml up -d
docker-compose -f docker-compose-gitea.yml up -d

# Build
mvn clean install

# Test
mvn test

# Run
mvn javafx:run

# Check containers
docker ps

# View logs
docker logs soft40051-mysql
docker logs soft40051-file-server

# Stop containers
docker stop soft40051-mysql soft40051-file-server

# Start containers
docker start soft40051-mysql soft40051-file-server
```

---

**Estimated Total Setup Time:** 45-60 minutes  
**Most Time-Consuming:** First Maven build (downloads dependencies)  
**Requires Internet:** Yes (for dependency download)  
**Can Run Offline After Setup:** Yes (except CI/CD features)

---

## END OF COMPLETE SETUP GUIDE

For additional help, see:

- `JENKINS_SETUP.md` - CI/CD configuration
- `GITEA_SETUP.md` - Git service setup
- `IMPLEMENTATION_REPORT.md` - Feature documentation
- `FILE_LOCATIONS.txt` - File placement guide
