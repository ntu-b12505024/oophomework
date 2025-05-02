package dao;

import model.Reservation;
import util.DBUtil;

import java.sql.*; // Import Timestamp
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    // Define a formatter for SQLite DATETIME (TEXT)
    private static final DateTimeFormatter SQLITE_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Adds a new reservation to the database.
     * reservation_time and status are set by DB defaults.
     * Returns the generated UID of the new reservation, or -1 on failure.
     */
    public int addReservation(Reservation reservation) {
        String sql = "INSERT INTO reservation (member_uid, movie_uid, theater_uid, time, seat_no, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reservation.getMemberUid());
            stmt.setInt(2, reservation.getMovieUid());
            stmt.setInt(3, reservation.getTheaterUid());
            stmt.setString(4, reservation.getTime());
            stmt.setString(5, reservation.getSeatNo());
            stmt.setString(6, reservation.getStatus());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Reservation getReservationById(int id) {
        String sql = "SELECT uid, member_uid, movie_uid, theater_uid, time, seat_no, status FROM reservation WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        // Select specific columns
        String sql = "SELECT uid, member_uid, movie_uid, theater_uid, time, seat_no, status FROM reservation ORDER BY time DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    /**
     * Retrieves all reservations for a specific member.
     */
    public List<Reservation> getReservationsByMemberId(int memberUid) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE member_uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memberUid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(new Reservation(
                            rs.getInt("uid"),
                            rs.getInt("member_uid"),
                            rs.getInt("movie_uid"),
                            rs.getInt("theater_uid"),
                            rs.getString("time"),
                            rs.getString("seat_no"),
                            rs.getString("status"),
                            rs.getInt("num_tickets") // 新增票數屬性
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    /**
     * Updates the status of a specific reservation.
     * Returns true if the update was successful, false otherwise.
     */
    public boolean updateReservationStatus(int reservationId, String status) {
        String sql = "UPDATE reservation SET status = ? WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, reservationId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Deletes a reservation by its ID.
     * Note: Consider if associated seats need to be freed up in ShowtimeDAO.
     * ON DELETE CASCADE on showtime_uid might handle related data, but seat count needs manual adjustment.
     */
    public void deleteReservation(int id) {
        // Before deleting, you might want to get the reservation details
        // to know how many seats to add back to the showtime.
        // This logic is better placed in the ReservationService.
        String sql = "DELETE FROM reservation WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper method to map ResultSet to Reservation object
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        String timeStr = rs.getString("time");
        if (timeStr != null) {
            try {
                // SQLite stores DATETIME as TEXT, parse it
                LocalDateTime.parse(timeStr, SQLITE_DATETIME_FORMATTER);
            } catch (Exception e) {
                System.err.println("Error parsing reservation time: " + timeStr);
                // Handle parsing error, maybe log it or keep reservationTime as null
            }
        }

        return new Reservation(
                rs.getInt("uid"),
                rs.getInt("member_uid"),
                rs.getInt("movie_uid"),
                rs.getInt("theater_uid"),
                rs.getString("time"),
                rs.getString("seat_no"),
                rs.getString("status"),
                rs.getInt("num_tickets") // 新增票數屬性
        );
    }

    // Removed the old updateReservation method as updateReservationStatus is more specific
    // public void updateReservation(Reservation reservation) { ... }
}