package com.onlineexam.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.onlineexam.auth.Role;
import com.onlineexam.db.Db;
import com.onlineexam.http.ApiException;
import com.onlineexam.http.Request;
import com.onlineexam.http.Router;

/** Admin-only management of subjects, topics, questions, results and students. */
public final class AdminApi {
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final int PAGE_SIZE = 10;

    private AdminApi() {}

    public static void register(Router r) {
        Role A = Role.ADMIN;
        r.role(A, "GET", "/api/admin/stats", AdminApi::stats);

        r.role(A, "POST", "/api/admin/subjects", AdminApi::createSubject);
        r.role(A, "PUT", "/api/admin/subjects/{id}", AdminApi::updateSubject);
        r.role(A, "DELETE", "/api/admin/subjects/{id}", req -> deleted(Db.update("delete from subjects where id = ?", req.pathLong("id")), "Subject"));

        r.role(A, "POST", "/api/admin/topics", AdminApi::createTopic);
        r.role(A, "PUT", "/api/admin/topics/{id}", AdminApi::updateTopic);
        r.role(A, "DELETE", "/api/admin/topics/{id}", req -> deleted(Db.update("delete from topics where id = ?", req.pathLong("id")), "Topic"));

        r.role(A, "GET", "/api/admin/questions", AdminApi::listQuestions);
        r.role(A, "GET", "/api/admin/questions/{id}", AdminApi::getQuestion);
        r.role(A, "POST", "/api/admin/questions", AdminApi::createQuestion);
        r.role(A, "PUT", "/api/admin/questions/{id}", AdminApi::updateQuestion);
        r.role(A, "DELETE", "/api/admin/questions/{id}", req -> deleted(Db.update("delete from questions where id = ?", req.pathLong("id")), "Question"));
        r.role(A, "POST", "/api/admin/questions/import", AdminApi::importQuestions);

        r.role(A, "GET", "/api/admin/attempts", AdminApi::listAttempts);
        r.role(A, "GET", "/api/admin/students", AdminApi::listStudents);
        r.role(A, "DELETE", "/api/admin/students/{id}", req ->
                deleted(Db.update("delete from users where id = ? and role = 'STUDENT'", req.pathLong("id")), "Student"));
    }

    private static Object deleted(int rows, String what) {
        if (rows == 0) throw ApiException.notFound(what);
        return Map.of("ok", true);
    }

    // ---------------- dashboard ----------------

