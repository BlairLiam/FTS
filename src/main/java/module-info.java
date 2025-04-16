module com.example.fts {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.fts to javafx.fxml;
    exports com.example.fts;
}