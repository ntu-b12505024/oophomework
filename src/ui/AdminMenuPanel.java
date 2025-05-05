package ui;

import model.Member;
import model.Movie;
import model.Reservation;
import model.Showtime;
import service.MemberService;
import service.MovieService;
import service.ReservationService;
import service.ShowtimeService;
import util.DBUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminMenuPanel extends JPanel {

    private final CinemaBookingGUI mainGUI;
    private final ReservationService reservationService;
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final MemberService memberService; // Needed for some operations potentially

    private JTabbedPane tabbedPane;

    // Tab 1: Movie Management
    private JTable moviesTable;
    private DefaultTableModel moviesTableModel;
    private JTextField movieNameField, movieDurationField, movieDescField, movieRatingField;
    private JButton addMovieButton;
    private JTextField removeMovieIdField;
    private JButton removeMovieButton;

    // Tab 2: Showtime Management
    private JTable showtimesTable;
    private DefaultTableModel showtimesTableModel;
    private JTextField updateShowtimeIdField, updateShowtimeTimeField;
    private JButton updateShowtimeButton;
    // TODO: Add Showtime functionality could be added here

    // Tab 3: Reservation Management
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;
    private JTextField updateReservationIdField;
    private JComboBox<String> updateReservationStatusCombo;
    private JButton updateReservationStatusButton;

    // Tab 4: Database Management
    private JButton resetDatabaseButton;

    public AdminMenuPanel(CinemaBookingGUI mainGUI, ReservationService reservationService, MovieService movieService, ShowtimeService showtimeService, MemberService memberService) {
        this.mainGUI = mainGUI;
        this.reservationService = reservationService;
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.memberService = memberService;

        setLayout(new BorderLayout());

        // Logout Button (Top Right)
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("管理員模式");
        JButton logoutButton = new JButton("登出");
        logoutButton.addActionListener(e -> mainGUI.showLoginPanel());
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();

        // --- Tab 1: Movie Management ---
        JPanel moviePanel = createMovieManagementPanel();
        tabbedPane.addTab("電影管理", null, moviePanel, "新增、移除電影");

        // --- Tab 2: Showtime Management ---
        JPanel showtimePanel = createShowtimeManagementPanel();
        tabbedPane.addTab("場次管理", null, showtimePanel, "更新場次時間");

        // --- Tab 3: Reservation Management ---
        JPanel reservationPanel = createReservationManagementPanel();
        tabbedPane.addTab("訂票管理", null, reservationPanel, "查看與更新訂票狀態");

        // --- Tab 4: Database Management ---
        JPanel databasePanel = createDatabaseManagementPanel();
        tabbedPane.addTab("資料庫管理", null, databasePanel, "重設資料庫");

        add(tabbedPane, BorderLayout.CENTER);

        // Initial data load for tables
        loadMovies();
        loadAllShowtimes();
        loadAllReservations();
    }

    // =========================================================================
    // Movie Management Panel (Tab 1)
    // =========================================================================
    private JPanel createMovieManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Movie List Table ---
        moviesTableModel = new DefaultTableModel(new String[]{"ID", "名稱", "分級", "片長(分)", "描述"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        moviesTable = new JTable(moviesTableModel);
        JScrollPane moviesScrollPane = new JScrollPane(moviesTable);
        panel.add(moviesScrollPane, BorderLayout.CENTER);

        // --- Action Panel (Add/Remove) ---
        JPanel actionPanel = new JPanel(new GridLayout(2, 1, 10, 10)); // 2 rows for Add and Remove sections

        // Add Movie Section
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.setBorder(BorderFactory.createTitledBorder("新增電影"));
        addPanel.add(new JLabel("名稱:"));
        movieNameField = new JTextField(15);
        addPanel.add(movieNameField);
        addPanel.add(new JLabel("片長(分):"));
        movieDurationField = new JTextField(5);
        addPanel.add(movieDurationField);
        addPanel.add(new JLabel("描述:"));
        movieDescField = new JTextField(20);
        addPanel.add(movieDescField);
        addPanel.add(new JLabel("分級:"));
        movieRatingField = new JTextField(5);
        addPanel.add(movieRatingField);
        addMovieButton = new JButton("新增");
        addMovieButton.addActionListener(e -> handleAddMovie());
        addPanel.add(addMovieButton);

        // Remove Movie Section
        JPanel removePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removePanel.setBorder(BorderFactory.createTitledBorder("移除電影"));
        removePanel.add(new JLabel("輸入要移除的電影 ID:"));
        removeMovieIdField = new JTextField(5);
        removePanel.add(removeMovieIdField);
        removeMovieButton = new JButton("移除");
        removeMovieButton.addActionListener(e -> handleRemoveMovie());
        removePanel.add(removeMovieButton);

        actionPanel.add(addPanel);
        actionPanel.add(removePanel);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadMovies() {
        moviesTableModel.setRowCount(0);
        List<Movie> movies = movieService.getAllMovies();
        for (Movie movie : movies) {
            moviesTableModel.addRow(new Object[]{
                    movie.getUid(),
                    movie.getName(),
                    movie.getRating(),
                    movie.getDuration(),
                    movie.getDescription()
            });
        }
    }

    private void handleAddMovie() {
        String name = movieNameField.getText().trim();
        String durationStr = movieDurationField.getText().trim();
        String desc = movieDescField.getText().trim();
        String rating = movieRatingField.getText().trim();

        if (name.isEmpty() || durationStr.isEmpty() || desc.isEmpty() || rating.isEmpty()) {
            JOptionPane.showMessageDialog(this, "所有欄位皆為必填", "新增錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            Movie added = movieService.addMovie(name, duration, desc, rating);
            if (added != null) {
                JOptionPane.showMessageDialog(this, "電影新增成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                // Clear fields and reload table
                movieNameField.setText("");
                movieDurationField.setText("");
                movieDescField.setText("");
                movieRatingField.setText("");
                loadMovies();
            } else {
                JOptionPane.showMessageDialog(this, "新增電影失敗 (可能是內部錯誤)", "新增失敗", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "片長必須是有效的數字", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "新增電影時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void handleRemoveMovie() {
        String idStr = removeMovieIdField.getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入要移除的電影 ID", "移除錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int movieId = Integer.parseInt(idStr);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "確定要移除電影 ID: " + movieId + " 嗎？相關的場次和訂票也會被影響！",
                    "確認移除",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean removed = movieService.removeMovie(movieId);
                if (removed) {
                    JOptionPane.showMessageDialog(this, "電影移除成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                    removeMovieIdField.setText("");
                    loadMovies();
                    loadAllShowtimes(); // Refresh showtimes as they might be affected
                    loadAllReservations(); // Refresh reservations
                } else {
                    JOptionPane.showMessageDialog(this, "移除電影失敗 (可能是 ID 不存在)", "移除失敗", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字電影 ID", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "移除電影時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Showtime Management Panel (Tab 2)
    // =========================================================================
    private JPanel createShowtimeManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Showtime List Table ---
        showtimesTableModel = new DefaultTableModel(new String[]{"ID", "電影 ID", "電影名稱", "影廳", "時間"}, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        showtimesTable = new JTable(showtimesTableModel);
        JScrollPane showtimesScrollPane = new JScrollPane(showtimesTable);
        panel.add(showtimesScrollPane, BorderLayout.CENTER);

        // --- Action Panel (Update Time) ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.setBorder(BorderFactory.createTitledBorder("更新場次時間"));
        actionPanel.add(new JLabel("場次 ID:"));
        updateShowtimeIdField = new JTextField(5);
        actionPanel.add(updateShowtimeIdField);
        actionPanel.add(new JLabel("新時間 (yyyy-MM-dd HH:mm):"));
        updateShowtimeTimeField = new JTextField(15);
        actionPanel.add(updateShowtimeTimeField);
        updateShowtimeButton = new JButton("更新時間");
        updateShowtimeButton.addActionListener(e -> handleUpdateShowtime());
        actionPanel.add(updateShowtimeButton);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadAllShowtimes() {
        showtimesTableModel.setRowCount(0);
        List<Showtime> showtimes = showtimeService.getAllShowtimes(); // Need a method to get all showtimes
        for (Showtime st : showtimes) {
            Movie movie = st.getMovie(); // Assuming Showtime has getMovie()
            String movieName = (movie != null) ? movie.getName() : "N/A";
            String theaterType = (st.getTheater() != null) ? st.getTheater().getType() : "N/A";
            showtimesTableModel.addRow(new Object[]{
                    st.getUid(),
                    (movie != null) ? movie.getUid() : -1,
                    movieName,
                    theaterType, // Use getType
                    st.getShowTime() // Consider formatting
            });
        }
    }

    private void handleUpdateShowtime() {
        String idStr = updateShowtimeIdField.getText().trim();
        String newTime = updateShowtimeTimeField.getText().trim();

        if (idStr.isEmpty() || newTime.isEmpty()) {
            JOptionPane.showMessageDialog(this, "場次 ID 和新時間不能為空", "更新錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Basic format check (can be improved with regex or SimpleDateFormat)
        if (!newTime.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")) {
             JOptionPane.showMessageDialog(this, "時間格式錯誤，請使用 yyyy-MM-dd HH:mm", "格式錯誤", JOptionPane.WARNING_MESSAGE);
             return;
        }

        try {
            int showtimeId = Integer.parseInt(idStr);
            boolean updated = movieService.updateShowtimeTime(showtimeId, newTime); // Using movieService method as per Main.java
            if (updated) {
                JOptionPane.showMessageDialog(this, "場次時間更新成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                updateShowtimeIdField.setText("");
                updateShowtimeTimeField.setText("");
                loadAllShowtimes(); // Reload table
            } else {
                JOptionPane.showMessageDialog(this, "更新場次時間失敗 (可能是 ID 不存在或格式錯誤)", "更新失敗", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字場次 ID", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "更新場次時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Reservation Management Panel (Tab 3)
    // =========================================================================
    private JPanel createReservationManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Reservation List Table ---
        reservationsTableModel = new DefaultTableModel(new String[]{"訂票 ID", "會員 ID", "場次 ID", "座位", "狀態"}, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        reservationsTable = new JTable(reservationsTableModel);
        JScrollPane reservationsScrollPane = new JScrollPane(reservationsTable);
        panel.add(reservationsScrollPane, BorderLayout.CENTER);

        // --- Action Panel (Update Status) ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.setBorder(BorderFactory.createTitledBorder("更新訂票狀態"));
        actionPanel.add(new JLabel("訂票 ID:"));
        updateReservationIdField = new JTextField(5);
        actionPanel.add(updateReservationIdField);
        actionPanel.add(new JLabel("新狀態:"));
        updateReservationStatusCombo = new JComboBox<>(new String[]{"CONFIRMED", "CANCELLED"}); // Add other statuses if needed
        actionPanel.add(updateReservationStatusCombo);
        updateReservationStatusButton = new JButton("更新狀態");
        updateReservationStatusButton.addActionListener(e -> handleUpdateReservationStatus());
        actionPanel.add(updateReservationStatusButton);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadAllReservations() {
        reservationsTableModel.setRowCount(0);
        List<Reservation> reservations = reservationService.listReservations(); // Need a method to get all reservations
        for (Reservation res : reservations) {
            reservationsTableModel.addRow(new Object[]{
                    res.getUid(),
                    res.getMemberUid(),
                    res.getShowtimeUid(),
                    String.join(", ", res.getSeatNumbers()),
                    res.getStatus()
            });
        }
    }

    private void handleUpdateReservationStatus() {
        String idStr = updateReservationIdField.getText().trim();
        String newStatus = (String) updateReservationStatusCombo.getSelectedItem();

        if (idStr.isEmpty() || newStatus == null) {
            JOptionPane.showMessageDialog(this, "訂票 ID 和新狀態不能為空", "更新錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int reservationId = Integer.parseInt(idStr);
            boolean updated = reservationService.setReservationStatus(reservationId, newStatus);
            if (updated) {
                JOptionPane.showMessageDialog(this, "訂票狀態更新成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
                updateReservationIdField.setText("");
                loadAllReservations(); // Reload table
            } else {
                JOptionPane.showMessageDialog(this, "更新訂票狀態失敗 (可能是 ID 不存在)", "更新失敗", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字訂票 ID", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "更新訂票狀態時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Database Management Panel (Tab 4)
    // =========================================================================
    private JPanel createDatabaseManagementPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        resetDatabaseButton = new JButton("重設資料庫至預設狀態");
        resetDatabaseButton.setForeground(Color.RED);
        resetDatabaseButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        resetDatabaseButton.addActionListener(e -> handleResetDatabase());

        panel.add(resetDatabaseButton);
        return panel;
    }

    private void handleResetDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "警告：此操作將清除所有資料並還原至初始狀態！\n確定要重設資料庫嗎？",
                "確認重設資料庫",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DBUtil.clearDatabase();
                DBUtil.initializeDatabase();
                JOptionPane.showMessageDialog(this, "資料庫已成功重設至預設狀態。", "重設成功", JOptionPane.INFORMATION_MESSAGE);
                // Reload all data in other tabs
                loadMovies();
                loadAllShowtimes();
                loadAllReservations();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "重設資料庫時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}
