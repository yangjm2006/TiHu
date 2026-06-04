package com.tihu.frontend.controller;

import com.tihu.frontend.service.MockBackendService;
import com.tihu.frontend.service.MockBackendService.BookSortMode;
import com.tihu.frontend.utils.AppContext;
import com.tihu.frontend.utils.AppContext.BookDetailReturnTarget;
import com.tihu.frontend.utils.ImageDataUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class AdminBooksController implements MainContentController {

    @FXML private TableView<MockBackendService.BookCard> tableView;
    @FXML private TableColumn<MockBackendService.BookCard, String> titleColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> authorColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> ratingColumn;
    @FXML private TableColumn<MockBackendService.BookCard, String> tagsColumn;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField tagsField;
    @FXML private TextArea introArea;
    @FXML private TextField searchTitleField;
    @FXML private TextField searchTagsField;
    @FXML private ComboBox<String> sortBox;
    @FXML private ImageView coverPreview;
    @FXML private Label coverStatusLabel;
    @FXML private Label pageInfoLabel;
    @FXML private Label messageLabel;

    private final AppContext context = AppContext.getInstance();
    private static final int PAGE_SIZE = 8;
    private MainController mainController;
    private Long selectedBookId;
    private String selectedCoverImage;
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean restoringState;
    private boolean updatingTable;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title()));
        authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().author()));
        ratingColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().averageScoreText()));
        tagsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().tagsSummary()));

        sortBox.getItems().addAll("默认排序", "按评分排序", "按书名排序");
        sortBox.getSelectionModel().select(0);
        sortBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {
            if (restoringState) {
                return;
            }
            currentPage = 1;
            saveState();
            refresh();
        });

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> fillForm(newValue));
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenDetail();
            }
        });
    }

    @Override
    public void onShow() {
        restoreState();
        refresh();
    }

    @FXML
    private void onAddBook() {
        try {
            context.service().addBook(titleField.getText(), authorField.getText(), introArea.getText(), tagsField.getText(),
                    selectedCoverImage);
            clearForm();
            currentPage = 1;
            saveState();
            refresh();
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onUpdateBook() {
        if (selectedBookId == null) {
            messageLabel.setText("请先选择要编辑的图书");
            return;
        }
        try {
            context.service().updateBook(selectedBookId, titleField.getText(), authorField.getText(), introArea.getText(),
                    tagsField.getText(), selectedCoverImage);
            saveState();
            refresh();
            messageLabel.setText("图书已更新");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void onDeleteBook() {
        MockBackendService.BookCard selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("请先选择图书");
            return;
        }
        context.service().deleteBook((long) selected.id());
        clearForm();
        saveState();
        refresh();
    }

    @FXML
    private void onSearch() {
        currentPage = 1;
        saveState();
        refresh();
    }

    @FXML
    private void onResetSearch() {
        searchTitleField.clear();
        searchTagsField.clear();
        currentPage = 1;
        sortBox.getSelectionModel().select(0);
        saveState();
        refresh();
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            saveState();
            refresh();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            saveState();
            refresh();
        }
    }

    @FXML
    private void onOpenDetail() {
        MockBackendService.BookCard selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null && mainController != null) {
            selectedBookId = selected.id();
            saveState();
            mainController.openBookDetail(selected.id(), BookDetailReturnTarget.ADMIN_BOOKS);
        }
    }

    @FXML
    private void onUploadCover() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择图书封面");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = chooser.showOpenDialog(coverPreview == null ? null : coverPreview.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            selectedCoverImage = ImageDataUtil.toDataUri(file);
            updateCoverPreview();
            messageLabel.setText("封面已选择，保存图书后生效");
        } catch (Exception ex) {
            messageLabel.setText("读取封面失败：" + ex.getMessage());
        }
    }

    @FXML
    private void onClearCover() {
        selectedCoverImage = "";
        updateCoverPreview();
        messageLabel.setText("封面已清空，保存图书后生效");
    }

    private void fillForm(MockBackendService.BookCard selected) {
        if (selected == null) {
            if (updatingTable) {
                return;
            }
            selectedBookId = null;
            context.setAdminSelectedBookId(null);
            return;
        }
        selectedBookId = selected.id();
        context.setAdminSelectedBookId(selectedBookId);
        MockBackendService.Book book = context.service().getBook((long) selected.id());
        titleField.setText(book.title());
        authorField.setText(book.author());
        introArea.setText(book.intro());
        tagsField.setText(String.join(" ", book.tags()));
        selectedCoverImage = book.coverImage();
        updateCoverPreview();
        messageLabel.setText("已载入选中图书，标签请用空格分隔后直接修改再更新");
    }

    private void clearForm() {
        selectedBookId = null;
        context.setAdminSelectedBookId(null);
        tableView.getSelectionModel().clearSelection();
        titleField.clear();
        authorField.clear();
        tagsField.clear();
        introArea.clear();
        selectedCoverImage = null;
        updateCoverPreview();
    }

    private void refresh() {
        MockBackendService.BookListPage page = context.service().listBooks(searchTitleField.getText(), searchTags(), currentPage, PAGE_SIZE, currentSortMode());
        totalPages = page.totalPages();
        currentPage = Math.min(Math.max(1, page.page()), totalPages);
        Long desiredSelection = context.adminSelectedBookId();
        updatingTable = true;
        try {
            tableView.getItems().setAll(page.items());
        } finally {
            updatingTable = false;
        }
        context.setAdminBookPage(currentPage);
        restoreSelection(desiredSelection);
        pageInfoLabel.setText("第 " + currentPage + "/" + totalPages + " 页，共 " + page.totalItems() + " 本");
        messageLabel.setText("管理员可增删图书，也可上传或修改封面");
    }

    private void restoreState() {
        restoringState = true;
        try {
            searchTitleField.setText(context.adminBookTitleKeyword());
            searchTagsField.setText(context.adminBookTagsText());
            currentPage = context.adminBookPage();
            sortBox.getSelectionModel().select(sortIndex(context.adminBookSortMode()));
            selectedBookId = context.adminSelectedBookId();
        } finally {
            restoringState = false;
        }
    }

    private void saveState() {
        context.setAdminBookTitleKeyword(searchTitleField.getText());
        context.setAdminBookTagsText(searchTagsField.getText());
        context.setAdminBookPage(currentPage);
        context.setAdminBookSortMode(currentSortMode());
        context.setAdminSelectedBookId(selectedBookId);
    }

    private void restoreSelection(Long id) {
        if (id == null) {
            return;
        }
        for (MockBackendService.BookCard item : tableView.getItems()) {
            if (item.id() == id) {
                tableView.getSelectionModel().select(item);
                tableView.scrollTo(item);
                return;
            }
        }
    }

    private void updateCoverPreview() {
        if (coverPreview == null) {
            return;
        }
        Image image = ImageDataUtil.image(selectedCoverImage);
        coverPreview.setImage(image);
        if (coverStatusLabel != null) {
            coverStatusLabel.setText(image == null ? "当前无封面" : "已选择封面");
        }
    }

    private List<String> searchTags() {
        return Arrays.stream(searchTagsField.getText() == null ? new String[0] : searchTagsField.getText().trim().split("\\s+"))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    private BookSortMode currentSortMode() {
        int index = sortBox.getSelectionModel().getSelectedIndex();
        return switch (index) {
            case 1 -> BookSortMode.RATING_DESC;
            case 2 -> BookSortMode.TITLE_ASC;
            default -> BookSortMode.DEFAULT;
        };
    }

    private int sortIndex(BookSortMode sortMode) {
        return switch (sortMode) {
            case RATING_DESC -> 1;
            case TITLE_ASC -> 2;
            default -> 0;
        };
    }
}

