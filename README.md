# ExamSphere: Online Examination Portal

An online MCQ examination portal built with **Java + PostgreSQL** and a hand-crafted HTML/CSS/JS frontend.
Students take timed tests subject-wise or topic-wise and get their result immediately. Admins create and manage
questions subject-wise and topic-wise.

No Maven, no frameworks: it runs on the JDK's built-in HTTP server plus the PostgreSQL JDBC driver in `lib/`.

## Run it

Requirements: **JDK 21+** and **PostgreSQL** (running locally).

1. Copy `config.example.properties` to `config.properties` and set `db.password`
   (the run scripts create the copy for you on first run). `config.properties` is git-ignored, so your password never gets pushed.
2. Start the app:
   - Windows: double-click `run.bat`
   - macOS / Linux / Git Bash: `./run.sh`
   - VS Code: open the folder, install the *Extension Pack for Java*, then press **F5** (uses "Run ExamSphere")
3. Open http://localhost:8080

On first start the app automatically:
- creates the `exam_portal` database and all tables,
- creates the admin account (`admin@exam.com` / `Admin@123` by default; change these in `config.properties`),
- loads 4 sample subjects with 37 questions (set `seed.sample=false` to skip).

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
config.example.properties  template for config.properties (DB + server + admin settings; env vars override, e.g. DB_PASSWORD)
run.bat / run.sh           compile + start
lib/                       PostgreSQL JDBC driver
src/portal/
  Main.java                bootstraps DB and HTTP server
  Config.java
  db/Db.java               JDBC helper (rows -> camelCase maps)
  db/Schema.java           tables, admin seed, sample questions
  auth/                    password hashing, sessions, roles
  http/                    tiny router, JSON parser, static file server
  api/AuthApi.java         /api/auth/*
  api/CatalogApi.java      /api/subjects, /api/stats (public)
  api/ExamApi.java         /api/exams/* (start, submit, result, history)
  api/AdminApi.java        /api/admin/* (subjects, topics, questions, import, results, students)
web/
  index.html               landing page
  login.html, register.html
  result.html              result + answer review (student & admin)
  student/                 dashboard, exams (picker), test (exam screen), history
  admin/                   dashboard, subjects, questions, results, students
  css/style.css            design system (light + dark theme)
  js/app.js                API client, navbar, modals, toasts, icons
```

## Database tables

`users`, `sessions`, `subjects`, `topics`, `questions`, `attempts`, `attempt_answers`.
