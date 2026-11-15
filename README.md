# Engineering Agent - Conversational MVP

Java 21 / Maven / Spring Boot / Spring AI / SQLite. This version provides chat, durable conversation history, knowledge retrieval from a separate Git checkout and independently generated agent interpretations. It does **not** yet implement autonomous code execution, GitHub webhooks, approvals or CI/CD.

## Prerequisites

- JDK 21 and Maven 3.9+
- An OpenAI API key with API billing enabled
- Git for the knowledge repository

## Setup

1. Rename the supplied `gitignore` file to `.gitignore`.
2. Remove the original archetype files `src/main/java/com/greenpalmsolutions/agents/App.java` and `src/test/java/com/greenpalmsolutions/agents/AppTest.java` if they exist.
3. Create a **separate private GitHub repository** for architecture knowledge, under the same GitHub account as this application. Suggested name: `architecture-knowledge`.
4. In that repository, create `authoritative/`, `interpretations/human/`, `interpretations/agent/` and `decisions/` with a README in each directory. Authoritative sources should contain attributed notes/links, not unauthorized copies of books. Human interpretations and decisions are human-controlled.
5. Clone the knowledge repository into `workspace/architecture-knowledge`:

   `git clone git@github.com:YOUR_ACCOUNT/architecture-knowledge.git workspace/architecture-knowledge`

6. Export your API key (never commit it):

   `export OPENAI_API_KEY='your-key'`

7. Build and run:

   `mvn clean test`

   `mvn spring-boot:run`

8. Open http://localhost:8080.

## Configuration

`OPENAI_MODEL` defaults to `gpt-4.1-mini`. `AGENT_DATABASE` defaults to `data/agent.db`. `AGENT_KNOWLEDGE_PATH` defaults to `workspace/architecture-knowledge`. `PORT` defaults to 8080. Set environment variables to override.

## Knowledge ownership and safety

- The app reads Markdown documents from the local checkout, selecting up to eight relevant files for each request. This is a basic keyword retrieval MVP, not vector search or exhaustive scholarly retrieval.
- Only `interpretations/agent/*.md` can be created through the app. Existing files are not overwritten. It **does not automatically commit or push** these files.
- Protect the authoritative, human and decisions directories with GitHub reviews / CODEOWNERS and enforce write restrictions when adding GitHub automation. Path ownership is not secured merely by prompting the LLM.
- Generated interpretations are working hypotheses, not model training or permanent model beliefs. Review them before committing.
- This local app has **no authentication**. Bind it to localhost or use a trusted private network; do not expose it directly to the internet.
- SQLite stores conversation content in plain text. Back up `data/agent.db` securely and avoid putting secrets in chat.
- Each model call sends selected history and knowledge excerpts to the configured external LLM API. Monitor usage and provider billing; hard caps and per-task budgets are later milestones.

## REST API

- `POST /api/chat` — `{ "conversationId": "...", "message": "..." }`
- `POST /api/interpret` — same body, but asks for an independent interpretation
- `POST /api/knowledge/interpretations` — `{ "conversationId": "...", "title": "...", "question": "..." }`
- `GET /api/conversations` — recent conversation IDs
- `GET /api/conversations/{id}` — stored messages
- `GET /api/knowledge/status` — knowledge checkout status