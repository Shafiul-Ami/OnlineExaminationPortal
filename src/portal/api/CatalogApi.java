package portal.api;

import java.util.LinkedHashMap;
import java.util.Map;
import portal.db.Db;
import portal.http.ApiException;
import portal.http.Router;

/** Public, read-only endpoints: subjects, topics and headline stats for the landing page. */
public final class CatalogApi {
    private CatalogApi() {}

    public static void register(Router r) {
        r.get("/api/subjects", req -> Db.query("""
                select s.id, s.name, s.description, s.color,
                       (select count(*) from topics t where t.subject_id = s.id) as topic_count,
                       (select count(*) from questions q join topics t on t.id = q.topic_id where t.subject_id = s.id) as question_count
                from subjects s
                order by s.name
                """));

        r.get("/api/subjects/{id}", req -> {
            long id = req.pathLong("id");
            Map<String, Object> subject = Db.one("select id, name, description, color from subjects where id = ?", id);
            if (subject == null) throw ApiException.notFound("Subject");
            subject.put("topics", Db.query("""
                    select t.id, t.name, t.description,
                           (select count(*) from questions q where q.topic_id = t.id) as question_count
                    from topics t where t.subject_id = ?
                    order by t.name
                    """, id));
            return subject;
        });

        r.get("/api/stats", req -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subjects", Db.count("select count(*) from subjects"));
            m.put("questions", Db.count("select count(*) from questions"));
            m.put("students", Db.count("select count(*) from users where role = 'STUDENT'"));
            m.put("testsTaken", Db.count("select count(*) from attempts where status = 'SUBMITTED'"));
            return m;
        });
    }
}
