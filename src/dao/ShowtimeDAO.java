package dao;

import model.Showtime;
import model.Theater; // Import Theater
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; // Import Statement
import java.util.ArrayList;
import java.util.List;

public class ShowtimeDAO {

    private final TheaterDAO theaterDAO = new TheaterDAO(); // Instantiate TheaterDAO

    /**
     * Adds a new showtime to the database, initializing available seats based on the theater.
     * @param showtime The Showtime object to add (UID can be 0).
     * @return The generated UID of the newly added showtime, or -1 on failure.
     */
    public int addShowtime(Showtime showtime) {
        Theater theater = theaterDAO.getTheaterById(showtime.getTheaterUid());
        if (theater == null) {
            System.err.println("Error adding showtime: Theater with UID " + showtime.getTheaterUid() + " not found.");
            return -1; // Indicate failure
        }
        int initialAvailableSeats = theater.getTotalSeats();

        String sql = "INSERT INTO showtime (movie_uid, theater_uid, time, available_seats) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // Add RETURN_GENERATED_KEYS
            stmt.setInt(1, showtime.getMovieUid());
            stmt.setInt(2, showtime.getTheaterUid());
            stmt.setString(3, showtime.getTime());
            stmt.setInt(4, initialAvailableSeats);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                return -1; // Insert failed
            }

            // Retrieve the generated key
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1); // Return generated UID
                } else {
                    System.err.println("Failed to retrieve generated key for new showtime.");
                    return -1; // Failed to get generated key
                }
            }
        } catch (SQLException e) {
            // Consider more specific error handling, e.g., foreign key constraints
            e.printStackTrace();
            return -1; // Indicate failure
        }
    }

    public Showtime getShowtimeById(int id) {
        String sql = "SELECT * FROM showtime WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToShowtime(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Showtime> getAllShowtimes() {
        List<Showtime> showtimes = new ArrayList<>();
        String sql = "SELECT * FROM showtime ORDER BY time"; // Order by time
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                showtimes.add(mapResultSetToShowtime(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return showtimes;
    }

    public List<Showtime> getShowtimesByMovieId(int movieId) {
        List<Showtime> showtimes = new ArrayList<>();
        String query = "SELECT * FROM showtime WHERE movie_uid = ? ORDER BY time"; // Order by time
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                showtimes.add(mapResultSetToShowtime(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return showtimes;
    }

    /**
     * Retrieves all showtimes for a specific theater, ordered by time.
     * @param theaterId The ID of the theater.
     * @return A list of showtimes for the given theater.
     */
    public List<Showtime> getShowtimesByTheaterId(int theaterId) {
        List<Showtime> showtimes = new ArrayList<>();
        String query = "SELECT * FROM showtime WHERE theater_uid = ? ORDER BY time";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, theaterId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                showtimes.add(mapResultSetToShowtime(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return showtimes;
    }

    /**
     * Updates an existing showtime.
     * Note: available_seats might be better handled by decrease/increase methods.
     * @param showtime The Showtime object with updated details.
     * @return true if the update affected 1 row, false otherwise.
     */
    public boolean updateShowtime(Showtime showtime) {
        String sql = "UPDATE showtime SET movie_uid = ?, theater_uid = ?, time = ?, available_seats = ? WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, showtime.getMovieUid());
            stmt.setInt(2, showtime.getTheaterUid());
            stmt.setString(3, showtime.getTime());
            stmt.setInt(4, showtime.getAvailableSeats());
            stmt.setInt(5, showtime.getUid());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a showtime by its ID.
     * ON DELETE CASCADE in DB handles related reservations.
     * @param id The UID of the showtime to delete.
     * @return true if the deletion affected 1 row, false otherwise.
     */
    public boolean deleteShowtime(int id) {
        String sql = "DELETE FROM showtime WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Decreases the available seats for a given showtime.
     * Returns true if the update was successful (enough seats were available),
     * false otherwise.
     */
    public boolean decreaseAvailableSeats(int showtimeId, int count) {
        // Use atomic update to prevent race conditions
        String sql = "UPDATE showtime SET available_seats = available_seats - ? " +
                     "WHERE uid = ? AND available_seats >= ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, count);
            stmt.setInt(2, showtimeId);
            stmt.setInt(3, count); // Ensure enough seats are available before decrementing
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // If 1 row was affected, the update was successful
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

     /**
     * Increases the available seats for a given showtime (e.g., for cancellations).
     */
    public boolean increaseAvailableSeats(int showtimeId, int count) {
        String sql = "UPDATE showtime SET available_seats = available_seats + ? WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, count);
            stmt.setInt(2, showtimeId);
            int rowsAffected = stmt.executeUpdate();
            // We might want to add a check against total_seats if needed, but generally adding back is safe.
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 在事務中減少場次的可用座位數量
     * @param conn 資料庫連接（必須在事務中）
     * @param showtimeUid 場次ID
     * @param count 要減少的座位數量
     * @return 操作是否成功
     */
    public boolean decreaseAvailableSeatsWithConnection(Connection conn, int showtimeUid, int count) throws SQLException {
        String sql = "UPDATE showtime SET available_seats = available_seats - ? WHERE uid = ? AND available_seats >= ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, count);
            stmt.setInt(2, showtimeUid);
            stmt.setInt(3, count);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * 在事務中增加指定場次的可用座位數
     * @param conn 已開啟的資料庫連接
     * @param showtimeUid 場次ID
     * @param numSeats 要增加的座位數量
     * @return 是否成功增加座位數
     */
    public boolean increaseAvailableSeatsWithConnection(Connection conn, int showtimeUid, int numSeats) throws SQLException {
        String sql = "UPDATE showtime SET available_seats = available_seats + ? WHERE uid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, numSeats);
            stmt.setInt(2, showtimeUid);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // Helper method to map ResultSet to Showtime object
    private Showtime mapResultSetToShowtime(ResultSet rs) throws SQLException {
        return new Showtime(
                rs.getInt("uid"),
                rs.getInt("movie_uid"),
                rs.getInt("theater_uid"),
                rs.getString("time"),
                rs.getInt("available_seats") // Include available_seats
        );
    }
}