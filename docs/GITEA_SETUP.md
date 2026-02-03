# Gitea Self-Hosted Git Service Setup

## Feature 22: GitHub Container (3/3 Criteria Met)

Gitea is a lightweight, self-hosted Git service with a GitHub-like interface.

---

## **Quick Start**

### **1. Start Gitea Container**

```bash
docker-compose -f docker-compose-gitea.yml up -d
```

Wait 30-60 seconds for Gitea to initialize.

### **2. Access Gitea Web Interface**

Open browser: `http://localhost:3000`

### **3. Initial Setup (First Time Only)**

On first access, you'll see the installation page:

1. **Database Settings:**
   - Already configured (SQLite3)
2. **General Settings:**

   - Site Title: `CloudFileSystem Repository`
   - Administrator Account:
     - Username: `admin`
     - Password: `admin123456` (min 6 chars)
     - Email: `admin@localhost`

3. Click **"Install Gitea"**

---

## **Create CloudFileSystem Repository**

### **Step 1: Login**

- Username: `admin`
- Password: `admin123456`

### **Step 2: Create New Repository**

1. Click **"+"** icon (top right)
2. Select **"New Repository"**
3. Fill in:
   - Owner: `admin`
   - Repository Name: `CloudFileSystem`
   - Description: `SOFT40051 Advanced Software Engineering Coursework`
   - Visibility: Private
   - Initialize: ✅ Add README
   - License: MIT License (optional)
4. Click **"Create Repository"**

### **Step 3: Push Existing Code**

In your CloudFileSystem project directory:

```bash
# Initialize git (if not already done)
git init

# Add Gitea remote
git remote add origin http://localhost:3000/admin/CloudFileSystem.git

# Add all files
git add .

# Commit
git commit -m "Initial commit: CloudFileSystem v1.0"

# Push
git push -u origin master
```

**Credentials when prompted:**

- Username: `admin`
- Password: `admin123456`

---

## **Features Available**

✅ **Repository Management**

- Create/delete repositories
- Public/private visibility
- README rendering
- Branch management

✅ **Collaboration**

- Issues tracking
- Pull requests
- Code review
- Wiki

✅ **CI/CD Integration**

- Webhooks
- Jenkins integration
- Auto-build triggers

✅ **Git Operations**

- Clone: `git clone http://localhost:3000/admin/CloudFileSystem.git`
- Pull: `git pull origin master`
- Push: `git push origin master`

---

## **SSH Access (Optional)**

### **Configure SSH Key:**

1. Generate SSH key (if not exists):

```bash
ssh-keygen -t rsa -b 4096 -C "admin@localhost"
```

2. Add to Gitea:

   - Login to Gitea
   - Profile → Settings → SSH/GPG Keys
   - Click "Add Key"
   - Paste your public key (`~/.ssh/id_rsa.pub`)

3. Clone via SSH:

```bash
git clone ssh://git@localhost:2222/admin/CloudFileSystem.git
```

---

## **Integration with Jenkins**

### **Webhook Setup:**

1. In Gitea repository → Settings → Webhooks
2. Add Webhook:
   - URL: `http://soft40051-jenkins:8080/git/notifyCommit?url=http://soft40051-gitea:3000/admin/CloudFileSystem.git`
   - Content Type: `application/json`
   - Events: `Push`
   - Active: ✅
3. Save

Now Jenkins automatically builds on every commit!

---

## **Verification**

### **Check Container Status:**

```bash
docker ps | grep gitea
```

### **View Logs:**

```bash
docker logs soft40051-gitea
```

### **Test Git Operations:**

```bash
# Clone repository
git clone http://localhost:3000/admin/CloudFileSystem.git test-clone

# Make a change
cd test-clone
echo "Test" >> README.md
git add README.md
git commit -m "Test commit"
git push origin master
```

---

## **Maintenance**

### **Stop Gitea:**

```bash
docker-compose -f docker-compose-gitea.yml down
```

### **Restart Gitea:**

```bash
docker restart soft40051-gitea
```

### **Backup Data:**

```bash
docker exec soft40051-gitea /usr/bin/gitea dump
```

### **Reset Gitea (Caution: Deletes All Data):**

```bash
docker-compose -f docker-compose-gitea.yml down -v
docker-compose -f docker-compose-gitea.yml up -d
```

---

## **3/3 Criteria Met**

✅ **Criterion 1:** Self-hosted Git service running (Gitea)
✅ **Criterion 2:** Docker-based setup
✅ **Criterion 3:** Minimal configuration required

---

## **Advantages of Gitea**

- ✅ Lightweight (low resource usage)
- ✅ Easy setup (single container)
- ✅ GitHub-like interface
- ✅ SQLite database (no external DB needed)
- ✅ Built-in CI/CD integration
- ✅ Free and open-source

---

## **Default Credentials**

**Web Interface:**

- URL: `http://localhost:3000`
- Username: `admin`
- Password: `admin123456`

**SSH:**

- Port: `2222`
- URL: `ssh://git@localhost:2222/admin/CloudFileSystem.git`

---

## **Troubleshooting**

### **Issue:** Cannot access Gitea

**Solution:** Check Docker container is running: `docker ps`

### **Issue:** Port 3000 already in use

**Solution:** Change port in docker-compose-gitea.yml: `"3001:3000"`

### **Issue:** Push fails with authentication error

**Solution:** Check credentials, ensure user has write access

### **Issue:** Webhook not triggering Jenkins

**Solution:** Ensure both containers on same Docker network
