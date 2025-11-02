Job Fit Agent

A Spring Boot web application that uses Embabel Agents (LLM orchestration) to analyze how well a candidate's CV matches a job description. It features both a responsive web interface and REST API endpoints for CV-to-job matching with AI-powered insights.

Key technologies
- Java 21 (Spring Boot 3.5.6)
- Embabel Agent Starter (LLM agent framework)
- OpenAI GPT models (gpt-4o-mini, gpt-5)
- Thymeleaf (web UI templating)
- Apache PDFBox (PDF text extraction)
- Bootstrap 5 (responsive UI)
- Maven

Features
- Modern responsive web interface with drag-and-drop PDF upload
- CV session storage for reuse across multiple job analyses
- Rate limiting (5 free analyses per IP per day)
- PDF text extraction from CV and job description files
- AI-powered skill extraction and job requirement analysis
- 0–100 fit score with detailed explanation
- REST API endpoints for programmatic access
- Real-time progress tracking during analysis

Project layout
- src/main/java/com/milton/agent/JobFitAgentApplication.java – Spring Boot app entry point
- src/main/java/com/milton/agent/controller/UiController.java – Web interface controller
- src/main/java/com/milton/agent/controller/JobFitProviderController.java – REST API controller
- src/main/java/com/milton/agent/service/JobFitProviderAgent.java – AI agent for CV/job analysis
- src/main/java/com/milton/agent/service/RateLimitService.java – IP-based rate limiting
- src/main/java/com/milton/agent/service/PDFTextExtratorImpl.java – PDF text extraction
- src/main/java/com/milton/agent/models – Data models and records
- src/main/resources/templates/index.html – Responsive web interface
- src/main/resources/prompts/ – AI prompts for extraction and scoring
- src/main/resources/application.properties – Configuration
- pom.xml – Maven dependencies and build configuration

Prerequisites
- Java Development Kit (JDK) 21 or newer
- Maven (wrapper included: mvnw/mvnw.cmd), or compatible IDE
- OpenAI API key for LLM access

Configuration
The application reads settings from src/main/resources/application.properties:

```properties
# Embabel AI Configuration
embabel.ai.default-llm=openai
embabel.ai.openai.api-key=${OPENAI_API_KEY}
embabel.ai.openai.model=gpt-4o-mini
embabel.ai.openai.base-url=https://api.openai.com/v1
embabel.ai.openai.timeout=60s
embabel.ai.openai.temperature=0.2
embabel.ai.openai.max-tokens=1024

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

Set your OpenAI API key as an environment variable:
- macOS/Linux: `export OPENAI_API_KEY=sk-...`
- Windows: `$Env:OPENAI_API_KEY="sk-..."`

Build and run
Using Maven Wrapper:
```bash
# macOS/Linux
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Alternatively, build and run as JAR:
```bash
./mvnw clean package
java -jar target/agent-0.0.1-SNAPSHOT.jar
```

Usage

Web Interface
1. Navigate to http://localhost:8080
2. Upload your CV (PDF format, max 10MB)
3. Paste the job description text
4. Click "Generate Score" to analyze
5. View detailed results with score and explanation
6. Optionally reuse your CV for analyzing multiple jobs

REST API Endpoints

**PDF Upload Endpoint**
- `POST /score`
- Content-Type: `multipart/form-data`
- Parameters:
  - `candidateFile`: PDF file containing CV
  - `jobDescriptionFile`: PDF file containing job description

**Web Form Endpoint**
- `POST /generate`
- Content-Type: `multipart/form-data`
- Parameters:
  - `candidateFile`: PDF file (optional if reusing)
  - `reuseCv`: boolean to reuse stored CV
  - `jobDescription`: text content of job description

Response format (FitScore):
```json
{
  "score": 85,
  "explanation": "Strong alignment in Java and Spring Boot expertise. Your 5+ years of REST API development directly matches their requirements..."
}
```

Rate Limiting
- Free tier: 5 analyses per IP address per day
- Resets at midnight UTC
- Session CV storage allows multiple job comparisons without re-upload

Testing
Run unit tests:
```bash
./mvnw test
```

Troubleshooting
- **401/403 from OpenAI**: Verify OPENAI_API_KEY is set correctly
- **File upload errors**: Ensure PDF files are under 10MB
- **Timeout errors**: Increase `embabel.ai.openai.timeout` value
- **Rate limit exceeded**: Wait until midnight or clear browser session
- **Build failures**: Ensure Java 21+ is installed and JAVA_HOME is set
- **PDF processing errors**: Verify uploaded files are valid PDF format

Extending the application
- **Modify scoring logic**: Update `JobFitProviderAgent.calculateFitScore()`
- **Customize extraction prompts**: Edit files in `src/main/resources/prompts/`
- **Adjust rate limits**: Modify `MAX_REQUESTS_PER_DAY` in `RateLimitService`
- **Add authentication**: Implement Spring Security filters
- **Support other file formats**: Extend `TextExtractor` interface

AI Model Configuration
The application uses different models for different tasks:
- Skills extraction: GPT-4o-mini (faster, cost-effective)
- Job requirements: GPT-4o-mini  
- Final scoring: GPT-5 (higher quality analysis)

License
No license specified. Add a LICENSE file if distributing.

Contact
Created by Chikere Ezeh - chikere@gmail.com

Acknowledgements
- Embabel Agent Starter for LLM orchestration
- Spring Boot for the web framework
- Apache PDFBox for PDF processing
- Bootstrap for responsive UI design
