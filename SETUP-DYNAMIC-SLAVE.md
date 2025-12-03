# Jenkins Docker Dynamic Slave Setup - Ubuntu EC2

## What is Docker Dynamic Slave?

**Important**: You only need ONE EC2 instance. Jenkins will automatically create Docker containers as "slaves" on the same server when builds run, then destroy them when done.

- **No separate slave servers needed**
- **Containers are created automatically**
- **Containers are deleted automatically**
- **Each build gets a fresh, isolated environment**

---

## Prerequisites

### EC2 Instance
- **OS**: Ubuntu 20.04 or 22.04
- **Instance Type**: t2.medium (2 vCPU, 4GB RAM minimum)
- **Storage**: 20GB minimum
- **Security Group**: 
  - Port 22 (SSH) - Your IP only
  - Port 8080 (Jenkins) - Your IP only

---

## PART 1: SERVER SETUP (EC2 Ubuntu)

### Step 1: Connect to EC2
```bash
ssh -i your-key.pem ubuntu@<EC2-PUBLIC-IP>
```

### Step 2: Update System
```bash
sudo apt update
sudo apt upgrade -y
```

### Step 3: Install Java 17
```bash
sudo apt install openjdk-17-jdk -y
java -version
```

### Step 4: Install Jenkins
```bash
# Add Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

# Install Jenkins
sudo apt update
sudo apt install jenkins -y

# Start Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins
sudo systemctl status jenkins
```

### Step 5: Install Docker
```bash
# Install Docker
sudo apt install docker.io -y

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker
sudo systemctl status docker

# Verify Docker
docker --version
```

### Step 6: Give Jenkins Permission to Use Docker
```bash
# Add jenkins user to docker group
sudo usermod -aG docker jenkins

# Add ubuntu user to docker group (for testing)
sudo usermod -aG docker ubuntu

# Set Docker socket permissions
sudo chmod 666 /var/run/docker.sock

# Restart Jenkins to apply changes
sudo systemctl restart jenkins
```

### Step 7: Verify Docker Access for Jenkins
```bash
# Test as jenkins user
sudo -u jenkins docker ps

# Should show empty list (no errors)
```

### Step 8: Get Jenkins Initial Password
```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```
**Copy this password** - you'll need it in the next part.

---

## PART 2: JENKINS WEB CONFIGURATION

### Step 1: Access Jenkins Web UI
1. Open browser: `http://<EC2-PUBLIC-IP>:8080`
2. Paste the initial admin password from Step 8 above
3. Click **Continue**

### Step 2: Install Plugins
1. Select **Install suggested plugins**
2. Wait for installation to complete (2-3 minutes)

### Step 3: Create Admin User
1. Fill in the form:
   - Username: `admin`
   - Password: `<your-password>`
   - Full name: `Admin`
   - Email: `admin@example.com`
2. Click **Save and Continue**
3. Click **Save and Finish**
4. Click **Start using Jenkins**

### Step 4: Install Docker Plugins
1. Click **Manage Jenkins** (left sidebar)
2. Click **Manage Plugins**
3. Click **Available** tab
4. Search and check these plugins:
   - `Docker Pipeline`
   - `Docker Plugin`
5. Click **Install without restart**
6. Check **Restart Jenkins when installation is complete**
7. Wait for Jenkins to restart (refresh page after 30 seconds)
8. Login again with your admin credentials

---

## PART 3: CREATE YOUR FIRST PIPELINE

### Step 1: Push Code to Git Repository
```bash
# On your local machine (not EC2)
cd jenkins-dynamic-slave
git init
git add .
git commit -m "Initial commit"
git remote add origin <YOUR-GITHUB-REPO-URL>
git push -u origin main
```

### Step 2: Create Pipeline Job in Jenkins
1. Jenkins Dashboard → Click **New Item**
2. Enter name: `spring-boot-demo`
3. Select **Pipeline**
4. Click **OK**

### Step 3: Configure Pipeline
1. Scroll to **Pipeline** section
2. Definition: Select **Pipeline script from SCM**
3. SCM: Select **Git**
4. Repository URL: `<YOUR-GITHUB-REPO-URL>`
5. Credentials: 
   - If public repo: Leave as `- none -`
   - If private repo: Click **Add** → Add GitHub credentials
6. Branch Specifier: `*/main`
7. Script Path: `Jenkinsfile`
8. Click **Save**

---

## PART 4: RUN YOUR FIRST BUILD

### Step 1: Trigger Build
1. Click **Build Now** (left sidebar)
2. Build will appear under **Build History**
3. Click on **#1** (build number)
4. Click **Console Output**

### Step 2: Watch the Magic Happen
You'll see:
```
[Pipeline] node
Running on <container-id> in /home/jenkins/workspace/spring-boot-demo
[Pipeline] {
[Pipeline] stage (Checkout)
...
```