    private static Object stats(Request req) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("students", Db.count("select count(*) from users where role = 'STUDENT'"));
        m.put("subjects", Db.count("select count(*) from subjects"));
        m.put("topics", Db.count("select count(*) from topics"));
        m.put("questions", Db.count("select count(*) from questions"));
        m.put("attempts", Db.count("select count(*) from attempts where status = 'SUBMITTED'"));
        m.put("avgScore", Db.one("select coalesce(round(avg(score_percent), 1), 0) as v from attempts where status = 'SUBMITTED'").get("v"));
        m.put("bySubject", Db.query("""
                select s.id, s.name, s.color,
                       (select count(*) from questions q join topics t on t.id = q.topic_id where t.subject_id = s.id) as question_count,
                       (select count(*) from attempts a where a.subject_id = s.id and a.status = 'SUBMITTED') as attempt_count,
                       (select round(avg(a.score_percent), 1) from attempts a where a.subject_id = s.id and a.status = 'SUBMITTED') as avg_score
                from subjects s order by s.name
                """));
        m.put("byDifficulty", Db.query("select difficulty, count(*) as count from questions group by difficulty"));
        m.put("recent", Db.query("""
                select a.id, u.name as student_name, a.subject_name, a.topic_name, a.score_percent,
                       a.correct_count, a.total_questions, a.submitted_at
                from attempts a join users u on u.id = a.user_id
                where a.status = 'SUBMITTED'
                order by a.submitted_at desc limit 8
                """));
        return m;
    }

    // ---------------- subjects & topics ----------------

    private static String color(Request req) {
        String c = req.str("color", false, 7);
        if (c == null) return "#6366f1";
        if (!c.matches("#[0-9a-fA-F]{6}")) throw ApiException.badRequest("Color must look like #6366f1");
        return c;
    }

    private static Object createSubject(Request req) throws Exception {
        long id = Db.insert("insert into subjects(name, description, color) values (?, ?, ?) returning id",
                req.str("name", true, 100), req.str("description", false, 500), color(req));
        return Map.of("id", id);
    }

    private static Object updateSubject(Request req) throws Exception {
        int n = Db.update("update subjects set name = ?, description = ?, color = ? where id = ?",
                req.str("name", true, 100), req.str("description", false, 500), color(req), req.pathLong("id"));
        return deleted(n, "Subject");
    }

    private static Object createTopic(Request req) throws Exception {
        long subjectId = req.num("subjectId", true);
        if (Db.one("select id from subjects where id = ?", subjectId) == null) throw ApiException.notFound("Subject");
        long id = Db.insert("insert into topics(subject_id, name, description) values (?, ?, ?) returning id",
                subjectId, req.str("name", true, 100), req.str("description", false, 500));
        return Map.of("id", id);
    }

    private static Object updateTopic(Request req) throws Exception {
        int n = Db.update("update topics set name = ?, description = ? where id = ?",
                req.str("name", true, 100), req.str("description", false, 500), req.pathLong("id"));
        return deleted(n, "Topic");
    }

    // ---------------- questions ----------------

    private static Object listQuestions(Request req) throws Exception {
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> params = new ArrayList<>();
        Long subjectId = req.queryLong("subjectId");
        Long topicId = req.queryLong("topicId");
        String difficulty = req.query.get("difficulty");
        String q = req.query.get("q");
        if (subjectId != null) { where.append(" and t.subject_id = ?"); params.add(subjectId); }
        if (topicId != null) { where.append(" and q.topic_id = ?"); params.add(topicId); }
        if (difficulty != null && DIFFICULTIES.contains(difficulty)) { where.append(" and q.difficulty = ?"); params.add(difficulty); }
        if (q != null && !q.isBlank()) { where.append(" and q.question_text ilike ?"); params.add("%" + q.trim() + "%"); }

        String from = " from questions q join topics t on t.id = q.topic_id join subjects s on s.id = t.subject_id";
        long total = Db.count("select count(*)" + from + where, params.toArray());
        long page = Math.max(1, req.queryLong("page") == null ? 1 : req.queryLong("page"));
        long pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.min(page, pages);

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(PAGE_SIZE);
        pageParams.add((page - 1) * PAGE_SIZE);
        List<Map<String, Object>> items = Db.query("""
                select q.id, q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.correct_option,
                       q.explanation, q.difficulty, q.topic_id, t.name as topic_name, t.subject_id,
                       s.name as subject_name, s.color as subject_color, q.updated_at
                """ + from + where + " order by q.id desc limit ? offset ?", pageParams.toArray());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", items);
        m.put("total", total);
        m.put("page", page);
        m.put("pages", pages);
        return m;
    }

    private static Object getQuestion(Request req) throws Exception {
        Map<String, Object> q = Db.one("""
                select q.*, t.subject_id from questions q join topics t on t.id = q.topic_id where q.id = ?
                """, req.pathLong("id"));
        if (q == null) throw ApiException.notFound("Question");
        return q;
    }

    private record QuestionInput(long topicId, String text, String a, String b, String c, String d,
                                 String correct, String explanation, String difficulty) {}

    private static QuestionInput readQuestion(Request req) throws Exception {
        long topicId = req.num("topicId", true);
        if (Db.one("select id from topics where id = ?", topicId) == null) throw ApiException.notFound("Topic");
        String correct = req.str("correctOption", true, 1).toUpperCase();
        if (!"ABCD".contains(correct)) throw ApiException.badRequest("Correct option must be A, B, C or D");
        String difficulty = req.str("difficulty", false, 10);
        difficulty = difficulty == null ? "MEDIUM" : difficulty.toUpperCase();
        if (!DIFFICULTIES.contains(difficulty)) throw ApiException.badRequest("Difficulty must be EASY, MEDIUM or HARD");
        return new QuestionInput(topicId, req.str("questionText", true, 2000),
                req.str("optionA", true, 500), req.str("optionB", true, 500),
                req.str("optionC", true, 500), req.str("optionD", true, 500),
                correct, req.str("explanation", false, 2000), difficulty);
    }

    private static Object createQuestion(Request req) throws Exception {
        QuestionInput q = readQuestion(req);
        long id = Db.insert("""
                insert into questions(topic_id, question_text, option_a, option_b, option_c, option_d, correct_option, explanation, difficulty)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id
                """, q.topicId, q.text, q.a, q.b, q.c, q.d, q.correct, q.explanation, q.difficulty);
        return Map.of("id", id);
    }

    private static Object updateQuestion(Request req) throws Exception {
        QuestionInput q = readQuestion(req);
        int n = Db.update("""
                update questions set topic_id = ?, question_text = ?, option_a = ?, option_b = ?, option_c = ?, option_d = ?,
                       correct_option = ?, explanation = ?, difficulty = ?, updated_at = now()
                where id = ?
                """, q.topicId, q.text, q.a, q.b, q.c, q.d, q.correct, q.explanation, q.difficulty, req.pathLong("id"));
        return deleted(n, "Question");
    }

    /** CSV columns: question, optionA, optionB, optionC, optionD, correct(A-D), explanation?, difficulty? */
    private static Object importQuestions(Request req) throws Exception {
        long topicId = req.num("topicId", true);
        if (Db.one("select id from topics where id = ?", topicId) == null) throw ApiException.notFound("Topic");
        String csv = req.str("csv", true, 1_000_000);

        List<List<String>> rows = parseCsv(csv);
        List<Map<String, Object>> errors = new ArrayList<>();
        List<String[]> valid = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            int line = i + 1;
            if (r.stream().allMatch(String::isBlank)) continue;
            if (i == 0 && r.get(0).trim().equalsIgnoreCase("question")) continue; // header row
            if (r.size() < 6) { errors.add(Map.of("line", line, "message", "Expected at least 6 columns")); continue; }
            String correct = r.get(5).trim().toUpperCase();
            if (correct.length() != 1 || !"ABCD".contains(correct)) {
                errors.add(Map.of("line", line, "message", "Correct option must be A, B, C or D")); continue;
            }
            boolean blank = false;
            for (int k = 0; k < 5; k++) if (r.get(k).isBlank()) blank = true;
            if (blank) { errors.add(Map.of("line", line, "message", "Question and all four options are required")); continue; }
            String diff = r.size() > 7 && !r.get(7).isBlank() ? r.get(7).trim().toUpperCase() : "MEDIUM";
            if (!DIFFICULTIES.contains(diff)) { errors.add(Map.of("line", line, "message", "Difficulty must be EASY, MEDIUM or HARD")); continue; }
            String expl = r.size() > 6 && !r.get(6).isBlank() ? r.get(6).trim() : null;
            valid.add(new String[]{r.get(0).trim(), r.get(1).trim(), r.get(2).trim(), r.get(3).trim(), r.get(4).trim(), correct, expl, diff});
        }
        Db.tx(c -> {
            for (String[] v : valid) {
                Db.update(c, """
                        insert into questions(topic_id, question_text, option_a, option_b, option_c, option_d, correct_option, explanation, difficulty)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, topicId, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]);
            }
            return null;
        });
        return Map.of("imported", valid.size(), "errors", errors);
    }

    /** RFC-4180-ish CSV parser: handles quoted fields, escaped quotes ("") and newlines inside quotes. */
    static List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                row.add(field.toString()); field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString()); field.setLength(0);
                rows.add(row); row = new ArrayList<>();
            } else {
                field.append(ch);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) { row.add(field.toString()); rows.add(row); }
        return rows;
    }

    // ---------------- results & students ----------------

    private static Object listAttempts(Request req) throws Exception {
        StringBuilder where = new StringBuilder(" where a.status = 'SUBMITTED'");
        List<Object> params = new ArrayList<>();
        Long subjectId = req.queryLong("subjectId");
        Long userId = req.queryLong("userId");
        String q = req.query.get("q");
        if (subjectId != null) { where.append(" and a.subject_id = ?"); params.add(subjectId); }
        if (userId != null) { where.append(" and a.user_id = ?"); params.add(userId); }
        if (q != null && !q.isBlank()) {
            where.append(" and (u.name ilike ? or u.email ilike ?)");
            params.add("%" + q.trim() + "%");
            params.add("%" + q.trim() + "%");
        }
        return Db.query("""
                select a.id, u.name as student_name, u.email as student_email, a.subject_name, a.topic_name,
                       a.total_questions, a.correct_count, a.wrong_count, a.unanswered_count, a.score_percent,
                       a.time_taken_sec, a.submitted_at
                from attempts a join users u on u.id = a.user_id
                """ + where + " order by a.submitted_at desc limit 500", params.toArray());
    }

    private static Object listStudents(Request req) throws Exception {
        return Db.query("""
                select u.id, u.name, u.email, u.created_at,
                       count(a.id) as tests,
                       round(avg(a.score_percent), 1) as avg_score,
                       max(a.score_percent) as best_score,
                       max(a.submitted_at) as last_active
                from users u
                left join attempts a on a.user_id = u.id and a.status = 'SUBMITTED'
                where u.role = 'STUDENT'
                group by u.id
                order by u.created_at desc
                """);
    }
}
