package dao;

import model.Reservation;
import util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    /**
     * Adds a new reservation to the database.
     * reservation_time and status are set by DB defaults.
     * Returns the generated UID of the new reservation, or -1 on failure.
     */
    public int addReservation(Reservation reservation) {
        String checkSql = "SELECT uid FROM reservation WHERE seat_no = ? AND time = ? AND status = 'CANCELLED'";
        String updateSql = "UPDATE reservation SET member_uid = ?, movie_uid = ?, theater_uid = ?, num_tickets = ?, status = 'CONFIRMED' WHERE uid = ?";
        String insertSql = "INSERT INTO reservation (member_uid, movie_uid, theater_uid, time, seat_no, num_tickets, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection()) {
            // 檢查是否存在相同座位和時間的 CANCELLED 記錄
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, reservation.getSeatNo());
                checkStmt.setString(2, reservation.getTime());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // 如果存在，更新該記錄
                        int existingId = rs.getInt("uid");
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, reservation.getMemberUid());
                            updateStmt.setInt(2, reservation.getMovieUid());
                            updateStmt.setInt(3, reservation.getTheaterUid());
                            updateStmt.setInt(4, reservation.getNumTickets());
                            updateStmt.setInt(5, existingId);
                            updateStmt.executeUpdate();
                            return existingId;
                        }
                    }
                }
            }

            // 如果不存在，插入新記錄
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, reservation.getMemberUid());
                insertStmt.setInt(2, reservation.getMovieUid());
                insertStmt.setInt(3, reservation.getTheaterUid());
                insertStmt.setString(4, reservation.getTime());
                insertStmt.setString(5, reservation.getSeatNo());
                insertStmt.setInt(6, reservation.getNumTickets());
                insertStmt.setString(7, reservation.getStatus());
                insertStmt.executeUpdate();

                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.err.println("座位已被預訂: " + reservation.getSeatNo());
            } else {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public Reservation getReservationById(int id) {
        String sql = "SELECT uid, member_uid, movie_uid, theater_uid, time, seat_no, num_tickets, status FROM reservation WHERE uid = ?";
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
        String sql = "SELECT * FROM reservation WHERE member_uid = ? AND status != 'CANCELLED'";
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
                            rs.getInt("num_tickets")
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

    // 刪除重複的 deleteReservation 方法，保留唯一的定義
    public void deleteReservation(int id) {
        String sql = "DELETE FROM reservation WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Reservation> getReservationsByShowtimeAndSeat(int showtimeUid, String seatNo) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE theater_uid = ? AND seat_no = ? AND status = 'CONFIRMED'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, showtimeUid);
            stmt.setString(2, seatNo);
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
                            rs.getInt("num_tickets")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    // Helper method to map ResultSet to Reservation object
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getInt("uid"),
                rs.getInt("member_uid"),
                rs.getInt("movie_uid"),
                rs.getInt("theater_uid"),
                rs.getString("time"),
                rs.getString("seat_no"),
                rs.getString("status"),
                rs.getInt("num_tickets")
        );
    }
}