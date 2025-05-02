package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBUtil {
    private static final String URL = "jdbc:sqlite:cinema_booking.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * 清空所有資料表
     */
    public static void clearDatabase() {
        try (Connection conn = getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS reservation;");
            conn.createStatement().execute("DROP TABLE IF EXISTS showtime;");
            conn.createStatement().execute("DROP TABLE IF EXISTS seat;");
            conn.createStatement().execute("DROP TABLE IF EXISTS theater;");
            conn.createStatement().execute("DROP TABLE IF EXISTS movie;");
            conn.createStatement().execute("DROP TABLE IF EXISTS member;");
            System.out.println("Database cleared successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String createMemberTable = "CREATE TABLE IF NOT EXISTS member (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "email TEXT NOT NULL UNIQUE," +
                        "password TEXT NOT NULL," +
                        "birth_date TEXT NOT NULL" +
                        ");";

                String createMovieTable = "CREATE TABLE IF NOT EXISTS movie (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "duration INTEGER NOT NULL," +
                        "description TEXT," +
                        "rating TEXT NOT NULL" +
                        ");";

                String createTheaterTable = "CREATE TABLE IF NOT EXISTS theater (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "type TEXT NOT NULL UNIQUE," +
                        "total_seats INTEGER NOT NULL" +
                        ");";

                String createShowtimeTable = "CREATE TABLE IF NOT EXISTS showtime (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "movie_uid INTEGER," +
                        "theater_uid INTEGER," +
                        "time TEXT NOT NULL," +
                        "available_seats INTEGER NOT NULL DEFAULT 0," +
                        "FOREIGN KEY (movie_uid) REFERENCES movie(uid) ON DELETE CASCADE," +
                        "FOREIGN KEY (theater_uid) REFERENCES theater(uid) ON DELETE CASCADE" +
                        ");";

                String createReservationTable = "CREATE TABLE IF NOT EXISTS reservation (" +
                        "uid INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "member_uid INTEGER," +
                        "showtime_uid INTEGER," +
                        "seat_no TEXT," +
                        "reservation_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "status TEXT DEFAULT 'CONFIRMED'," +
                        "FOREIGN KEY (member_uid) REFERENCES member(uid) ON DELETE CASCADE," +
                        "FOREIGN KEY (showtime_uid) REFERENCES showtime(uid) ON DELETE CASCADE" +
                        ");";

                conn.createStatement().execute(createMemberTable);
                conn.createStatement().execute(createMovieTable);
                String createMovieNameIndex = "CREATE UNIQUE INDEX IF NOT EXISTS idx_movie_name ON movie(name);";
                conn.createStatement().execute(createMovieNameIndex);
                conn.createStatement().execute(createTheaterTable);
                // conn.createStatement().execute(createSeatTable);
                conn.createStatement().execute(createShowtimeTable);
                conn.createStatement().execute(createReservationTable);

                String insertTheaters = "INSERT OR IGNORE INTO theater (type, total_seats) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertTheaters)) {
                    stmt.setString(1, "Hall A");
                    stmt.setInt(2, 100);
                    stmt.addBatch();

                    stmt.setString(1, "Hall B");
                    stmt.setInt(2, 50);
                    stmt.addBatch();

                    stmt.executeBatch();
                }

                // Insert default movies
                String insertMovies = "INSERT OR IGNORE INTO movie (name, duration, description, rating) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertMovies)) {
                    stmt.setString(1, "Star Wars");
                    stmt.setInt(2, 120);
                    stmt.setString(3, "A space opera about the battle between good and evil.");
                    stmt.setString(4, "PG-13");
                    stmt.addBatch();

                    stmt.setString(1, "Zootopia");
                    stmt.setInt(2, 108);
                    stmt.setString(3, "A city of anthropomorphic animals.");
                    stmt.setString(4, "PG");
                    stmt.addBatch();

                    stmt.setString(1, "Inception");
                    stmt.setInt(2, 148);
                    stmt.setString(3, "A mind-bending thriller about dreams within dreams.");
                    stmt.setString(4, "PG-13");
                    stmt.addBatch();

                    stmt.setString(1, "The Godfather");
                    stmt.setInt(2, 175);
                    stmt.setString(3, "The story of a powerful Italian-American crime family.");
                    stmt.setString(4, "R");
                    stmt.addBatch();

                    stmt.setString(1, "Frozen");
                    stmt.setInt(2, 102);
                    stmt.setString(3, "A magical tale of two sisters in a frozen kingdom.");
                    stmt.setString(4, "PG");
                    stmt.addBatch();

                    stmt.setString(1, "Avengers: Endgame");
                    stmt.setInt(2, 181);
                    stmt.setString(3, "The epic conclusion to the Marvel Cinematic Universe's Infinity Saga.");
                    stmt.setString(4, "PG-13");
                    stmt.addBatch();

                    stmt.executeBatch();
                }

                // 移除舊的 insertAdmin 預設管理員，僅保留以下 upsert:
                String adminUpsert = "INSERT INTO member (email, password, birth_date) VALUES (?, ?, ?) " +
                                     "ON CONFLICT(email) DO UPDATE SET password=excluded.password, birth_date=excluded.birth_date;";
                try (PreparedStatement stmt = conn.prepareStatement(adminUpsert)) {
                    stmt.setString(1, "admin@admin.com");
                    stmt.setString(2, PasswordUtil.encrypt("admin123"));
                    stmt.setString(3, "2000-01-01");
                    stmt.executeUpdate();
                }

                // conn.createStatement().execute("DELETE FROM showtime;");
                // String insertShowtimes = "INSERT INTO showtime (movie_uid, theater_uid, time, available_seats) VALUES " +
                //                       "((SELECT uid FROM movie WHERE name='StarWar'), (SELECT uid FROM theater WHERE type='Hall A'), '2025-05-10 14:00', 100), " +
                //                       "((SELECT uid FROM movie WHERE name='Zootopia'), (SELECT uid FROM theater WHERE type='Hall B'), '2025-05-10 15:00', 50);";
                // conn.createStatement().execute(insertShowtimes);

                // Insert default showtimes
                String insertShowtimes = "INSERT OR IGNORE INTO showtime (movie_uid, theater_uid, time, available_seats) VALUES ((SELECT uid FROM movie WHERE name = ?), (SELECT uid FROM theater WHERE type = ?), ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertShowtimes)) {
                    stmt.setString(1, "Star Wars");
                    stmt.setString(2, "Hall A");
                    stmt.setString(3, "2025-05-10 14:00");
                    stmt.setInt(4, 100);
                    stmt.addBatch();

                    stmt.setString(1, "Zootopia");
                    stmt.setString(2, "Hall B");
                    stmt.setString(3, "2025-05-10 15:00");
                    stmt.setInt(4, 50);
                    stmt.addBatch();

                    stmt.setString(1, "Inception");
                    stmt.setString(2, "Hall A");
                    stmt.setString(3, "2025-05-11 16:00");
                    stmt.setInt(4, 100);
                    stmt.addBatch();

                    stmt.setString(1, "The Godfather");
                    stmt.setString(2, "Hall B");
                    stmt.setString(3, "2025-05-11 17:00");
                    stmt.setInt(4, 50);
                    stmt.addBatch();

                    stmt.setString(1, "Frozen");
                    stmt.setString(2, "Hall A");
                    stmt.setString(3, "2025-05-12 18:00");
                    stmt.setInt(4, 100);
                    stmt.addBatch();

                    stmt.setString(1, "Avengers: Endgame");
                    stmt.setString(2, "Hall B");
                    stmt.setString(3, "2025-05-12 19:00");
                    stmt.setInt(4, 50);
                    stmt.addBatch();

                    stmt.executeBatch();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        clearDatabase();
        System.out.println("Initializing database...");
        initializeDatabase();
        System.out.println("Database initialized successfully.");
    }
}