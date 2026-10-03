package com.onlineexam.api;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.onlineexam.auth.Role;
import com.onlineexam.db.Db;
import com.onlineexam.http.ApiException;
import com.onlineexam.http.Request;
import com.onlineexam.http.Router;

/**
 * Student test flow. Correct answers never leave the server until the attempt is submitted:
 * start -> paper (questions only) -> submit (server grades) -> result (with answers + explanations).
 */
public final class ExamApi {
    private static final int SECONDS_PER_QUESTION = 60;
    private static final int MAX_QUESTIONS = 100;

    private ExamApi() {}

    public static void register(Router r) {
        r.role(Role.STUDENT, "POST", "/api/exams/start", ExamApi::start);
        r.role(Role.STUDENT, "GET", "/api/exams/mine", ExamApi::mine);
        r.role(Role.STUDENT, "GET", "/api/exams/summary", ExamApi::summary);
        r.role(Role.STUDENT, "GET", "/api/exams/{id}", ExamApi::paper);
        r.role(Role.STUDENT, "POST", "/api/exams/{id}/submit", ExamApi::submit);
        r.authed("GET", "/api/exams/{id}/result", ExamApi::result);
    }

    private static Object start(Request req) throws Exception {
        long subjectId = req.num("subjectId", true);
        Long topicId = req.num("topicId", false);
        long count = req.num("count", true);
        if (count < 1 || count > MAX_QUESTIONS) throw ApiException.badRequest("Number of questions must be between 1 and " + MAX_QUESTIONS);

        Map<String, Object> subject = Db.one("select id, name from subjects where id = ?", subjectId);
        if (subject == null) throw ApiException.notFound("Subject");
        String topicName = null;
        if (topicId != null) {
            Map<String, Object> topic = Db.one("select name from topics where id = ? and subject_id = ?", topicId, subjectId);
            if (topic == null) throw ApiException.notFound("Topic");
            topicName = (String) topic.get("name");
        }
        final String finalTopicName = topicName;

        long attemptId = Db.tx(c -> {
            List<Map<String, Object>> picked = topicId != null
                    ? Db.query(c, "select id from questions where topic_id = ? order by random() limit ?", topicId, count)
                    : Db.query(c, """
                        select q.id from questions q join topics t on t.id = q.topic_id
                        where t.subject_id = ? order by random() limit ?
                        """, subjectId, count);
            if (picked.isEmpty()) throw ApiException.badRequest("No questions available for this selection yet");

            int duration = picked.size() * SECONDS_PER_QUESTION;
            long id = Db.insert(c, """
                    insert into attempts(user_id, subject_id, topic_id, subject_name, topic_name, total_questions, duration_sec)
                    values (?, ?, ?, ?, ?, ?, ?) returning id
                    """, req.user.id(), subjectId, topicId, subject.get("name"), finalTopicName, picked.size(), duration);
            int pos = 1;
            for (Map<String, Object> q : picked) {
                Db.update(c, "insert into attempt_answers(attempt_id, question_id, position) values (?, ?, ?)",
                        id, q.get("id"), pos++);
            }
            return id;
        });
        return loadPaper(attemptId, req.user.id());
    }

    private static Object paper(Request req) throws Exception {
        return loadPaper(req.pathLong("id"), req.user.id());
    }

