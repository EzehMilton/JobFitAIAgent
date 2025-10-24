Job Fit Agent

A minimal Spring Boot service that uses Embabel Agents (LLM orchestration) to compute how well a candidate CV matches a given job description. It extracts skills and requirements using an LLM, then returns a numeric fit score and a short explanation.

Key technologies
- Java 25, (Spring Boot 3)
- Embabel Agent Starter (LLM agent framework)
- OpenAI (default LLM, configurable)
- Maven

Features
- Single REST endpoint POST /score
- Extracts skills from CV text and requirements from job description using an LLM
- Computes a 0–100 fit score with a brief explanation
- Pluggable LLM via Embabel configuration (OpenAI by default)

Project layout
- src/main/java/com/milton/agent/JobFitAgentApplication.java – Spring Boot app entry point, enables Embabel Agents
- src/main/java/com/milton/agent/controller/JobFitProviderController.java – REST controller exposing /score
- src/main/java/com/milton/agent/service/JobFitProviderAgent.java – Agent actions that interact with the LLM
- src/main/java/com/milton/agent/models – Simple records for request/response and intermediate objects
- src/main/resources/application.properties – Embabel/OpenAI configuration and logging
- pom.xml – Maven build, Embabel repositories and dependencies

Prerequisites
- Java Development Kit (JDK) 21 or newer. The project is configured with <java.version>25</java.version> in pom.xml. If you use an older JDK, either:
  - set your JAVA_HOME to a JDK that can target Java 25, or
  - lower the Java version in pom.xml to match your JDK (e.g., 21) and rebuild.
- Maven (wrapper included: mvnw/mvnw.cmd), or a compatible IDE (IntelliJ IDEA) with Maven support
- An OpenAI API key if you use the default OpenAI LLM

Configuration
The application reads Embabel LLM settings from src/main/resources/application.properties. Defaults are provided for OpenAI:

- embabel.ai.default-llm=openai
- embabel.ai.openai.api-key=${OPENAI_API_KEY}
- embabel.ai.openai.model=gpt-4o-mini
- embabel.ai.openai.base-url=https://api.openai.com/v1
- embabel.ai.openai.timeout=60s
- embabel.ai.openai.temperature=0.2
- embabel.ai.openai.max-tokens=1024

Set your OpenAI key as an environment variable before running:
- macOS/Linux: export OPENAI_API_KEY=sk-... 
- Windows (PowerShell): $Env:OPENAI_API_KEY="sk-..."

You may change the model, base URL, or switch to another LLM supported by Embabel by updating the properties accordingly.

Build and run
Using Maven Wrapper (recommended):
- macOS/Linux: ./mvnw spring-boot:run
- Windows: mvnw.cmd spring-boot:run

Alternatively, build a jar and run it:
- ./mvnw clean package
- java -jar target/agent-0.0.1-SNAPSHOT.jar

API usage
Endpoint
- POST /score
- Content-Type: application/json

Request body (JobFitRequest):
{
  "CvText": "Senior Java developer with Spring Boot, REST APIs, AWS, Docker...",
  "JobDescription": "Looking for a backend engineer with Java, Spring Boot, REST, cloud experience..."
}

Response body (FitScore):
{
  "score": 82,
  "explanation": "Strong overlap in Java, Spring Boot, and REST. Some gaps in Kubernetes and CI/CD."
}

Example with curl
curl -X POST "http://localhost:8080/score" \
  -H "Content-Type: application/json" \
  -d '{
        "CvText": "Senior Java developer with Spring Boot, REST APIs, AWS, Docker...",
        "JobDescription": "Looking for a backend engineer with Java, Spring Boot, REST, cloud experience..."
      }'

Notes
- Property names in JobFitRequest are capitalized (CvText, JobDescription). Make sure your JSON uses those exact names.
- The service relies on live calls to the configured LLM provider. Network access and valid credentials are required for meaningful results.

Testing
Run unit tests:
- ./mvnw test

There is a basic Spring context test at src/test/java/com/milton/agent/AgentApplicationTests.java.

Troubleshooting
- 401/403 from LLM provider: Ensure OPENAI_API_KEY is set and valid.
- Timeout errors: Increase embabel.ai.openai.timeout or check network connectivity.
- SSL or corporate proxy issues: Configure JVM/system proxy or set an alternative base URL if using a gateway.
- Build fails due to Java version: Align <java.version> in pom.xml with your installed JDK (e.g., 21) and re-run the build.
- Dependency resolution: The pom.xml includes Embabel repositories. If resolution fails behind a firewall, ensure your environment can reach https://repo.embabel.com.

Extending
- To adjust scoring logic or add extra signals, modify JobFitProviderAgent.calculateFitScore.
- To change how skills/requirements are extracted, tweak prompts in extractSkillsFromCv and extractJobRequirements.
- To add authentication or rate limiting, introduce Spring Boot security and filters in the controller layer.

License
No license specified in this repository. Add a LICENSE file if you intend to distribute.

Acknowledgements
- Embabel Agent Starter for agent orchestration
- Spring Boot for the web framework
