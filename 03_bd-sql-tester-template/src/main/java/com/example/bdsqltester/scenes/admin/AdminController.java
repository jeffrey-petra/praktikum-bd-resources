package com.example.bdsqltester.scenes.admin;

import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;

public class AdminController {

    @FXML
    private TextArea answerKeyField;

    @FXML
    private ListView<Assignment> assignmentList = new ListView<>();

    @FXML
    private TextField idField;

    @FXML
    private TextArea instructionsField;

    @FXML
    private TextField nameField;

    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        idField.setEditable(false);
        idField.setMouseTransparent(true);
        idField.setFocusTraversable(false);
        refreshAssignmentList();

        assignmentList.setCellFactory(param -> new ListCell<Assignment>() {
            @Override
            protected void updateItem(Assignment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected) {
                    onAssignmentSelected(getItem());
                }
            }
        });
    }

    void refreshAssignmentList() {
        assignments.clear();
        try (Connection c = MainDataSource.getConnection()) {
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM assignments");

            while (rs.next()) {
                assignments.add(new Assignment(rs));
            }
        } catch (Exception e) {
            showAlert("Database Error", e.toString());
        }
        assignmentList.setItems(assignments);
    }

    void onAssignmentSelected(Assignment assignment) {
        idField.setText(String.valueOf(assignment.id));
        nameField.setText(assignment.name);
        instructionsField.setText(assignment.instructions);
        answerKeyField.setText(assignment.answerKey);
    }

    @FXML
    void onNewAssignmentClick(ActionEvent event) {
        idField.clear();
        nameField.clear();
        instructionsField.clear();
        answerKeyField.clear();
    }

    @FXML
    void onSaveClick(ActionEvent event) {
        if (idField.getText().isEmpty()) {
            try (Connection c = MainDataSource.getConnection()) {
                PreparedStatement stmt = c.prepareStatement("INSERT INTO assignments (name, instructions, answer_key) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, nameField.getText());
                stmt.setString(2, instructionsField.getText());
                stmt.setString(3, answerKeyField.getText());
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    idField.setText(String.valueOf(rs.getLong(1)));
                }
            } catch (Exception e) {
                showAlert("Database Error", e.toString());
            }
        } else {
            try (Connection c = MainDataSource.getConnection()) {
                PreparedStatement stmt = c.prepareStatement("UPDATE assignments SET name = ?, instructions = ?, answer_key = ? WHERE id = ?");
                stmt.setString(1, nameField.getText());
                stmt.setString(2, instructionsField.getText());
                stmt.setString(3, answerKeyField.getText());
                stmt.setInt(4, Integer.parseInt(idField.getText()));
                stmt.executeUpdate();
            } catch (Exception e) {
                showAlert("Database Error", e.toString());
            }
        }
        refreshAssignmentList();
    }

    @FXML
    void onShowGradesClick(ActionEvent event) {
        if (idField.getText().isEmpty()) {
            showAlert("No Assignment Selected", "Please select an assignment to view grades.");
            return;
        }

        long assignmentId = Long.parseLong(idField.getText());
        showGradesWindow(assignmentId);
    }

    private void showGradesWindow(long assignmentId) {
        Stage stage = new Stage();
        stage.setTitle("Grades for Assignment " + assignmentId);

        TableView<ArrayList<String>> tableView = new TableView<>();
        ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
        ArrayList<String> headers = new ArrayList<>();

        try (Connection conn = GradingDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM grades WHERE assignment_id = ?")) {
            stmt.setLong(1, assignmentId);
            ResultSet rs = stmt.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                headers.add(metaData.getColumnLabel(i));

                TableColumn<ArrayList<String>, String> column = new TableColumn<>(metaData.getColumnLabel(i));
                final int columnIndex = i - 1;
                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    return new SimpleStringProperty(rowData.get(columnIndex));
                });
                tableView.getColumns().add(column);
            }

            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }

            if (data.isEmpty()) {
                showAlert("No Grades", "No grades available for this assignment.");
                return;
            }

            tableView.setItems(data);
            StackPane root = new StackPane();
            root.getChildren().add(tableView);
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            showAlert("Database Error", e.toString());
        }
    }

    @FXML
    void onTestButtonClick(ActionEvent event) {
        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(answerKeyField.getText())) {

            Stage stage = new Stage();
            stage.setTitle("Query Results");

            TableView<ArrayList<String>> tableView = new TableView<>();
            ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
            ArrayList<String> headers = new ArrayList<>();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                headers.add(metaData.getColumnLabel(i));

                TableColumn<ArrayList<String>, String> column = new TableColumn<>(metaData.getColumnLabel(i));
                final int columnIndex = i - 1;
                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    return new SimpleStringProperty(rowData.get(columnIndex));
                });
                tableView.getColumns().add(column);
            }

            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }

            tableView.setItems(data);
            StackPane root = new StackPane();
            root.getChildren().add(tableView);
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            showAlert("SQL Error", "Error executing the query: " + e.getMessage());
        }
    }

    @FXML
    void onDeleteClick(ActionEvent event) {
        if (idField.getText().isEmpty()) {
            showAlert("No Assignment Selected", "Please select an assignment to delete.");
            return;
        }

        long assignmentId = Long.parseLong(idField.getText());

        try (Connection c = MainDataSource.getConnection()) {
            PreparedStatement stmt = c.prepareStatement("DELETE FROM assignments WHERE id = ?");
            stmt.setLong(1, assignmentId);
            stmt.executeUpdate();
            refreshAssignmentList();
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to delete assignment: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
