package portal.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import portal.auth.Passwords;

/** Creates the database and tables on first run, and seeds the admin account plus sample questions. */
public final class Schema {
    private Schema() {}

    /** Connects to the default "postgres" database and creates the portal database if it is missing. */
    public static void ensureDatabase(String host, int port, String database, String user, String password) throws SQLException {
        if (!database.matches("[a-zA-Z_][a-zA-Z0-9_]*")) throw new IllegalArgumentException("Invalid database name: " + database);
        String url = "jdbc:postgresql://" + host + ":" + port + "/postgres";
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            boolean exists = !Db.query(c, "select 1 from pg_database where datname = ?", database).isEmpty();
            if (!exists) {
                try (Statement st = c.createStatement()) {
                    st.execute("create database " + database);
                }
                System.out.println("[db] created database " + database);
            }
        }
    }

    private static final String DDL = """
            create table if not exists users (
                id            bigserial primary key,
                name          varchar(100) not null,
                email         varchar(150) not null unique,
                password_hash text not null,
                role          varchar(10)  not null check (role in ('ADMIN', 'STUDENT')),
                created_at    timestamptz  not null default now()
            );

            create table if not exists sessions (
                token      varchar(64) primary key,
                user_id    bigint not null references users(id) on delete cascade,
                created_at timestamptz not null default now(),
                expires_at timestamptz not null
            );

            create table if not exists subjects (
                id          bigserial primary key,
                name        varchar(100) not null unique,
                description varchar(500),
                color       varchar(7)   not null default '#6366f1',
                created_at  timestamptz  not null default now()
            );

            create table if not exists topics (
                id          bigserial primary key,
                subject_id  bigint not null references subjects(id) on delete cascade,
                name        varchar(100) not null,
                description varchar(500),
                created_at  timestamptz not null default now(),
                unique (subject_id, name)
            );

            create table if not exists questions (
                id             bigserial primary key,
                topic_id       bigint not null references topics(id) on delete cascade,
                question_text  text not null,
                option_a       text not null,
                option_b       text not null,
                option_c       text not null,
                option_d       text not null,
                correct_option char(1) not null check (correct_option in ('A', 'B', 'C', 'D')),
                explanation    text,
                difficulty     varchar(10) not null default 'MEDIUM' check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
                created_at     timestamptz not null default now(),
                updated_at     timestamptz not null default now()
            );
            create index if not exists idx_questions_topic on questions(topic_id);

            create table if not exists attempts (
                id               bigserial primary key,
                user_id          bigint not null references users(id) on delete cascade,
                subject_id       bigint references subjects(id) on delete set null,
                topic_id         bigint references topics(id) on delete set null,
                subject_name     varchar(100) not null,
                topic_name       varchar(100),
                total_questions  int not null,
                correct_count    int not null default 0,
                wrong_count      int not null default 0,
                unanswered_count int not null default 0,
                score_percent    numeric(5, 2) not null default 0,
                duration_sec     int not null,
                time_taken_sec   int,
                status           varchar(12) not null default 'IN_PROGRESS' check (status in ('IN_PROGRESS', 'SUBMITTED')),
                started_at       timestamptz not null default now(),
                submitted_at     timestamptz
            );
            create index if not exists idx_attempts_user on attempts(user_id);

            create table if not exists attempt_answers (
                attempt_id      bigint not null references attempts(id) on delete cascade,
                question_id     bigint not null references questions(id) on delete cascade,
                position        int not null,
                selected_option char(1),
                is_correct      boolean,
                primary key (attempt_id, question_id)
            );
            """;

    public static void migrate() throws SQLException {
        try (Connection c = Db.connect(); Statement st = c.createStatement()) {
            st.execute(DDL);
            st.execute("delete from sessions where expires_at < now()");
        }
    }

    public static void seedAdmin(String name, String email, String password) throws SQLException {
        if (Db.count("select count(*) from users where role = 'ADMIN'") > 0) return;
        Db.update("insert into users(name, email, password_hash, role) values (?, ?, ?, 'ADMIN')",
                name, email.toLowerCase(), Passwords.hash(password));
        System.out.println("[db] created admin account " + email);
    }

    // subject, colour, description
    private static final String[][] SUBJECTS = {
            {"Java Programming", "#6366f1", "Core Java, object-oriented programming, collections and exception handling."},
            {"Databases & SQL", "#0ea5e9", "Relational databases, SQL queries, keys, joins and PostgreSQL essentials."},
            {"Mathematics", "#f59e0b", "Arithmetic, percentages and algebra for aptitude tests."},
            {"Computer Fundamentals", "#10b981", "Hardware, operating systems and computer networking basics."},
    };

    // subject, topic, question, A, B, C, D, correct, explanation, difficulty
    private static final String[][] QUESTIONS = {
            {"Java Programming", "OOP Concepts", "Which keyword is used to inherit a class in Java?", "implements", "extends", "inherits", "super", "B", "A class extends another class; it implements interfaces.", "EASY"},
            {"Java Programming", "OOP Concepts", "Which OOP principle hides internal state behind methods?", "Inheritance", "Polymorphism", "Encapsulation", "Abstraction", "C", "Encapsulation bundles data with methods and restricts direct access using private fields.", "EASY"},
            {"Java Programming", "OOP Concepts", "Method overloading is an example of…", "Runtime polymorphism", "Compile-time polymorphism", "Multiple inheritance", "Encapsulation", "B", "Overloaded methods are resolved by the compiler based on parameter lists.", "MEDIUM"},
            {"Java Programming", "OOP Concepts", "Can an abstract class have a constructor?", "No, never", "Only if it has no abstract methods", "Yes", "Only a private one", "C", "Abstract classes can have constructors; they run when a subclass is instantiated.", "MEDIUM"},
            {"Java Programming", "OOP Concepts", "Which access modifier makes a member visible only within its own class?", "public", "protected", "default", "private", "D", "private restricts access to the declaring class.", "EASY"},
            {"Java Programming", "Collections & Generics", "Which collection does NOT allow duplicate elements?", "ArrayList", "LinkedList", "HashSet", "Vector", "C", "Set implementations such as HashSet store unique elements only.", "EASY"},
            {"Java Programming", "Collections & Generics", "Which Map implementation keeps keys in sorted order?", "HashMap", "TreeMap", "LinkedHashMap", "Hashtable", "B", "TreeMap is a red-black tree that orders keys by natural ordering or a Comparator.", "MEDIUM"},
            {"Java Programming", "Collections & Generics", "What is the average time complexity of HashMap.get()?", "O(1)", "O(log n)", "O(n)", "O(n log n)", "A", "Hash-based lookup is constant time on average.", "MEDIUM"},
            {"Java Programming", "Collections & Generics", "What does `List<? extends Number>` allow you to do safely?", "Add any Number", "Read elements as Number", "Add Integers only", "Nothing at all", "B", "With an upper-bounded wildcard you can read as Number but cannot add (except null).", "HARD"},
            {"Java Programming", "Exceptions", "Which of these is a checked exception?", "NullPointerException", "ArithmeticException", "IOException", "ArrayIndexOutOfBoundsException", "C", "IOException must be declared or caught; the others are unchecked RuntimeExceptions.", "EASY"},
            {"Java Programming", "Exceptions", "When does a finally block NOT run?", "When an exception is thrown", "When return is called in try", "When System.exit() is called", "When catch rethrows", "C", "System.exit() halts the JVM, so finally is skipped.", "MEDIUM"},
            {"Java Programming", "Exceptions", "try-with-resources requires the resource to implement…", "Serializable", "AutoCloseable", "Runnable", "Iterable", "B", "Resources must implement AutoCloseable (Closeable extends it).", "MEDIUM"},

            {"Databases & SQL", "SQL Basics", "Which SQL command removes all rows but keeps the table structure?", "DROP", "DELETE without WHERE / TRUNCATE", "ALTER", "REMOVE", "B", "DELETE without WHERE or TRUNCATE empties the table; DROP removes the table itself.", "EASY"},
            {"Databases & SQL", "SQL Basics", "Which clause filters groups after aggregation?", "WHERE", "HAVING", "ORDER BY", "LIMIT", "B", "HAVING filters on aggregated results; WHERE filters rows before grouping.", "MEDIUM"},
            {"Databases & SQL", "SQL Basics", "What does SERIAL do in PostgreSQL?", "Encrypts a column", "Creates an auto-incrementing integer", "Makes a column unique only", "Stores serialized objects", "B", "SERIAL is shorthand for an integer column backed by a sequence.", "EASY"},
            {"Databases & SQL", "SQL Basics", "Which keyword returns the generated id after an INSERT in PostgreSQL?", "OUTPUT", "RETURNING", "SELECT LAST", "GENERATED", "B", "INSERT ... RETURNING id gives back values from the inserted row.", "EASY"},
            {"Databases & SQL", "SQL Basics", "Which aggregate ignores NULL values?", "COUNT(*)", "COUNT(column)", "Both", "Neither", "B", "COUNT(column) skips NULLs; COUNT(*) counts every row.", "HARD"},
            {"Databases & SQL", "Joins & Keys", "Which join returns only rows with matches in both tables?", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "FULL JOIN", "C", "INNER JOIN keeps only matching rows.", "EASY"},
            {"Databases & SQL", "Joins & Keys", "A foreign key ensures…", "Values are unique", "Values exist in the referenced table", "Values are not null", "The column is indexed", "B", "Foreign keys enforce referential integrity.", "EASY"},
            {"Databases & SQL", "Joins & Keys", "A LEFT JOIN with no match fills the right side with…", "Zeros", "Empty strings", "NULLs", "Duplicate rows", "C", "Unmatched columns from the right table are NULL.", "MEDIUM"},
            {"Databases & SQL", "Joins & Keys", "Which normal form removes transitive dependencies?", "1NF", "2NF", "3NF", "BCNF only", "C", "Third normal form eliminates transitive dependencies on the primary key.", "HARD"},

            {"Mathematics", "Arithmetic", "What is 15% of 240?", "32", "36", "38", "40", "B", "0.15 × 240 = 36.", "EASY"},
            {"Mathematics", "Arithmetic", "The average of 4, 8, 12 and 16 is…", "8", "10", "12", "11", "B", "(4+8+12+16)/4 = 40/4 = 10.", "EASY"},
            {"Mathematics", "Arithmetic", "A price rises from 200 to 250. The percentage increase is…", "20%", "25%", "30%", "50%", "B", "Increase 50 on 200 = 25%.", "MEDIUM"},
            {"Mathematics", "Arithmetic", "LCM of 12 and 18 is…", "36", "6", "72", "54", "A", "12 = 2²·3, 18 = 2·3²; LCM = 2²·3² = 36.", "MEDIUM"},
            {"Mathematics", "Algebra", "Solve: 3x + 5 = 20", "x = 3", "x = 5", "x = 6", "x = 15", "B", "3x = 15, so x = 5.", "EASY"},
            {"Mathematics", "Algebra", "(a + b)² equals…", "a² + b²", "a² + 2ab + b²", "a² − 2ab + b²", "2a + 2b", "B", "Expand (a+b)(a+b).", "EASY"},
            {"Mathematics", "Algebra", "Roots of x² − 5x + 6 = 0 are…", "1 and 6", "2 and 3", "−2 and −3", "3 and 4", "B", "x² − 5x + 6 = (x−2)(x−3).", "MEDIUM"},
            {"Mathematics", "Algebra", "If 2^x = 32, then x = ?", "4", "5", "6", "16", "B", "2^5 = 32.", "EASY"},

            {"Computer Fundamentals", "Hardware & OS", "Which memory is volatile?", "ROM", "Hard disk", "RAM", "SSD", "C", "RAM loses its contents when power is removed.", "EASY"},
            {"Computer Fundamentals", "Hardware & OS", "The 'brain' of the computer is the…", "GPU", "CPU", "RAM", "Motherboard", "B", "The CPU executes instructions.", "EASY"},
            {"Computer Fundamentals", "Hardware & OS", "Which scheduling algorithm can cause starvation?", "Round Robin", "FCFS", "Priority scheduling", "None", "C", "Low-priority processes may wait indefinitely under priority scheduling.", "HARD"},
            {"Computer Fundamentals", "Hardware & OS", "1 kilobyte (binary) equals…", "1000 bytes", "1024 bytes", "512 bytes", "2048 bytes", "B", "1 KiB = 2^10 = 1024 bytes.", "EASY"},
            {"Computer Fundamentals", "Networking", "Which protocol translates domain names to IP addresses?", "HTTP", "FTP", "DNS", "SMTP", "C", "DNS resolves names such as example.com to IP addresses.", "EASY"},
            {"Computer Fundamentals", "Networking", "Default port for HTTPS is…", "80", "21", "443", "8080", "C", "HTTPS uses port 443 by default.", "EASY"},
            {"Computer Fundamentals", "Networking", "TCP differs from UDP because TCP is…", "Connectionless", "Faster with no overhead", "Connection-oriented and reliable", "Used only for video", "C", "TCP establishes a connection and guarantees ordered delivery.", "MEDIUM"},
            {"Computer Fundamentals", "Networking", "How many layers does the OSI model have?", "4", "5", "7", "8", "C", "Physical, Data Link, Network, Transport, Session, Presentation, Application.", "EASY"},
    };

    public static void seedSampleData() throws Exception {
        if (Db.count("select count(*) from subjects") > 0) return;
        Db.tx(c -> {
            Map<String, Long> subjectIds = new HashMap<>();
            for (String[] s : SUBJECTS) {
                subjectIds.put(s[0], Db.insert(c,
                        "insert into subjects(name, color, description) values (?, ?, ?) returning id", s[0], s[1], s[2]));
            }
            Map<String, Long> topicIds = new HashMap<>();
            for (String[] q : QUESTIONS) {
                String key = q[0] + "|" + q[1];
                Long topicId = topicIds.get(key);
                if (topicId == null) {
                    topicId = Db.insert(c, "insert into topics(subject_id, name) values (?, ?) returning id", subjectIds.get(q[0]), q[1]);
                    topicIds.put(key, topicId);
                }
                Db.update(c, """
                        insert into questions(topic_id, question_text, option_a, option_b, option_c, option_d,
                                              correct_option, explanation, difficulty)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, topicId, q[2], q[3], q[4], q[5], q[6], q[7], q[8], q[9]);
            }
            return null;
        });
        System.out.println("[db] seeded " + SUBJECTS.length + " subjects and " + QUESTIONS.length + " sample questions");
    }
}
