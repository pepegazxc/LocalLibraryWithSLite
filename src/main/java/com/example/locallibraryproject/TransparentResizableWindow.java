package com.example.locallibraryproject;

import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransparentResizableWindow extends Application {
    private double xOffset = 0;
    private double yOffset = 0;
    private double mouseX, mouseY;
    private final double border = 5;

    @Override
    public void start(Stage stage) {
        // Создаем полупрозрачную серую сцену
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(50, 50, 50, 0.8); -fx-padding: 5;");
        Scene scene = new Scene(root, 1350, 1000, Color.rgb(50, 50, 50, 0.8));
        stage.initStyle(StageStyle.TRANSPARENT);


        //Создание панельки управления для окна
        HBox controlPanel = new HBox();
        controlPanel.setAlignment(Pos.CENTER_RIGHT);
        controlPanel.setSpacing(5);
        controlPanel.setStyle("-fx-background-color: rgba(50, 50, 50, 0.8); -fx-padding: 5;");
        controlPanel.setPrefHeight(30);

        //Кнопки управления
        Button closeButton = new Button("X");
        Button minimizeButton = new Button("_");
        Button maximizeButton = new Button("⛶");

        // Кнопка добавления новой книги
        Button addButton = new Button("Add new book");
        addButton.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            File selectFile = fileChooser.showOpenDialog(stage);

            if (selectFile != null) {
                String filePath = selectFile.getAbsolutePath();
                String fileName = selectFile.getName();
                String bookTitle = fileName.substring(0, fileName.lastIndexOf('.'));

                // Создаем контейнер для иконок и текста, если его нет
                HBox hBoxForIcons = (HBox) root.lookup("#hBoxForIcons");
                if (hBoxForIcons == null) {
                    hBoxForIcons = new HBox(10);
                    hBoxForIcons.setId("hBoxForIcons");
                    hBoxForIcons.setLayoutX(50);  // Начальная позиция X
                    hBoxForIcons.setLayoutY(50);  // Начальная позиция Y
                    hBoxForIcons.setAlignment(Pos.TOP_RIGHT);
                    hBoxForIcons.setSpacing(20);
                    root.getChildren().add(hBoxForIcons);
                }

                // Создание контейнера для иконки и текста
                VBox bookItemContainer = new VBox(5);
                bookItemContainer.setAlignment(Pos.CENTER);

                // Добавление иконки PDF
                if (filePath.endsWith(".pdf")) {
                    Image coverImage = new Image("file:C:/ForProjects/pdf-icon.png"); // Путь к иконке PDF
                    ImageView coverImageView = new ImageView(coverImage);

                    coverImageView.setFitWidth(100);  // Устанавливаем ширину
                    coverImageView.setFitHeight(100); // Устанавливаем высоту

                    // Создаем кнопку для удаления ярлыка
                    Button deleteButton = new Button("Delete");
                    deleteButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
                    HBox finalHBoxForIcons = hBoxForIcons;
                    deleteButton.setOnAction(e -> {
                        // Удаляем иконку и название книги
                        finalHBoxForIcons.getChildren().remove(bookItemContainer);
                        // Удаляем путь из базы данных
                        deleteFilePathFromDatabase(filePath);
                    });


                    // Убедимся, что обработчик кликов только на иконке
                    coverImageView.setOnMouseClicked(mouseEvent -> openBook(selectFile));

                    // Создание текста с названием книги
                    Label bookTitleLabel = new Label(bookTitle);
                    bookTitleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

                    // Добавляем иконку, текст и кнопку удаления в контейнер
                    bookItemContainer.getChildren().addAll(coverImageView, bookTitleLabel, deleteButton);
                }

                // Добавляем контейнер в hBoxForIcons
                hBoxForIcons.getChildren().add(bookItemContainer);
            }
        });

        // Добавление функционала кнопкам
        closeButton.setOnAction(e -> stage.close());
        minimizeButton.setOnAction(e -> stage.setIconified(true));
        maximizeButton.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));

        // Добавление кнопок в панель управления
        controlPanel.getChildren().addAll(minimizeButton, maximizeButton, closeButton);

        // Контейнер для панели управления
        VBox container = new VBox();
        container.getChildren().add(controlPanel);
        container.setAlignment(Pos.TOP_CENTER);
        container.setOpacity(0); // Изначально скрываем панель
        container.setStyle("-fx-background-color: rgba(50, 50, 50, 0.8); -fx-padding: 5;");

        // Обработчик событий для показа панели при наведении
        root.setOnMouseEntered(e -> container.setOpacity(1));
        root.setOnMouseExited(e -> container.setOpacity(0));

        root.getChildren().addAll(container, addButton);

        // Устанавливаем сцену
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);

        // Делаем окно перетаскиваемым
        makeWindowDraggable(root, stage);

        // Делаем окно изменяемым
        makeWindowResizable(root, stage);

        stage.setScene(scene);
        stage.show();
    }

    // Сохранение пути файла в базу данных
    private void saveFilePathToDatabase(String filePath) {
        String url = "jdbc:sqlite:C:/sqlite/LocalLibrary.db"; // Замените путь к вашей базе данных
        String sql = "INSERT INTO files (path) VALUES (?);";

        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

                preparedStatement.setString(1, filePath);
                preparedStatement.executeUpdate();
                System.out.println("File saved in DB: " + filePath);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Драйвер SQLite не найден: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
    private void deleteFilePathFromDatabase(String filePath) {
        String url = "jdbc:sqlite:C:/sqlite/LocalLibrary.db"; // Путь к базе данных
        String sql = "DELETE FROM files WHERE path = ?;";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, filePath);
            pstmt.executeUpdate();
            System.out.println("Путь успешно удален из базы данных: " + filePath);
        } catch (SQLException e) {
            System.err.println("Ошибка при удалении пути из базы данных: " + e.getMessage());
        }
    }

    private void openBook(File bookFile) {
        try {
            if (bookFile.getName().endsWith(".pdf")) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(bookFile);  // Открывает PDF с помощью дефолтного приложения
            } else {
                // Для изображений или других типов файлов можно добавить обработку
                System.out.println("Это не PDF, но можно добавить обработку для других типов.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка при открытии книги: " + e.getMessage());
        }
    }


    // Метод для перетаскивания окна
    private void makeWindowDraggable(StackPane root, Stage stage) {
        root.setOnMousePressed(event -> {
            if (root.getCursor() == Cursor.DEFAULT && event.isPrimaryButtonDown()) {
                xOffset = event.getScreenX() - stage.getX();
                yOffset = event.getScreenY() - stage.getY();
            }
        });

        root.setOnMouseDragged(event -> {
            if (root.getCursor() == Cursor.DEFAULT && event.isPrimaryButtonDown()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    // Метод для изменения размера окна
    private void makeWindowResizable(StackPane root, Stage stage) {
        root.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
            double width = stage.getWidth();
            double height = stage.getHeight();

            boolean resizeRight = mouseX > width - border;
            boolean resizeLeft = mouseX < border;
            boolean resizeBottom = mouseY > height - border;
            boolean resizeTop = mouseY < border;

            if (resizeRight && resizeBottom) {
                root.setCursor(Cursor.SE_RESIZE); // ↘
            } else if (resizeLeft && resizeBottom) {
                root.setCursor(Cursor.SW_RESIZE); // ↙
            } else if (resizeRight && resizeTop) {
                root.setCursor(Cursor.NE_RESIZE); // ↗
            } else if (resizeLeft && resizeTop) {
                root.setCursor(Cursor.NW_RESIZE); // ↖
            } else if (resizeRight) {
                root.setCursor(Cursor.E_RESIZE); // ↔
            } else if (resizeLeft) {
                root.setCursor(Cursor.W_RESIZE); // ↔
            } else if (resizeBottom) {
                root.setCursor(Cursor.S_RESIZE); // ↕
            } else if (resizeTop) {
                root.setCursor(Cursor.N_RESIZE); // ↕
            } else {
                root.setCursor(Cursor.DEFAULT);
            }
        });

        root.setOnMouseDragged(event -> {
            double width = stage.getWidth();
            double height = stage.getHeight();

            if (root.getCursor() == Cursor.E_RESIZE) {
                stage.setWidth(event.getX());
            } else if (root.getCursor() == Cursor.S_RESIZE) {
                stage.setHeight(event.getY());
            } else if (root.getCursor() == Cursor.SE_RESIZE) {
                stage.setWidth(event.getX());
                stage.setHeight(event.getY());
            } else if (root.getCursor() == Cursor.W_RESIZE) {
                double newWidth = width - (event.getScreenX() - stage.getX());
                if (newWidth > 100) {
                    stage.setX(event.getScreenX());
                    stage.setWidth(newWidth);
                }
            } else if (root.getCursor() == Cursor.N_RESIZE) {
                double newHeight = height - (event.getScreenY() - stage.getY());
                if (newHeight > 100) {
                    stage.setY(event.getScreenY());
                    stage.setHeight(newHeight);
                }
            } else if (root.getCursor() == Cursor.NW_RESIZE) {
                double newWidth = width - (event.getScreenX() - stage.getX());
                double newHeight = height - (event.getScreenY() - stage.getY());
                if (newWidth > 100) {
                    stage.setX(event.getScreenX());
                    stage.setWidth(newWidth);
                }
                if (newHeight > 100) {
                    stage.setY(event.getScreenY());
                    stage.setHeight(newHeight);
                }
            } else if (root.getCursor() == Cursor.NE_RESIZE) {
                double newHeight = height - (event.getScreenY() - stage.getY());
                if (newHeight > 100) {
                    stage.setY(event.getScreenY());
                    stage.setHeight(newHeight);
                }
                stage.setWidth(event.getX());
            } else if (root.getCursor() == Cursor.SW_RESIZE) {
                double newWidth = width - (event.getScreenX() - stage.getX());
                if (newWidth > 100) {
                    stage.setX(event.getScreenX());
                    stage.setWidth(newWidth);
                }
                stage.setHeight(event.getY());
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