### Step 3: Verify Dynamic Slave
**Open new SSH terminal to EC2:**
```bash
# While build is running
docker ps
```
You'll see a Maven container running!

**After build completes:**
```bash
docker ps
```
Container is gone! (Dynamic slave destroyed)

---

## PART 5: VERIFY APPLICATION

### Check if Application is Running
```bash
# On EC2 server
docker ps

# You should see jenkins-docker-demo container
```

### Test Application
```bash
# From EC2
curl http://localhost:8080/
curl http://localhost:8080/health

# From your browser
http://<EC2-PUBLIC-IP>:8080/
```

**Note**: Add port 8080 to Security Group if accessing from browser.

---

## HOW IT WORKS

### Traditional Jenkins Slave:
```
Jenkins Master → Permanent Slave Server (always running, costs money)
```

### Docker Dynamic Slave:
```
Jenkins Master → Creates Docker Container → Runs Build → Destroys Container
                 (only when needed)      (isolated)    (no waste)
```

### Your Pipeline Flow:
1. You click "Build Now"
2. Jenkins pulls `maven:3.9.5-eclipse-temurin-17` image
3. Jenkins creates container from image
4. Jenkins runs build inside container
5. Container has Maven, Java, Docker access
6. Build completes
7. Jenkins destroys container
8. Next build gets fresh container

---

## TROUBLESHOOTING

### Problem: "Permission denied" on docker.sock
```bash
sudo chmod 666 /var/run/docker.sock
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### Problem: "Cannot connect to Docker daemon"
```bash
sudo systemctl status docker
sudo systemctl restart docker
sudo systemctl restart jenkins
```

### Problem: Jenkins not accessible on port 8080
```bash
# Check if Jenkins is running
sudo systemctl status jenkins

# Check if port is open
sudo netstat -tlnp | grep 8080

# Check EC2 Security Group allows port 8080
```

### Problem: Build fails with "docker: command not found"
```bash
# Install Docker in the container (add to Jenkinsfile)
agent {
    docker {
        image 'maven:3.9.5-eclipse-temurin-17'
        args '-v /var/run/docker.sock:/var/run/docker.sock'
    }
}

# Then install Docker client in pipeline
sh 'apt-get update && apt-get install -y docker.io'
```

### Problem: Maven dependencies download every time
**Solution**: Already handled in Jenkinsfile with:
```groovy
args '-v $HOME/.m2:/root/.m2'
```
This caches Maven dependencies on the host.

---

## USEFUL COMMANDS

### Jenkins Commands
```bash
# Start/Stop/Restart Jenkins
sudo systemctl start jenkins
sudo systemctl stop jenkins
sudo systemctl restart jenkins
sudo systemctl status jenkins

# View Jenkins logs
sudo journalctl -u jenkins -f
sudo tail -f /var/lib/jenkins/logs/jenkins.log
```

### Docker Commands
```bash
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# List images
docker images

# Remove stopped containers
docker container prune -f

# Remove unused images
docker image prune -a -f

# View container logs
docker logs <container-id>

# Check disk usage
docker system df
```

### Application Commands
```bash
# Check if app is running
docker ps | grep jenkins-docker-demo

# View app logs
docker logs jenkins-docker-demo

# Stop app
docker stop jenkins-docker-demo

# Remove app container
docker rm jenkins-docker-demo
```

---

## SECURITY BEST PRACTICES

1. **Restrict Security Group**:
   - Port 22: Your IP only
   - Port 8080: Your IP only (or use VPN)

2. **Change Default Port** (optional):
```bash
sudo vi /etc/default/jenkins
# Change HTTP_PORT=8080 to HTTP_PORT=8081
sudo systemctl restart jenkins
```

3. **Enable HTTPS** (production):
   - Use Nginx reverse proxy with SSL
   - Use Let's Encrypt for free SSL certificate

4. **Regular Updates**:
```bash
sudo apt update
sudo apt upgrade -y
```

---

## WHAT YOU HAVE NOW

✅ **One EC2 server** running Jenkins + Docker  
✅ **No separate slave servers** needed  
✅ **Automatic container creation** for each build  
✅ **Automatic container cleanup** after builds  
✅ **Isolated build environments** (no conflicts)  
✅ **Cost efficient** (no idle resources)  
✅ **Scalable** (multiple builds = multiple containers)  

---

## NEXT STEPS

1. **Add GitHub Webhook**: Auto-trigger builds on git push
2. **Add Notifications**: Email/Slack on build success/failure
3. **Add More Stages**: Code quality, security scanning
4. **Deploy to Production**: Add deployment stage
5. **Multi-branch Pipeline**: Separate pipelines for dev/staging/prod

---

## SUMMARY

You've successfully set up Jenkins with Docker Dynamic Slaves on a single EC2 instance. Every build now runs in a fresh, isolated Docker container that's automatically created and destroyed. No manual slave management needed!
