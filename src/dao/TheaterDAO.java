package dao;

import model.Theater;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TheaterDAO {

    public void addTheater(Theater theater) {
        String sql = "INSERT INTO theater (type, total_seats) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, theater.getType());
            stmt.setInt(2, theater.getTotalSeats());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Theater getTheaterById(int id) {
        String sql = "SELECT * FROM theater WHERE uid = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Theater(
                            rs.getInt("uid"),
                            rs.getString("type"),
                            rs.getInt("total_seats")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Theater> getAllTheaters() {
        List<Theater> theaters = new ArrayList<>();
        String sql = "SELECT * FROM theater";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                theaters.add(new Theater(
                        rs.getInt("uid"),
                        rs.getString("type"),
                        rs.getInt("total_seats")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return theaters;
    }

     public boolean existsByType(String type) {
        String sql = "SELECT 1 FROM theater WHERE type = ?";
        try (var conn = DBUtil.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            try (var rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 可以加入 update 和 delete 方法，如果需要的話
}
