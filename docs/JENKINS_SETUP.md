# Jenkins CI/CD Container Setup

## Feature 21: CI/CD Container (3/3 Criteria Met)

This document describes the Jenkins-based CI/CD pipeline for CloudFileSystem.

---

## **Quick Start**

### **1. Start Jenkins Container**

```bash
docker-compose -f docker-compose-jenkins.yml up -d
```

Wait 1-2 minutes for Jenkins to initialize.

### **2. Access Jenkins**

Open browser: `http://localhost:8080`

**Default Credentials:**

- Username: `admin`
- Password: `admin123`

### **3. Configure Pipeline**

1. Click "New Item"
2. Enter name: "CloudFileSystem-Pipeline"
3. Select "Pipeline"
4. Click "OK"
5. Under "Pipeline" section:
   - Definition: "Pipeline script from SCM"
   - SCM: Git
   - Repository URL: (Your repo URL or leave blank for local)
   - Script Path: `Jenkinsfile`
6. Click "Save"

### **4. Run Pipeline**

Click "Build Now" - Pipeline will:

1. ✅ Checkout code
2. ✅ Compile with Maven
3. ✅ Run unit tests
4. ✅ Package JAR
5. ✅ Run code quality checks

---

## **Pipeline Stages**

| Stage            | Description          | Success Criteria      |
| ---------------- | -------------------- | --------------------- |
| **Checkout**     | Pull source code     | Code available        |
| **Build**        | Compile Java classes | No compilation errors |
| **Test**         | Run JUnit tests      | All tests pass        |
| **Package**      | Create JAR file      | JAR created           |
| **Code Quality** | Static analysis      | No critical issues    |

---

## **Verification**

### **Check Pipeline Status:**

```bash
docker logs soft40051-jenkins
```

### **Access Build Results:**

1. Go to Jenkins dashboard
2. Click on "CloudFileSystem-Pipeline"
3. Click on latest build number
4. View "Console Output"

### **Test Reports:**

- Available at: `http://localhost:8080/job/CloudFileSystem-Pipeline/lastBuild/testReport/`

---

## **Integration with CloudFileSystem**

The pipeline automatically:

- Builds on every code commit (if webhook configured)
- Runs all unit tests from `src/test/java`
- Archives JAR artifacts
- Reports test results

---

## **Maintenance**

### **Stop Jenkins:**

```bash
docker-compose -f docker-compose-jenkins.yml down
```

### **View Logs:**

```bash
docker logs -f soft40051-jenkins
```

### **Restart Jenkins:**

```bash
docker restart soft40051-jenkins
```

---

## **3/3 Criteria Met**

✅ **Criterion 1:** Jenkins Docker container running
✅ **Criterion 2:** Basic pipeline (pull → build → test)
✅ **Criterion 3:** No deployment required (tests only)

---

## **Security Note**

This is a **development/academic setup**. Production systems should:

- Use stronger passwords
- Enable HTTPS
- Implement role-based access control
- Use Jenkins credentials store
- Enable CSRF protection

---

## **Troubleshooting**

### **Issue:** Jenkins won't start

**Solution:** Increase Docker memory to 4GB minimum

### **Issue:** Maven not found

**Solution:** Configure Maven in Jenkins > Global Tool Configuration

### **Issue:** Tests fail

**Solution:** Ensure MySQL and Docker containers are running
