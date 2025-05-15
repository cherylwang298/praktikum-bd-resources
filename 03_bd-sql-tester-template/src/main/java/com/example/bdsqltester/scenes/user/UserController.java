package com.example.bdsqltester.scenes.user;

import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.session.session;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class UserController {

    @FXML
    private ListView<String> assignmentList;

    @FXML
    private TextArea instructionsField;

    @FXML
    private TextArea answerField;

    @FXML
    public void initialize() {
        loadAssignments();

        assignmentList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadInstructions(newValue);  // Panggil loadInstructions saat item dipilih
            }
        });
    }

    @FXML
    private void onSaveClick(ActionEvent event) {
        String selectedAssignment = assignmentList.getSelectionModel().getSelectedItem();
        String userAnswer = answerField.getText().trim();
        int userId = session.getUserId(); // Ambil user_id dari session

        if (selectedAssignment == null || userAnswer.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete", "Please select an assignment and provide your answer.");
            return;
        }

        try (Connection conn = MainDataSource.getConnection()) {
            // Ambil assignment_id dan kunci jawaban dari database
            String query = "SELECT id, answer_key FROM assignments WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, selectedAssignment);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Assignment not found in the database.");
                return;
            }

            int assignmentId = rs.getInt("id");
            String correctAnswer = rs.getString("answer_key").trim();

            // Penilaian
            int grade = calculateGrade(userAnswer, correctAnswer);

            // Simpan ke tabel grades
            String insertSql = "INSERT INTO grades (user_id, assignment_id, grade) VALUES (?, ?, ?) " +
                    "ON CONFLICT (user_id, assignment_id) DO UPDATE SET grade = EXCLUDED.grade";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, userId);
            insertStmt.setInt(2, assignmentId);
            insertStmt.setInt(3, grade);
            insertStmt.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Your answer has been graded and saved.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    @FXML
    private void onTestButtonClick(ActionEvent event) {
        String query = answerField.getText().trim();

        if (query.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Query", "Please enter a SQL query.");
            return;
        }

        try (Connection conn = GradingDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            TableView<List<String>> tableView = new TableView<>();

            int columnCount = rs.getMetaData().getColumnCount();
            // Buat kolom dinamis
            for (int i = 1; i <= columnCount; i++) {
                final int colIndex = i - 1;
                String colName = rs.getMetaData().getColumnName(i);
                TableColumn<List<String>, String> column = new TableColumn<>(colName);
                column.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(colIndex)));
                tableView.getColumns().add(column);
            }

            // Isi data
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                tableView.getItems().add(row);
            }

            // Tampilkan di jendela/tab baru
            Stage stage = new Stage();
            VBox vbox = new VBox(tableView);
            Scene scene = new Scene(vbox, 600, 400);
            stage.setTitle("Query Result");
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Query Error", e.getMessage());
        }
    }

    @FXML
    private void onShowGradeClick(ActionEvent event) {
        String selectedAssignment = assignmentList.getSelectionModel().getSelectedItem();
        if (selectedAssignment == null) {
            showAlert(Alert.AlertType.WARNING, "No Assignment Selected", "Please select an assignment to view your grade.");
            return;
        }

        int userId = session.getUserId();  // Ambil user_id dari session

        try (Connection conn = MainDataSource.getConnection()) {
            // Ambil assignment_id berdasarkan nama assignment yang dipilih
            String query = "SELECT id FROM assignments WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, selectedAssignment);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                showAlert(Alert.AlertType.ERROR, "Assignment Not Found", "The selected assignment could not be found.");
                return;
            }

            int assignmentId = rs.getInt("id");

            // Ambil nilai grade berdasarkan user_id dan assignment_id
            String gradeQuery = "SELECT grade FROM grades WHERE user_id = ? AND assignment_id = ?";
            PreparedStatement gradeStmt = conn.prepareStatement(gradeQuery);
            gradeStmt.setInt(1, userId);
            gradeStmt.setInt(2, assignmentId);
            ResultSet gradeRs = gradeStmt.executeQuery();

            if (gradeRs.next()) {
                int grade = gradeRs.getInt("grade");
                showAlert(Alert.AlertType.INFORMATION, "Your Grade", "Your grade for the assignment is: " + grade);
            } else {
                showAlert(Alert.AlertType.INFORMATION, "No Grade Found", "You haven't received a grade for this assignment yet.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadAssignments() {
        // Kosongkan list view terlebih dahulu
        assignmentList.getItems().clear();

        try (Connection conn = MainDataSource.getConnection()) {
            String sql = "SELECT name FROM assignments";  // Query untuk mengambil nama assignment
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Tambahkan nama-nama assignment ke ListView
            while (rs.next()) {
                String assignmentName = rs.getString("name");
                assignmentList.getItems().add(assignmentName);
            }

            if (assignmentList.getItems().isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No Assignments", "No assignments found in the database.");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }
    private void loadInstructions(String assignmentName) {
        try (Connection conn = MainDataSource.getConnection()) {
            String sql = "SELECT instructions FROM assignments WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, assignmentName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String instructions = rs.getString("instructions");
                instructionsField.setText(instructions);  // Tampilkan instruksi di TextArea
            } else {
                instructionsField.clear();  // Jika tidak ditemukan, kosongkan TextArea
                showAlert(Alert.AlertType.WARNING, "No Instructions", "No instructions found for the selected assignment.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }
    private int calculateGrade(String userAnswer, String correctAnswer) {
        String normalizedUser = userAnswer.replaceAll("\\s+", " ").trim().toLowerCase();
        String normalizedCorrect = correctAnswer.replaceAll("\\s+", " ").trim().toLowerCase();

        if (normalizedUser.equals(normalizedCorrect)) {
            return 100;
        }

        // Cek isi sama tapi urutan beda
        String[] userTokens = normalizedUser.split(" ");
        String[] correctTokens = normalizedCorrect.split(" ");

        java.util.Arrays.sort(userTokens);
        java.util.Arrays.sort(correctTokens);

        if (java.util.Arrays.equals(userTokens, correctTokens)) {
            return 50;
        }

        return 0;
    }
}