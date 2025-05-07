package com.example.bdsqltester.scenes;

import com.example.bdsqltester.datasources.MainDataSource;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;

import java.sql.*;
import java.util.Arrays;
import java.util.Random;

public class UserController {

    @FXML private TextArea instructionArea;
    @FXML private TextArea queryArea;
    @FXML private Label resultLabel;
    @FXML private Label gradeLabel;
    @FXML private TextArea previousGradesArea;
    @FXML private Button testButton;
    @FXML private Button submitButton;

    private String correctQuery = "";
    private int currentAssignmentId = -1;

    @FXML
    public void initialize() {
        loadRandomAssignment();
    }

    // Load random assignment (instructions + answer_key)
    private void loadRandomAssignment() {
        try (Connection conn = MainDataSource.getConnection()) {
            ResultSet countResult = conn.createStatement().executeQuery("SELECT COUNT(*) AS total FROM assignments");
            int totalAssignments = countResult.next() ? countResult.getInt("total") : 0;
            if (totalAssignments == 0) {
                instructionArea.setText("No assignments available.");
                return;
            }

            int randomId = new Random().nextInt(totalAssignments) + 1;
            PreparedStatement stmt = conn.prepareStatement("SELECT id, instructions, answer_key FROM assignments WHERE id = ?");
            stmt.setInt(1, randomId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                currentAssignmentId = rs.getInt("id");
                correctQuery = rs.getString("answer_key");
                instructionArea.setText(rs.getString("instructions"));
            }
        } catch (SQLException e) {
            instructionArea.setText("Failed to load assignment: " + e.getMessage());
        }
    }

    private String runQuery(String sql) {
        try (Connection conn = MainDataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            StringBuilder result = new StringBuilder();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                for (int i = 1; i <= colCount; i++) {
                    result.append(rs.getString(i)).append(" ");
                }
                result.append("\n");
            }

            return result.toString().trim();
        } catch (SQLException e) {
            return "SQL Error: " + e.getMessage();
        }
    }

    private int compareQueries(String userOutput, String correctOutput) {
        if (userOutput.equals(correctOutput)) return 100;

        String[] userLines = userOutput.split("\n");
        String[] correctLines = correctOutput.split("\n");

        if (userLines.length != correctLines.length) return 0;

        Arrays.sort(userLines);
        Arrays.sort(correctLines);

        return Arrays.equals(userLines, correctLines) ? 50 : 0;
    }

    private void saveGrade(int grade) {
        previousGradesArea.appendText("Assignment " + currentAssignmentId + " → " + grade + "%\n");
        // Extend this to update a real grades table as needed
    }

    @FXML
    private void handleTest() {
        String userQuery = queryArea.getText();
        String output = runQuery(userQuery);
        resultLabel.setText("Test Output:\n" + output);
    }

    @FXML
    private void handleSubmit() {
        String userQuery = queryArea.getText();
        String correctOutput = runQuery(correctQuery);
        String userOutput = runQuery(userQuery);

        int grade = compareQueries(userOutput, correctOutput);
        gradeLabel.setText("Your Grade: " + grade + "%");
        saveGrade(grade);
    }
}
