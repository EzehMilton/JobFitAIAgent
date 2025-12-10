Job Fit Agent
=============

Job Fit Agent is a Spring Boot application that compares a candidate’s CV with a target job description and produces an AI-generated fit score, narrative explanation, and optional upgraded CV. It uses Embabel’s agent framework to orchestrate multiple OpenAI models and exposes both a rich web UI and a REST endpoint.

Key Capabilities
---------------
- **Job fit scoring** – Extracts skills from the CV and requirements from the job description, then runs a scoring prompt that returns a 0–100 score plus a human-readable explanation.
- **Response mode control** – Users can request a quick score (GPT‑5‑nano) or a more thoughtful score (GPT‑5). Quick mode is the default checkbox selection.
- **CV session reuse** – Uploaded CVs are stored in the session so users can run multiple analyses without uploading again and can clear the cached file at any time.
- **Upgrade my CV flow** – When the score is in the “almost there” band (75–85) users can request a rewritten CV that highlights ATS keywords, view a breakdown page, and download a PDF generated via Apache PDFBox.
- **Daily rate limiting** – Every IP receives 10 free scans per day. The UI surfaces remaining scans, and the backend enforces the limit.
- **REST access** – Programmatic clients can call `/score` with PDF files to obtain the same JSON response used by the UI.

Architecture Overview
---------------------
The app is split into three layers:
1. **Controllers** – UI responsibilities are split across focused controllers (`AnalysisController`, `UpgradeCvController`, `RecommendationsController`, `NavigationController`), while `JobFitProviderController` exposes the REST `/score` endpoint.
2. **Agents** – `JobFitProviderAgent` (prod profile) and `MockJobFitProviderAgent` (dev profile) define the Embabel agent actions for extracting CV skills, extracting job requirements, computing the fit score, and rewriting CVs.
3. **Services & Utilities** – `RateLimitService`, `TextExtractor` (PDF text extraction), and helper utilities manage IP limits, file validation, IP lookup, etc.

Requirements
------------
- JDK 21+
- Maven or the provided Maven Wrapper (`mvnw` / `mvnw.cmd`)
- OpenAI API key with access to gpt‑5 / gpt‑5‑nano (set `OPENAI_API_KEY`)

Getting Started
---------------
1. **Clone the repo** and move into the directory.
2. **Configure credentials**:
   ```bash
   export OPENAI_API_KEY=sk-your-key
   # Optional: switch to dev profile to use the mock agent
   export SPRING_PROFILES_ACTIVE=prod   # or dev
   ```
3. **Run the app**:
   ```bash
   ./mvnw spring-boot:run
   ```
