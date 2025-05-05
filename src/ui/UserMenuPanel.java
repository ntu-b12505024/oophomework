package ui;

import model.Member;
import model.Movie;
import model.Reservation;
import model.Showtime;
import service.MovieService;
import service.ReservationService;
import service.ShowtimeService;
import exception.AgeRestrictionException;
import exception.SeatUnavailableException;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UserMenuPanel extends JPanel {

    private final CinemaBookingGUI mainGUI;
    private final ReservationService reservationService;
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final Member currentUser;

    private JTabbedPane tabbedPane;

    // Tab 1: View Movies & Showtimes / Book Tickets
    private JTable moviesTable;
    private JTable showtimesTable;
    private DefaultTableModel moviesTableModel;
    private DefaultTableModel showtimesTableModel;
    private JTextField seatsField;
    private JButton bookButton;
    private Movie selectedMovie = null;
    private Showtime selectedShowtime = null;

    // Tab 2: My Reservations / Cancel Reservation
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;
    private JTextField reservationIdToCancelField;
    private JButton cancelReservationButton;


    public UserMenuPanel(CinemaBookingGUI mainGUI, ReservationService reservationService, MovieService movieService, ShowtimeService showtimeService, Member currentUser) {
        this.mainGUI = mainGUI;
        this.reservationService = reservationService;
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout());

        // Logout Button (Top Right)
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("歡迎, " + currentUser.getEmail());
        JButton logoutButton = new JButton("登出");
        logoutButton.addActionListener(e -> mainGUI.showLoginPanel());
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);


        // Tabbed Pane for different functions
        tabbedPane = new JTabbedPane();

        // --- Tab 1: Booking ---
        JPanel bookingPanel = createBookingPanel();
        tabbedPane.addTab("查看電影與訂票", null, bookingPanel, "查看目前上映電影、場次並訂票");

        // --- Tab 2: Reservations Management ---
        JPanel reservationsPanel = createReservationsPanel();
        tabbedPane.addTab("我的訂票紀錄", null, reservationsPanel, "查看與管理您的訂票");

        add(tabbedPane, BorderLayout.CENTER);

        // Initial data load
        loadMovies();
        loadUserReservations();
    }

    // =========================================================================
    // Booking Panel (Tab 1)
    // =========================================================================
    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Movie List ---
        JPanel moviesPanel = new JPanel(new BorderLayout());
        moviesPanel.setBorder(BorderFactory.createTitledBorder("選擇電影"));
        moviesTableModel = new DefaultTableModel(new String[]{"ID", "名稱", "分級", "片長(分)", "描述"}, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; } // Not editable
        };
        moviesTable = new JTable(moviesTableModel);
        moviesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moviesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && moviesTable.getSelectedRow() != -1) {
                int selectedRow = moviesTable.getSelectedRow();
                int movieId = (int) moviesTableModel.getValueAt(selectedRow, 0);
                selectedMovie = movieService.getMovieById(movieId).orElse(null); // Get full movie object
                loadShowtimesForMovie(movieId);
                clearBookingSelection(); // Clear previous showtime/seat selection
            }
        });
        JScrollPane moviesScrollPane = new JScrollPane(moviesTable);
        moviesPanel.add(moviesScrollPane, BorderLayout.CENTER);


        // --- Showtime List ---
        JPanel showtimesPanel = new JPanel(new BorderLayout());
        showtimesPanel.setBorder(BorderFactory.createTitledBorder("選擇場次"));
        showtimesTableModel = new DefaultTableModel(new String[]{"ID", "影廳", "時間", "剩餘座位"}, 0){
             @Override public boolean isCellEditable(int row, int column) { return false; } // Not editable
        };
        showtimesTable = new JTable(showtimesTableModel);
        showtimesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        showtimesTable.getSelectionModel().addListSelectionListener(e -> {
             if (!e.getValueIsAdjusting() && showtimesTable.getSelectedRow() != -1) {
                int selectedRow = showtimesTable.getSelectedRow();
                int showtimeId = (int) showtimesTableModel.getValueAt(selectedRow, 0);
                selectedShowtime = showtimeService.getShowtimeById(showtimeId); // Directly get Showtime object
                seatsField.setEnabled(selectedShowtime != null);
                bookButton.setEnabled(selectedShowtime != null && !seatsField.getText().trim().isEmpty());
             }
        });
        JScrollPane showtimesScrollPane = new JScrollPane(showtimesTable);
        showtimesPanel.add(showtimesScrollPane, BorderLayout.CENTER);


        // --- Booking Action Area ---
        JPanel bookingActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bookingActionPanel.setBorder(BorderFactory.createTitledBorder("訂票操作"));
        JLabel seatsLabel = new JLabel("輸入座位 (以逗號分隔, e.g., A1,A2):");
        seatsField = new JTextField(20);
        seatsField.setEnabled(false); // Initially disabled
        bookButton = new JButton("確認訂票");
        bookButton.setEnabled(false); // Initially disabled

        seatsField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { checkBookButtonState(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { checkBookButtonState(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { checkBookButtonState(); }
        });


        bookingActionPanel.add(seatsLabel);
        bookingActionPanel.add(seatsField);
        bookingActionPanel.add(bookButton);

        bookButton.addActionListener(e -> handleBooking());


        // --- Layout Setup ---
        JSplitPane movieShowtimeSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, moviesPanel, showtimesPanel);
        movieShowtimeSplit.setResizeWeight(0.5); // Distribute space equally initially

        panel.add(movieShowtimeSplit, BorderLayout.CENTER);
        panel.add(bookingActionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadMovies() {
        moviesTableModel.setRowCount(0); // Clear existing data
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

    private void loadShowtimesForMovie(int movieId) {
        showtimesTableModel.setRowCount(0); // Clear existing data
        if (movieId <= 0) return;

        List<Showtime> showtimes = showtimeService.getShowtimesByMovieId(movieId);
        for (Showtime st : showtimes) {
             // Calculate remaining seats (This might need optimization or a dedicated service method)
             List<String> bookedSeats = reservationService.getBookedSeatsForShowtime(st.getUid());
             int totalSeats = st.getTheater().getTotalSeats(); // Assuming Theater has capacity -> Changed to getTotalSeats
             int availableSeats = totalSeats - bookedSeats.size();

            showtimesTableModel.addRow(new Object[]{
                    st.getUid(),
                    st.getTheater().getType(), // Assuming Theater has name -> Changed to getType
                    st.getShowTime(), // Consider formatting the date/time
                    availableSeats + " / " + totalSeats
            });
        }
    }

     private void clearBookingSelection() {
        selectedShowtime = null;
        showtimesTable.clearSelection();
        seatsField.setText("");
        seatsField.setEnabled(false);
        bookButton.setEnabled(false);
    }

    private void checkBookButtonState() {
         bookButton.setEnabled(selectedShowtime != null && !seatsField.getText().trim().isEmpty());
    }


    private void handleBooking() {
        if (selectedShowtime == null) {
            JOptionPane.showMessageDialog(this, "請先選擇一個場次", "訂票錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedMovie == null) {
             JOptionPane.showMessageDialog(this, "請先選擇一部電影", "訂票錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String seatsInput = seatsField.getText().trim();
        if (seatsInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入座位號碼", "訂票錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Split seats and trim whitespace
        List<String> seatNumbers = Arrays.stream(seatsInput.split(","))
                                         .map(String::trim)
                                         .filter(s -> !s.isEmpty())
                                         .collect(Collectors.toList());

        if (seatNumbers.isEmpty()) {
             JOptionPane.showMessageDialog(this, "請輸入有效的座位號碼", "訂票錯誤", JOptionPane.WARNING_MESSAGE);
             return;
        }


        try {
            // Call the booking service
            String result = reservationService.bookTickets(currentUser.getUid(), selectedShowtime.getUid(), seatNumbers);

            // Display result
            JOptionPane.showMessageDialog(this, result, "訂票結果", JOptionPane.INFORMATION_MESSAGE);

            // If successful, clear fields and refresh relevant data
            if (result.startsWith("訂票成功")) {
                seatsField.setText("");
                loadShowtimesForMovie(selectedMovie.getUid()); // Refresh showtimes to show updated seat count
                loadUserReservations(); // Refresh user's reservations list on the other tab
                tabbedPane.setSelectedIndex(1); // Switch to reservations tab
            }

        } catch (IllegalArgumentException ex) { // Removed SeatUnavailableException as bookTickets handles it internally
             JOptionPane.showMessageDialog(this, "訂票失敗: " + ex.getMessage(), "訂票失敗", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) { // Keep a general catch for other unexpected errors
            JOptionPane.showMessageDialog(this, "訂票時發生未知錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }


    // =========================================================================
    // Reservations Management Panel (Tab 2)
    // =========================================================================
    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Reservations List ---
        JPanel reservationsListPanel = new JPanel(new BorderLayout());
        reservationsListPanel.setBorder(BorderFactory.createTitledBorder("我的訂票紀錄"));
        reservationsTableModel = new DefaultTableModel(new String[]{"訂票 ID", "電影", "影廳", "時間", "座位", "狀態"}, 0){
             @Override public boolean isCellEditable(int row, int column) { return false; } // Not editable
        };
        reservationsTable = new JTable(reservationsTableModel);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane reservationsScrollPane = new JScrollPane(reservationsTable);
        reservationsListPanel.add(reservationsScrollPane, BorderLayout.CENTER);

        // --- Cancel Action Area ---
        JPanel cancelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cancelPanel.setBorder(BorderFactory.createTitledBorder("取消訂票"));
        JLabel cancelLabel = new JLabel("輸入要取消的訂票 ID:");
        reservationIdToCancelField = new JTextField(10);
        cancelReservationButton = new JButton("確認取消");

        cancelPanel.add(cancelLabel);
        cancelPanel.add(reservationIdToCancelField);
        cancelPanel.add(cancelReservationButton);

        cancelReservationButton.addActionListener(e -> handleCancelReservation());

        // --- Layout ---
        panel.add(reservationsListPanel, BorderLayout.CENTER);
        panel.add(cancelPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadUserReservations() {
        reservationsTableModel.setRowCount(0); // Clear existing data
        List<Reservation> reservations = reservationService.listReservationsByMember(currentUser.getUid());

        for (Reservation res : reservations) {
            Showtime st = res.getShowtime(); // Assuming Reservation has getShowtime()
            Movie mv = (st != null) ? st.getMovie() : null; // Assuming Showtime has getMovie()
            String movieName = (mv != null) ? mv.getName() : "N/A";
            String theaterType = (st != null && st.getTheater() != null) ? st.getTheater().getType() : "N/A"; // Use getType
            String showTimeStr = (st != null) ? st.getShowTime().toString() : "N/A"; // Consider formatting

            reservationsTableModel.addRow(new Object[]{
                    res.getUid(),
                    movieName,
                    theaterType, // Use getType
                    showTimeStr,
                    String.join(", ", res.getSeatNumbers()),
                    res.getStatus()
            });
        }
    }

    private void handleCancelReservation() {
        String idText = reservationIdToCancelField.getText().trim();
        if (idText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入要取消的訂票 ID。", "錯誤", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int reservationId = Integer.parseInt(idText);

            // Confirmation dialog
            int confirm = JOptionPane.showConfirmDialog(this,
                    "確定要取消訂票 ID: " + reservationId + " 嗎？此操作無法復原。",
                    "確認取消",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = reservationService.cancelReservation(reservationId, currentUser.getUid());

                if (success) {
                    JOptionPane.showMessageDialog(this, "訂票 ID: " + reservationId + " 已成功取消。", "取消成功", JOptionPane.INFORMATION_MESSAGE);
                    reservationIdToCancelField.setText(""); // Clear input field
                    loadUserReservations(); // Refresh the reservations list
                    loadShowtimesForMovie(selectedMovie != null ? selectedMovie.getUid() : -1); // Refresh showtimes on the other tab if a movie is selected
                } else {
                    JOptionPane.showMessageDialog(this, "取消失敗。可能原因：\n- 訂票 ID 不存在\n- 此訂票不屬於您\n- 訂票狀態無法取消 (例如已過期或已取消)", "取消失敗", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字訂票 ID。", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "取消時發生錯誤: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
