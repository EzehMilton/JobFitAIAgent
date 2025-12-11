# JobFit AI Agent

**JobFit** is an intelligent Spring Boot application that uses AI to match candidates' CVs with job descriptions, providing comprehensive career guidance through personalized recommendations, fit scoring, CV optimization, and interview preparation.

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [User Workflows](#user-workflows)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Development Guide](#development-guide)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Overview

JobFit analyzes candidates' CVs against target job descriptions using AI-powered agents to provide:
- **Fit Scores** (0-100) with detailed explanations
- **Personalized Career Suggestions** based on skills and experience
- **CV Improvement Recommendations** with keyword suggestions
- **Optimized CV Rewrites** tailored for ATS systems
- **Interview Preparation Guides** with STAR stories and likely questions

The application leverages the Embabel Agent Framework to orchestrate multiple OpenAI models, providing both a rich web UI and REST API access.

## Key Features

### 🎯 Core Capabilities

- **AI-Powered Job Fit Scoring**
  - Extracts skills from CV and requirements from job description
  - Generates 0-100 fit score with detailed narrative explanation
  - Identifies strengths, weaknesses, and alignment issues

- **Quick vs. Thoughtful Response Modes**
  - Quick Mode: Fast responses using GPT-4o-mini
  - Thoughtful Mode: Detailed analysis using GPT-5
  - User-selectable at analysis time

- **Personalized Career Guidance** (Score-based Actions)
  - **Suggestions** (Score < 40%): Alternative career paths and skill development
  - **Improve Fit** (40-74%): Gap analysis, keyword suggestions, course recommendations
  - **Upgrade CV** (75-85%): ATS-optimized CV rewrite with downloadable PDF
  - **Get Ready** (>85%): Interview prep with 60-second pitch, STAR stories, questions

### 💼 User Experience

- **Session-Based CV Management**
  - Upload once, analyze multiple times
  - Clear cached CV anytime
  - Supports PDF uploads up to 10MB

- **Interactive Dashboard**
  - Save and track multiple job fit analyses
  - Quick access to historical results
  - View and download action reports

- **PDF Generation**
  - Download optimized CVs
  - Export career suggestions reports
  - Save improvement recommendations
  - Print interview prep guides

- **Loading Feedback**
  - Full-screen loading overlay for long-running operations
  - Clear progress indicators
  - User-friendly time estimates

### 🔐 Security & Limits

- **User Authentication**
  - Secure login/registration system
  - Spring Security integration
  - Session-based authentication

- **Rate Limiting**
  - Configurable daily scan limits per IP (default: 3)
  - Visual indicators for remaining scans
  - Automatic midnight reset

- **Input Validation**
  - PDF-only file uploads
  - File size restrictions
  - XSS protection

## Technology Stack

### Backend
- **Java 21** - Modern Java features and performance
- **Spring Boot 3.5.6** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database abstraction
- **H2 Database** - In-memory data storage
- **Embabel Agent Framework 0.1.2** - AI agent orchestration
- **OpenAI API** - GPT-4o-mini and GPT-5 models
- **Apache PDFBox 2.0.29** - PDF generation and text extraction
- **Lombok** - Code generation and boilerplate reduction

### Frontend
- **Thymeleaf** - Server-side templating
- **Bootstrap 5.3.3** - UI framework
- **Font Awesome 6.4.0** - Icons
- **Vanilla JavaScript** - Client-side interactions
- **Custom CSS** - Gradient designs and animations

### Build Tools
- **Maven** - Dependency management and build
- **Maven Wrapper** - Consistent build environment

## Architecture

JobFit follows a layered architecture pattern:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │  Index   │  │Dashboard │  │Suggestion│  │Get Ready│ │
│  │   Page   │  │   Page   │  │   Page   │  │  Page   │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                      │
│  ┌────────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ Analysis       │  │Recommenda-   │  │  Upgrade    │ │
│  │ Controller     │  │tions         │  │  CV         │ │
│  │                │  │Controller    │  │  Controller │ │
│  └────────────────┘  └──────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                     Service Layer                        │
│  ┌────────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ JobFit         │  │ Dashboard    │  │  PDF        │ │
│  │ ProviderAgent  │  │ Service      │  │  Service    │ │
│  │ (Embabel)      │  │              │  │             │ │
│  └────────────────┘  └──────────────┘  └─────────────┘ │
│  ┌────────────────┐  ┌──────────────┐                  │
│  │ RateLimit      │  │ Text         │                  │
│  │ Service        │  │ Extractor    │                  │
│  └────────────────┘  └──────────────┘                  │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                  Data Access Layer                       │
│  ┌────────────────────────────────────────────────────┐ │
│  │      DashboardEntryRepository (JPA/Hibernate)      │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                   Database Layer                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │              H2 In-Memory Database                 │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘

        External: OpenAI API (GPT-4o-mini, GPT-5)
```

### Component Responsibilities

**Controllers** - Handle HTTP requests, session management, validation
- `AnalysisController` - Main job fit analysis workflow
- `RecommendationsController` - Career suggestions, improvement, interview prep
- `UpgradeCvController` - CV optimization and PDF downloads
- `NavigationController` - Page routing
- `SessionController` - Login/registration

**Agents** (Embabel Framework)
- `JobFitProviderAgent` - Production agent with OpenAI integration
- `MockJobFitProviderAgent` - Development agent with mock responses

**Services**
- `DashboardService` - Persist and retrieve analysis history
- `PdfService` - Generate PDFs from text content
- `RateLimitService` - IP-based daily usage tracking
- `TextExtractor` - Extract text from PDF uploads
- `MatchPresentationService` - Format fit scores for display

**Models** (Records)
- Request models: `JobFitRequest`, `CvRewriteRequest`, `SuggestionsRequest`, etc.
- Response models: `FitScore`, `CareerSuggestions`, `ImproveScore`, `InterviewPrep`, `UpgradedCv`
- Domain models: `CvSkills`, `JobRequirements`, `DashboardEntry`

## Getting Started

### Prerequisites

- **JDK 21 or higher** - [Download here](https://adoptium.net/)
- **Maven 3.6+** (or use included Maven Wrapper)
- **OpenAI API Key** - Required for production mode
  - Get one at [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
  - Ensure access to GPT-4o-mini and GPT-5 models

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd JobFitAIAgent
   ```

2. **Set environment variables**
   ```bash
   # Required for production mode
   export OPENAI_API_KEY=sk-your-openai-api-key

   # Optional: Choose profile (prod or dev)
   export SPRING_PROFILES_ACTIVE=prod
   ```

3. **Run the application**
   ```bash
   # Using Maven Wrapper (recommended)
   ./mvnw spring-boot:run

   # Or using Maven directly
   mvn spring-boot:run
   ```

4. **Access the application**
   - Web UI: [http://localhost:8080](http://localhost:8080)
   - H2 Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
     - JDBC URL: `jdbc:h2:mem:jobfitdb`
     - Username: `sa`
     - Password: (empty)

### Quick Start Guide

1. **Register an account** at `/register`
2. **Login** at `/login`
3. **Upload your CV** (PDF format) on the main page
4. **Paste the job description**
5. **Select analysis mode** (Quick or Thoughtful)
6. **Click "Generate JobFit Score"**
7. **View results** and explore recommended actions
8. **Save to dashboard** for future reference

## Configuration

All configuration is managed through `src/main/resources/application.properties`:

### Core Settings

| Property | Default | Description |
|----------|---------|-------------|
| `embabel.ai.openai.api-key` | `${OPENAI_API_KEY}` | OpenAI API key (required for prod) |
| `embabel.ai.openai.model` | `gpt-4o-mini` | Default fallback model |
| `embabel.ai.openai.temperature` | `0.2` | Model creativity (0.0-1.0) |
| `embabel.ai.openai.max-tokens` | `1024` | Default max tokens per response |
| `embabel.ai.openai.timeout` | `60s` | API request timeout |

### File Upload Settings

| Property | Default | Description |
|----------|---------|-------------|
| `spring.servlet.multipart.max-file-size` | `10MB` | Maximum file upload size |
| `spring.servlet.multipart.max-request-size` | `10MB` | Maximum total request size |

### Rate Limiting

| Property | Default | Description |
|----------|---------|-------------|
| `jobfit.rate-limit.max-daily-scans` | `3` | Free scans per IP per day |

### Score Thresholds

| Property | Default | Description |
|----------|---------|-------------|
| `jobfit.score.suggestions-threshold` | `40` | Max score for suggestions action |
| `jobfit.score.improve-score-lower` | `40` | Min score for improve fit action |
| `jobfit.score.improve-score-upper` | `74` | Max score for improve fit action |
| `jobfit.score.cv-upgrade-lower` | `75` | Min score for CV upgrade action |
| `jobfit.score.cv-upgrade-upper` | `85` | Max score for CV upgrade action |
| `jobfit.score.interview-prep-threshold` | `85` | Min score for interview prep action |

### Match Labels

| Property | Default | Description |
|----------|---------|-------------|
| `jobfit.score.excellent-threshold` | `90` | "Excellent Match" label |
| `jobfit.score.good-threshold` | `70` | "Good Match" label |
| `jobfit.score.partial-threshold` | `50` | "Partial Match" label |

### Database (H2)

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:h2:mem:jobfitdb` | H2 database URL |
| `spring.datasource.username` | `sa` | Database username |
| `spring.datasource.password` | (empty) | Database password |
| `spring.h2.console.enabled` | `true` | Enable H2 web console |
| `spring.h2.console.path` | `/h2-console` | H2 console URL path |

### Profiles

| Profile | Description |
|---------|-------------|
| `prod` | Production mode with real OpenAI API calls |
| `dev` | Development mode with mock responses (no API calls) |

Set via environment variable:
```bash
export SPRING_PROFILES_ACTIVE=dev
```

## User Workflows

### 1. Initial Job Fit Analysis

```
User uploads CV (PDF) → User pastes job description →
Selects Quick/Thoughtful mode → Submits form →
Loading overlay appears → AI analyzes match →
Score displayed (0-100) with explanation →
Appropriate actions shown based on score
```

### 2. Score < 40% (Suggestions)

```
User clicks "Suggestions" → Loading overlay →
AI generates:
  - Alternative job titles
  - Skill clusters to develop
  - Strengths analysis
  - Areas to improve
  - Career direction advice
→ Results displayed → Option to download PDF
```

### 3. Score 40-74% (Improve Fit)

```
User clicks "Improve Score" → Loading overlay →
AI generates:
  - Missing skills/experience gaps
  - JD alignment issues
  - Recommended ATS keywords
  - Course recommendations
  - Achievement enhancement tips
→ Results displayed → Option to download PDF
```

### 4. Score 75-85% (Upgrade CV)

```
User clicks "Upgrade CV" → Loading overlay →
AI rewrites CV:
  - ATS keyword optimization
  - Role-aligned achievements
  - Improved formatting
→ Upgraded CV displayed →
User downloads optimized PDF
```

### 5. Score > 85% (Get Ready)

```
User clicks "Get Ready" → Loading overlay →
AI generates:
  - 60-second elevator pitch
  - Likely interview questions
  - STAR stories to prepare
  - Pre-interview recommendations
→ Results displayed → Option to download PDF
```

### 6. Dashboard Management

```
User saves analysis result →
Result appears in dashboard →
User can re-access any previous analysis →
Actions remain available for saved results
```

## Project Structure

```
JobFitAIAgent/
├── src/
│   ├── main/
│   │   ├── java/com/milton/agent/
│   │   │   ├── controller/
│   │   │   │   ├── AnalysisController.java          # Main analysis workflow
│   │   │   │   ├── RecommendationsController.java   # Suggestions, improve, get-ready
│   │   │   │   ├── UpgradeCvController.java         # CV upgrade & downloads
│   │   │   │   ├── SessionController.java           # Login/registration
│   │   │   │   ├── NavigationController.java        # Page routing
│   │   │   │   └── SessionAttributes.java           # Session key constants
│   │   │   ├── service/
│   │   │   │   ├── JobFitProviderAgent.java         # Production AI agent
│   │   │   │   ├── MockJobFitProviderAgent.java     # Development mock agent
│   │   │   │   ├── DashboardService.java            # Dashboard persistence
│   │   │   │   ├── PdfService.java                  # PDF generation
│   │   │   │   ├── RateLimitService.java            # IP-based rate limiting
│   │   │   │   ├── TextExtractor.java               # PDF text extraction
│   │   │   │   └── MatchPresentationService.java    # Score formatting
│   │   │   ├── repository/
│   │   │   │   └── DashboardEntryRepository.java    # JPA repository
│   │   │   ├── models/
│   │   │   │   ├── JobFitRequest.java               # Initial analysis request
│   │   │   │   ├── FitScore.java                    # Score + explanation
│   │   │   │   ├── CvSkills.java                    # Extracted CV skills
│   │   │   │   ├── JobRequirements.java             # Extracted JD requirements
│   │   │   │   ├── CareerSuggestions.java           # Suggestions response
│   │   │   │   ├── ImproveScore.java                # Improvement response
│   │   │   │   ├── UpgradedCv.java                  # Upgraded CV response
│   │   │   │   ├── InterviewPrep.java               # Interview prep response
│   │   │   │   ├── DashboardEntry.java              # Saved analysis entity
│   │   │   │   └── *Request.java                    # Various request models
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java              # Spring Security setup
│   │   │   │   └── PromptLoader.java                # Load prompt templates
│   │   │   ├── util/
│   │   │   │   ├── FileValidationUtil.java          # File validation
│   │   │   │   └── IpAddressUtil.java               # IP extraction
│   │   │   ├── exceptions/
│   │   │   │   └── PromptLoaderException.java       # Custom exception
│   │   │   └── JobFitAgentApplication.java          # Main application class
│   │   └── resources/
│   │       ├── prompts/
│   │       │   ├── skills-extractor.txt             # CV skills extraction
│   │       │   ├── job-description-extractor.txt    # JD requirements extraction
│   │       │   ├── jobfit-fit-score.txt             # Fit score calculation
│   │       │   ├── cv-rewriter.txt                  # CV upgrade prompt
│   │       │   ├── suggestions.txt                  # Career suggestions
│   │       │   ├── improve.txt                      # Improvement recommendations
│   │       │   └── getready.txt                     # Interview prep
│   │       ├── templates/
│   │       │   ├── index.html                       # Main analysis page
│   │       │   ├── dashboard.html                   # Dashboard view
│   │       │   ├── suggestions.html                 # Suggestions page
│   │       │   ├── improve.html                     # Improve fit page
│   │       │   ├── upgrade_cv.html                  # Upgrade CV page
│   │       │   ├── getready.html                    # Get ready page
│   │       │   ├── login.html                       # Login page
│   │       │   └── register.html                    # Registration page
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   └── loading-overlay.css          # Reusable loading styles
│   │       │   ├── js/
│   │       │   │   └── loading-overlay.js           # Loading overlay utility
│   │       │   └── loading-overlay-example.html     # Usage examples
│   │       └── application.properties               # Application configuration
│   └── test/
│       └── java/com/milton/agent/
│           └── JobFitAgentApplicationTests.java     # Basic tests
├── pom.xml                                          # Maven dependencies
├── README.md                                        # This file
└── LOADING_OVERLAY_README.md                        # Loading overlay docs
```

## API Documentation

### REST Endpoint: Generate Fit Score

**Endpoint:** `POST /score`

**Description:** Analyzes a candidate's CV against a job description and returns a fit score.

**Content-Type:** `multipart/form-data`

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `candidateFile` | File | Yes | Candidate's CV (PDF format) |
| `jobDescriptionFile` | File | Yes | Job description (PDF format) |
| `analysisMode` | String | No | `quick` (default) or `thoughtful` |

**Response:**

```json
{
  "score": 82,
  "explanation": "Strong alignment on product execution and technical leadership. The candidate demonstrates deep experience with cross-functional teams and data-driven decision making, which directly matches the role requirements. Minor gaps identified in cloud security certifications and experience with emerging AI frameworks."
}
```

**Status Codes:**

- `200 OK` - Analysis completed successfully
- `400 Bad Request` - Invalid file format or missing parameters
- `429 Too Many Requests` - Daily rate limit exceeded
- `500 Internal Server Error` - Analysis failed

**Example:**

```bash
curl -X POST http://localhost:8080/score \
  -F "candidateFile=@candidate_cv.pdf" \
  -F "jobDescriptionFile=@job_description.pdf" \
  -F "analysisMode=thoughtful"
```

### Response Models

All API responses follow consistent JSON structures defined by the record models in the `models` package.

## Development Guide

### Running in Development Mode

Use the `dev` profile to avoid OpenAI API calls during development:

```bash
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

The `MockJobFitProviderAgent` will return static, realistic responses for testing UI flows.

### Modifying AI Prompts

All prompts are externalized in `src/main/resources/prompts/`:

1. **skills-extractor.txt** - Extracts skills from CV
2. **job-description-extractor.txt** - Extracts requirements from JD
3. **jobfit-fit-score.txt** - Generates fit score and explanation
4. **cv-rewriter.txt** - Rewrites CV for ATS optimization
5. **suggestions.txt** - Generates career suggestions
6. **improve.txt** - Creates improvement recommendations
7. **getready.txt** - Prepares interview guidance

Modify these files to adjust AI behavior. Changes take effect on application restart.

### Adding New Actions

To add a new score-based action:

1. **Create request/response models** in `models` package
2. **Add prompt template** in `resources/prompts/`
3. **Create agent action** in `JobFitProviderAgent`
4. **Add controller method** in appropriate controller
5. **Create HTML template** in `resources/templates/`
6. **Update dashboard** to show new action button
7. **Add configuration** for score thresholds

### Working with the Loading Overlay

The reusable loading overlay is documented in `LOADING_OVERLAY_README.md`. Key files:

- **CSS:** `static/css/loading-overlay.css`
- **JS:** `static/js/loading-overlay.js`
- **Example:** `static/loading-overlay-example.html`

Usage:
```html
<link rel="stylesheet" href="/css/loading-overlay.css">
<script src="/js/loading-overlay.js"></script>
<script>
  LoadingOverlay.attachToButtons('.action-btn');
</script>
```

### Building for Production

```bash
# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# Run JAR
java -jar target/agent-1.0.0.jar
```

The JAR includes all dependencies and can be deployed to any environment with Java 21+.

## Security

### Authentication

- Spring Security with form-based login
- In-memory user store (H2 database)
- Session-based authentication
- Password encoding (BCrypt recommended for production)

### Authorization

- All pages except `/login` and `/register` require authentication
- CSRF protection enabled
- Secure session management

### Input Validation

- PDF-only file uploads
- File size limits (10MB)
- XSS protection via Thymeleaf escaping
- SQL injection protection via JPA/Hibernate

### Rate Limiting

- IP-based daily scan limits
- Prevents abuse and controls OpenAI API costs
- Configurable per environment

### Best Practices for Production

1. **Use HTTPS** - Configure SSL/TLS certificates
2. **Secure API Keys** - Use environment variables, never commit keys
3. **Database Migration** - Switch from H2 to PostgreSQL/MySQL
4. **Password Hashing** - Implement BCrypt with proper salting
5. **Session Security** - Configure secure session cookies
6. **CORS Configuration** - Restrict allowed origins
7. **Logging** - Implement comprehensive audit logging
8. **Monitoring** - Set up health checks and metrics
9. **Backup Strategy** - Regular database backups
10. **Rate Limiting** - Consider Redis for distributed rate limiting

## Troubleshooting

### Common Issues

**Problem:** Application fails to start with "OPENAI_API_KEY not found"

**Solution:** Set the environment variable:
```bash
export OPENAI_API_KEY=sk-your-key
```

Or switch to dev profile:
```bash
export SPRING_PROFILES_ACTIVE=dev
```

---

**Problem:** "401 Unauthorized" errors when calling OpenAI

**Solution:**
- Verify your API key is valid
- Check your OpenAI account has credits
- Ensure you have access to GPT-4o-mini and GPT-5

---

**Problem:** "File size exceeds maximum allowed" error

**Solution:** Increase the limit in `application.properties`:
```properties
spring.servlet.multipart.max-file-size=20MB
```

---

**Problem:** Rate limit reached immediately after restart

**Solution:** Rate limits are stored in-memory. They reset:
- On application restart
- At midnight (automatic cleanup)

For persistent rate limiting, implement Redis storage.

---

**Problem:** PDF generation fails with encoding errors

**Solution:** The `PdfService` sanitizes non-ASCII characters. If issues persist:
- Check CV contains valid UTF-8 text
- Verify PDFBox can extract text from the uploaded PDF
- Try re-saving the PDF with a different tool

---

**Problem:** Dashboard entries not persisting

**Solution:**
- Check H2 console at `/h2-console`
- Verify JDBC URL: `jdbc:h2:mem:jobfitdb`
- Ensure `spring.jpa.hibernate.ddl-auto=update`

---

**Problem:** Loading overlay doesn't appear

**Solution:**
- Check browser console for JavaScript errors
- Verify `loadingOverlay` element exists in HTML
- Ensure `.action-btn` class is on buttons
- Check z-index isn't overridden by other CSS

---

**Problem:** Slow response times

**Solution:**
- Use Quick mode (GPT-4o-mini) instead of Thoughtful mode
- Check OpenAI API status
- Consider implementing response caching
- Monitor OpenAI API rate limits

### Debug Mode

Enable detailed logging:

```properties
logging.level.com.embabel=DEBUG
logging.level.com.milton.agent=DEBUG
```

### Getting Help

1. Check the logs in the console
2. Review the H2 database state at `/h2-console`
3. Test with the mock agent (`SPRING_PROFILES_ACTIVE=dev`)
4. Verify environment variables are set correctly
5. Ensure all dependencies are downloaded (`./mvnw dependency:resolve`)

## License

**All rights reserved.**

This software is provided under a commercial End User License Agreement (EULA).

### Key Terms

- **Production Use:** A paid license is required for any production, hosted, or client-facing deployment
- **Redistribution:** Redistribution, sublicensing, or code modification for resale is strictly prohibited
- **Evaluation:** Evaluation or internal prototyping requires written approval
- **Confidentiality:** API keys and proprietary code must be kept confidential
- **Third-Party Terms:** All users must comply with OpenAI's Terms of Service and other third-party model providers

### Licensing & Contact

To obtain a commercial license, discuss partnership terms, or request evaluation access:

**Contact:** Chikere Ezeh
**Email:** chikere@gmail.com

**Use of this repository without a signed license agreement is strictly forbidden.**

---

## Acknowledgments

- **Embabel Agent Framework** - AI agent orchestration
- **OpenAI** - GPT-4o-mini and GPT-5 language models
- **Spring Boot Team** - Excellent application framework
- **Apache PDFBox** - PDF manipulation capabilities

---

**Version:** 1.0.0
**Last Updated:** December 2025
**Maintainer:** Chikere Ezeh
