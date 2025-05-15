package com.example.bdsqltester.scenes.admin;


import com.example.bdsqltester.datasources.MainDataSource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminShowGradesController {

    @FXML
    private TableView<GradeRecord> gradeTable;

    private final ObservableList<GradeRecord> gradeList = FXCollections.observableArrayList();

    private long assignmentId;

    public void setAssignmentId(String id) {
        try {
            this.assignmentId = Long.parseLong(id);
            loadGrades();
        } catch (NumberFormatException e) {
            showAlert("Invalid Assignment ID", "The assignment ID is not valid.");
        }
    }

    private void loadGrades() {
        gradeList.clear();

        try (Connection conn = MainDataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT g.user_id, u.username, g.grade " +
                            "FROM grades g JOIN users u ON g.user_id = u.id " +
                            "WHERE g.assignment_id = ?");
            stmt.setLong(1, assignmentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gradeList.add(new GradeRecord(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getInt("grade")
                ));
            }

            gradeTable.setItems(gradeList);

        } catch (Exception e) {
            showAlert("Database Error", e.getMessage());
        }
    }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class GradeRecord {
        private final long userId;
        private final String username;
        private final int grade;

        public GradeRecord(long userId, String username, int grade) {
            this.userId = userId;
            this.username = username;
            this.grade = grade;
        }

        public long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public int getGrade() {
            return grade;
        }
    }

    @FXML
    void initialize() {
        TableColumn<GradeRecord, Long> userIdCol = new TableColumn<>("User ID");
        userIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));

        TableColumn<GradeRecord, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<GradeRecord, Integer> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));

        gradeTable.getColumns().addAll(userIdCol, usernameCol, gradeCol);
    }
}