    private static Map<String, Object> loadPaper(long attemptId, long userId) throws Exception {
        Map<String, Object> a = Db.one("""
                select id, subject_name, topic_name, total_questions, duration_sec, status, started_at,
                       greatest(0, duration_sec - extract(epoch from now() - started_at))::int as remaining_sec
                from attempts where id = ? and user_id = ?
                """, attemptId, userId);
        if (a == null) throw ApiException.notFound("Test");
        if ("IN_PROGRESS".equals(a.get("status"))) {
            a.put("questions", Db.query("""
                    select q.id, q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.difficulty, t.name as topic
                    from attempt_answers aa
                    join questions q on q.id = aa.question_id
                    join topics t on t.id = q.topic_id
                    where aa.attempt_id = ?
                    order by aa.position
                    """, attemptId));
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    private static Object submit(Request req) throws Exception {
        long attemptId = req.pathLong("id");
        Object rawAnswers = req.body().get("answers");
        Map<String, Object> answers = rawAnswers instanceof Map ? (Map<String, Object>) rawAnswers : Map.of();

        Db.tx(c -> {
            Map<String, Object> a = Db.one(c, """
                    select id, status, duration_sec, extract(epoch from now() - started_at)::int as elapsed
                    from attempts where id = ? and user_id = ? for update
                    """, attemptId, req.user.id());
            if (a == null) throw ApiException.notFound("Test");
            if (!"IN_PROGRESS".equals(a.get("status"))) throw new ApiException(409, "This test has already been submitted");

            List<Map<String, Object>> rows = Db.query(c, """
                    select aa.question_id, q.correct_option
                    from attempt_answers aa join questions q on q.id = aa.question_id
                    where aa.attempt_id = ?
                    """, attemptId);
            int correct = 0, wrong = 0, unanswered = 0;
            for (Map<String, Object> row : rows) {
                String qid = String.valueOf(row.get("questionId"));
                String selected = normalizeOption(answers.get(qid));
                Boolean isCorrect = null;
                if (selected == null) {
                    unanswered++;
                } else {
                    isCorrect = selected.equals(String.valueOf(row.get("correctOption")).trim());
                    if (isCorrect) correct++; else wrong++;
                }
                Db.update(c, "update attempt_answers set selected_option = ?, is_correct = ? where attempt_id = ? and question_id = ?",
                        selected, isCorrect, attemptId, row.get("questionId"));
            }
            // Questions deleted by an admin mid-test cascade away; grade against what remains.
            int total = rows.size();
            double score = total == 0 ? 0 : Math.round(correct * 10000.0 / total) / 100.0;
            int duration = ((Number) a.get("durationSec")).intValue();
            int taken = Math.min(duration, ((Number) a.get("elapsed")).intValue());
            Db.update(c, """
                    update attempts set status = 'SUBMITTED', submitted_at = now(), total_questions = ?,
                           correct_count = ?, wrong_count = ?, unanswered_count = ?, score_percent = ?, time_taken_sec = ?
                    where id = ?
                    """, total, correct, wrong, unanswered, score, taken, attemptId);
            return null;
        });
        return buildResult(attemptId);
    }

    private static String normalizeOption(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim().toUpperCase();
        return s.length() == 1 && "ABCD".contains(s) ? s : null;
    }

    private static Object result(Request req) throws Exception {
        long attemptId = req.pathLong("id");
        Map<String, Object> owner = Db.one("select user_id, status from attempts where id = ?", attemptId);
        if (owner == null) throw ApiException.notFound("Result");
        boolean isOwner = ((Number) owner.get("userId")).longValue() == req.user.id();
        if (!isOwner && req.user.role() != Role.ADMIN) throw ApiException.forbidden();
        if (!"SUBMITTED".equals(owner.get("status"))) throw new ApiException(409, "This test has not been submitted yet");
        return buildResult(attemptId);
    }

    static Map<String, Object> buildResult(long attemptId) throws Exception {
        try (Connection c = Db.connect()) {
            Map<String, Object> a = Db.one(c, """
                    select a.id, a.subject_name, a.topic_name, a.total_questions, a.correct_count, a.wrong_count,
                           a.unanswered_count, a.score_percent, a.duration_sec, a.time_taken_sec, a.started_at, a.submitted_at,
                           u.name as student_name, u.email as student_email
                    from attempts a join users u on u.id = a.user_id
                    where a.id = ?
                    """, attemptId);
            List<Map<String, Object>> review = Db.query(c, """
                    select q.id, q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.correct_option,
                           q.explanation, q.difficulty, t.name as topic, aa.selected_option, aa.is_correct
                    from attempt_answers aa
                    join questions q on q.id = aa.question_id
                    join topics t on t.id = q.topic_id
                    where aa.attempt_id = ?
                    order by aa.position
                    """, attemptId);
            a.put("review", review);

            Map<String, int[]> byTopic = new LinkedHashMap<>();
            for (Map<String, Object> q : review) {
                int[] t = byTopic.computeIfAbsent((String) q.get("topic"), k -> new int[2]);
                t[1]++;
                if (Boolean.TRUE.equals(q.get("isCorrect"))) t[0]++;
            }
            List<Map<String, Object>> breakdown = new ArrayList<>();
            byTopic.forEach((topic, t) -> breakdown.add(Map.of("topic", topic, "correct", t[0], "total", t[1])));
            a.put("topicBreakdown", breakdown);
            return a;
        }
    }

    private static Object mine(Request req) throws Exception {
        return Db.query("""
                select id, subject_name, topic_name, total_questions, correct_count, wrong_count, unanswered_count,
                       score_percent, time_taken_sec, status, started_at, submitted_at
                from attempts where user_id = ?
                order by started_at desc
                """, req.user.id());
    }

    private static Object summary(Request req) throws Exception {
        long uid = req.user.id();
        Map<String, Object> totals = Db.one("""
                select count(*) as tests,
                       coalesce(round(avg(score_percent), 1), 0) as avg_score,
                       coalesce(max(score_percent), 0) as best_score,
                       coalesce(sum(correct_count), 0) as correct_answers,
                       coalesce(sum(total_questions), 0) as questions_answered
                from attempts where user_id = ? and status = 'SUBMITTED'
                """, uid);
        Map<String, Object> out = new HashMap<>(totals);
        out.put("bySubject", Db.query("""
                select subject_name, count(*) as tests, round(avg(score_percent), 1) as avg_score
                from attempts where user_id = ? and status = 'SUBMITTED'
                group by subject_name order by avg_score desc
                """, uid));
        out.put("recent", Db.query("""
                select id, subject_name, topic_name, score_percent, correct_count, total_questions, submitted_at
                from attempts where user_id = ? and status = 'SUBMITTED'
                order by submitted_at desc limit 5
                """, uid));
        out.put("inProgress", Db.query("""
                select id, subject_name, topic_name, total_questions
                from attempts where user_id = ? and status = 'IN_PROGRESS'
                  and started_at + make_interval(secs => duration_sec) > now()
                order by started_at desc
                """, uid));
        return out;
    }
}
