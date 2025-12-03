# Jenkins Docker Demo - Spring Boot Application

A simple Spring Boot REST API application demonstrating Jenkins CI/CD with Docker dynamic slaves.

## Project Structure

```
jenkins-dynamic-slave/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── Application.java
│   │   │   └── HelloController.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/example/
│           └── HelloControllerTest.java
├── Dockerfile
├── Jenkinsfile
├── pom.xml
├── SETUP-DYNAMIC-SLAVE.md
└── README.md
```

## Application Details

### Endpoints
- `GET /` - Returns welcome message
- `GET /health` - Health check endpoint

### Technology Stack
- Java 17
- Spring Boot 3.2.0
- Maven 3.9+
- JUnit 5

## Local Development

### Prerequisites
- Java 17+
- Maven 3.6+

### Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/jenkins-docker-demo-1.0.0.jar
```

### Test the Application

```bash
# Run tests
mvn test

# Access the application
curl http://localhost:8080/
curl http://localhost:8080/health
```

## Docker Build

```bash
# Build Docker image
docker build -t jenkins-docker-demo:latest .

# Run container
docker run -d -p 8080:8080 --name demo-app jenkins-docker-demo:latest

# Test
curl http://localhost:8080/
```

## Jenkins Pipeline

The `Jenkinsfile` defines a complete CI/CD pipeline with the following stages:

1. **Checkout** - Clone the repository
2. **Build** - Compile and package with Maven
3. **Test** - Run unit tests
4. **Docker Build** - Create Docker image
5. **Deploy** - Run the application container

### Dynamic Slave Configuration

The pipeline uses Docker dynamic slaves:
```groovy
agent {
    docker {
        image 'maven:3.9.5-eclipse-temurin-17'
        args '-v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.m2:/root/.m2'
    }
}
```

## Setup Instructions

For complete Jenkins and Docker dynamic slave setup on EC2, see:
**[SETUP-DYNAMIC-SLAVE.md](SETUP-DYNAMIC-SLAVE.md)**

## Quick Start on EC2

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd jenkins-dynamic-slave
   ```

2. **Follow the setup guide**
   - See `SETUP-DYNAMIC-SLAVE.md` for detailed instructions

3. **Create Jenkins pipeline job**
   - Point to your repository
   - Use `Jenkinsfile` from the repo

4. **Run the pipeline**
   - Click "Build Now"
   - Watch the magic happen!

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_PORT | 8080 | Application port |
| SPRING_APPLICATION_NAME | jenkins-docker-demo | Application name |

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

MIT License
