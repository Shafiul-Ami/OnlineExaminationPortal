# ExamSphere: Online Examination Portal

An online MCQ examination portal built with **Java + PostgreSQL** and a hand-crafted HTML/CSS/JS frontend.
Students take timed tests subject-wise or topic-wise and get their result immediately. Admins create and manage
questions subject-wise and topic-wise.

A **Jakarta Servlet web app for Apache Tomcat 10.1+** (same setup as a classic Tomcat project): no Maven needed,
`build.ps1` compiles everything into `OnlineExaminationPortal.war` and deploys it to Tomcat.

## Run it on Tomcat (local server)

Requirements: **JDK 17+**, **Apache Tomcat 10.1+** and **PostgreSQL** running locally.

1. Copy `src/main/resources/app.properties.example` to `app.properties` (same folder) and set `DB_PASSWORD`.
   `app.properties` is git-ignored, so your password is never pushed. (`build.ps1` creates the copy for you if it is missing.)
2. In VS Code, check the `CATALINA_HOME` / `JAVA_HOME` paths at the top of `.vscode/tasks.json`.
3. Build, deploy and start:
   - **VS Code:** press **F5** ("Debug on Tomcat"), or *Terminal → Run Task → Tomcat: Build, Deploy & Start*
   - **Terminal:** `powershell -ExecutionPolicy Bypass -File build.ps1` (copies the WAR to `%CATALINA_HOME%webapps` when `CATALINA_HOME` is set), then start Tomcat with `%CATALINA_HOME%instartup.bat`
4. Open **http://localhost:8080/OnlineExaminationPortal/**

After changing code, run *Tomcat: Build & Deploy* again; Tomcat reloads the new WAR automatically.

On first start the app automatically:
- creates the `exam_portal` database and all tables,
- creates the admin account (`admin@exam.com` / `Admin@123` by default; change `ADMIN_EMAIL` / `ADMIN_PASSWORD` in `app.properties` before the first start),
- loads 4 sample subjects with 37 questions (set `SEED_SAMPLE=false` to skip).

## Put it online (Render)

`Dockerfile` + `render.yaml` deploy the app on Tomcat with a free PostgreSQL database:
on render.com choose **New → Blueprint**, pick this GitHub repository and click **Apply**.
The site is then served at the root of your Render URL; the admin password is generated for you
(Render dashboard → the web service → *Environment* → `ADMIN_PASSWORD`).

## Features

**Students**
- Register / log in (passwords hashed with PBKDF2, HttpOnly session cookie)
- Pick a subject → all topics or one topic → number of questions (1 minute per question)
- Exam screen with countdown timer, auto-submit, question palette, mark-for-review, keyboard shortcuts (A–D / 1–4, ← →, M)
- Answers survive a page refresh, and an unfinished test can be resumed
- Instant result: animated score ring, correct/wrong/skipped, time taken, topic-wise breakdown,
  full answer review with explanations, print view
- Dashboard with averages, best score, subject-wise performance, and history

**Admin**
- Dashboard: students, questions, tests taken, average score, charts per subject, difficulty mix, latest submissions
- Subjects & topics: create / edit / delete with colour themes
- Questions: create / edit / delete, filter by subject, topic, difficulty, search, pagination
- Bulk import questions from CSV
- Results of all students (filter, search, export to CSV) and per-student result drill-down
- Students list with stats, plus delete

**Fair evaluation:** correct answers are never sent to the browser during a test. The server picks the questions,
stores the attempt, and grades it on submit.

## CSV import format

```
question,optionA,optionB,optionC,optionD,correct,explanation,difficulty
"What is 2 + 2?",3,4,5,6,B,"Basic addition",EASY
```
`explanation` and `difficulty` (EASY / MEDIUM / HARD) are optional; the header row is optional.

## Project structure

```
build.ps1                       compile + package WAR + deploy to Tomcat
lib/                            jakarta.servlet-api (compile only, Tomcat provides it) + PostgreSQL JDBC driver
Dockerfile, render.yaml         online hosting on Tomcat
src/main/java/com/onlineexam/
  listener/AppInitListener.java   on startup: create DB + tables, admin account, sample questions
  http/ApiServlet.java            @WebServlet("/api/*") - single entry point for the REST API
  http/Router.java                matches METHOD + /path/{param}, checks login/role, writes JSON
  http/Request.java, Json.java, ApiException.java
  api/AuthApi.java                /api/auth/*
  api/CatalogApi.java             /api/subjects, /api/stats (public)
  api/ExamApi.java                /api/exams/* (start, submit, result, history)
  api/AdminApi.java               /api/admin/* (subjects, topics, questions, import, results, students)
  auth/                           password hashing (PBKDF2), DB-backed sessions, roles
  db/Db.java, db/Schema.java      JDBC helper, tables, seed data
  util/AppConfig.java             settings: environment variables -> app.properties -> defaults
src/main/resources/
  app.properties.example          copy to app.properties (git-ignored)
src/main/webapp/
  WEB-INF/web.xml
  index.html, login.html, register.html, result.html, 404.jsp
  student/                        dashboard, exams (picker), test (exam screen), history
  admin/                          dashboard, subjects, questions, results, students
  css/style.css                   design system (light + dark theme)
  js/app.js                       API client, navbar, modals, toasts, icons, context-path handling
```

## Database tables

`users`, `sessions`, `subjects`, `topics`, `questions`, `attempts`, `attempt_answers`.
