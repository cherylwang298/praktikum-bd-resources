    package com.example.bdsqltester.scenes.admin;

    import com.example.bdsqltester.datasources.MainDataSource;
    import javafx.collections.FXCollections;
    import javafx.collections.ObservableList;
    import javafx.fxml.FXML;
    import javafx.scene.control.Label;
    import javafx.scene.control.TableColumn;
    import javafx.scene.control.TableView;
    import javafx.scene.control.cell.PropertyValueFactory;
    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;

    public class showGradeController {

        @FXML
        private TableView<UserGrade> gradeTable;

        @FXML
        private TableColumn<UserGrade, String> usernameColumn;

        @FXML
        private TableColumn<UserGrade, Integer> gradeColumn;

        @FXML
        private Label titleLabel;

        private int assignmentId;

        public void setAssignmentData(int assignmentId, String assignmentName) {
            this.assignmentId = assignmentId;
            titleLabel.setText("Grades for: " + assignmentName);
            loadGradesForAssignment(assignmentId);
        }

        @FXML
        public void initialize() {
            usernameColumn.setCellValueFactory(new PropertyValueFactory<UserGrade, String>("username"));
            gradeColumn.setCellValueFactory(new PropertyValueFactory<UserGrade, Integer>("grade"));
        }

        private void loadGradesForAssignment(int assignmentId) {
            ObservableList<UserGrade> grades = FXCollections.observableArrayList();

            try (Connection conn = MainDataSource.getConnection()) {
                String query = """
                    SELECT users.username, grades.grade
                    FROM grades
                    JOIN users ON grades.user_id = users.id
                    WHERE grades.assignment_id = ?
                """;
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, assignmentId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    grades.add(new UserGrade(rs.getString("username"), rs.getInt("grade")));
                }

                gradeTable.setItems(grades);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