4. **Open the UI** at [http://localhost:8080](http://localhost:8080) and upload a CV (PDF) plus a job description.

Configuration Highlights
------------------------
Settings live in `src/main/resources/application.properties`. Key entries:

| Property | Description |
| --- | --- |
| `embabel.ai.openai.api-key` | Inherits from `OPENAI_API_KEY`; required for prod profile. |
| `embabel.ai.openai.model` | Default fallback model (scoring agent overrides with GPT‑5 or GPT‑5‑nano). |
| `spring.servlet.multipart.max-file-size` | PDF upload limit (default 10MB). |
| `jobfit.rate-limit.max-daily-scans` | Number of free scans per IP per day (default 10). |
| `jobfit.upgrade-button.lower-score` | Minimum score that triggers the “Upgrade CV” CTA (default 75). |
| `jobfit.upgrade-button.upper-score` | Maximum score that still shows the CTA (default 85). |
| `spring.profiles.active` | Use `dev` for mock responses (no OpenAI calls) or `prod` for live scoring. |

Response Modes
--------------
- **Quick score** – Default checkbox in the UI. Sends `analysisMode=quick` and forces the scoring step to GPT‑5‑nano for fast turnaround.
- **Thoughtful score** – Users can select the second checkbox. Sends `analysisMode=thoughtful`, which switches the scoring agent to GPT‑5 for deeper reasoning (slower response, more detailed narratives).

Using the Web UI
----------------
1. Upload a PDF CV or reuse the cached CV if you already uploaded one earlier.
2. Paste the job description.
3. Choose **Quick score** or **Thoughtful score** above the “Generate Score” button.
4. Submit the form. A loading overlay appears while the AJAX call runs against `/generate`.
5. Review the score, explanation, and CV metadata in the result overlay.
6. If your score is between 75 and 85, click **Upgrade CV** to open the rewrite page, then download the generated PDF.

Rate Limiting
-------------
- `RateLimitService` enforces **10 scans per IP per day**.
- The UI banner displays remaining scans using 10 indicator dots.
- Once the quota is exhausted the user sees a friendly error and must wait for the daily reset (map resets at midnight + scheduled cleanup).
- Change the quota via `jobfit.rate-limit.max-daily-scans` in `application.properties` (takes effect on restart).

Upgrade Button Thresholds
-------------------------
- The “Upgrade CV” call-to-action only appears when the score lands between `jobfit.upgrade-button.lower-score` and `jobfit.upgrade-button.upper-score`.
- Defaults are 75–85, but you can raise/lower either value in `application.properties` to widen or narrow the band.
- The application validates that the lower bound is strictly less than the upper bound during startup and will skip showing the button if the configuration is invalid.

REST API
--------
Endpoint: `POST /score`

Form parameters:
- `candidateFile` – Candidate CV PDF (required)
- `jobDescriptionFile` – Job description PDF (required)
- `analysisMode` – Optional (`quick` default, `thoughtful` for GPT‑5)

Sample request:
```bash
curl -X POST http://localhost:8080/score \
  -F candidateFile=@cv.pdf \
  -F jobDescriptionFile=@job.pdf \
  -F analysisMode=thoughtful
```

Sample response:
```json
{
  "score": 82,
  "explanation": "Strong alignment on product execution ... gaps in cloud security experience."
}
```

Project Structure
-----------------
```
src/main/java/com/milton/agent/
├── controller/
│   ├── AnalysisController.java        # Home page + score generation
│   ├── UpgradeCvController.java       # CV upgrade flow + downloads
│   ├── RecommendationsController.java # Suggestions, improve, and interview prep pages
│   ├── NavigationController.java      # Login/register/dashboard navigation
│   ├── SessionController.java         # Session management endpoints
│   └── JobFitProviderController.java  # REST /score endpoint
├── service/
│   ├── JobFitProviderAgent.java       # Production Embabel agent
│   ├── MockJobFitProviderAgent.java   # Dev agent with hard-coded responses
│   ├── RateLimitService.java          # 10 scans/day rate limiter
│   ├── TextExtractor.java             # PDF text extraction abstraction
│   ├── PdfService.java                # PDF rendering + download naming
│   └── MatchPresentationService.java  # Score display helpers
├── models/                            # Records such as FitScore, JobFitRequest, etc.
└── config/prompts/                    # Prompt templates used by each agent action
```

Development Notes
-----------------
- **Profiles**: Run with `SPRING_PROFILES_ACTIVE=dev` to bypass OpenAI calls while building UI flows; the mock agent returns static data.
- **Prompts**: `src/main/resources/prompts/` contains `skills-extractor.txt`, `job-description-extractor.txt`, `jobfit-fit-score.txt`, and `cv-rewriter.txt`. Modify these when you want to tweak model behavior.
- **Upgrade PDFs**: The PDF download is rendered on the fly using PDFBox, stripping non-ASCII characters for compatibility.
- **Cleaning sessions**: `/clear-cv` POST removes cached CV data from the session.

Testing & Packaging
-------------------
- Run tests: `./mvnw test`
- Build jar: `./mvnw clean package`
- The generated artifact lives in `target/` (default `agent-0.0.1-SNAPSHOT.jar`).

Troubleshooting
---------------
- **Missing Java runtime**: Ensure JDK 21+ is installed; `java -version` should report the correct version.
- **OpenAI authentication**: 401 errors usually mean the `OPENAI_API_KEY` env var is missing or invalid.
- **Rate limit hit immediately**: Clear the in-memory counter by restarting the app or calling `RateLimitService.resetIp(...)` from a REPL/test.
- **File validation errors**: The controllers enforce PDF uploads; any other MIME type triggers a validation message.

License
-------
All rights reserved. This software is provided under a commercial End User License Agreement (EULA).  

Key terms:
- A paid license is required for any production, hosted, or client-facing use.
- Redistribution, sublicensing, or code modification for resale is prohibited.
- Evaluation or internal prototyping is permitted only after obtaining written approval.
- You must keep Embabel/API keys confidential and comply with all third-party model terms.

To obtain a commercial license or discuss partnership terms, contact **Chikere Ezeh – chikere@gmail.com**. Use of this repository without a signed agreement is strictly forbidden.
