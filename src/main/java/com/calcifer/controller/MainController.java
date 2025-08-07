package com.calcifer.controller;

import com.calcifer.model.CheckItem;
import com.calcifer.model.CheckResult;
import com.calcifer.service.AdbService;
import com.calcifer.service.CheckService;
import com.calcifer.utils.AdbUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Callback;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * @author CYC
 */
public class MainController implements Initializable {
    /**
     * PREF_FILE 用于存储应用的首选项文件名
     */
    private static final String PREF_FILE = "preferences.properties";
    private final ObservableList<CheckResult> results = FXCollections.observableArrayList();
    private final FilteredList<CheckResult> filteredResults = new FilteredList<>(results);
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Map<CheckResult, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final List<CheckResult> pendingTasks = new ArrayList<>();
    @FXML
    private TableView<CheckResult> resultTable;
    @FXML
    private TableColumn<CheckResult, Integer> indexColumn;
    @FXML
    private TableColumn<CheckResult, String> categoryColumn;
    @FXML
    private TableColumn<CheckResult, String> itemNameColumn;
    @FXML
    private TableColumn<CheckResult, String> versionColumn;
    @FXML
    private TableColumn<CheckResult, String> prerequisiteColumn;
    @FXML
    private TableColumn<CheckResult, String> propertyColumn;
    @FXML
    private TableColumn<CheckResult, String> expectedColumn;
    @FXML
    private TableColumn<CheckResult, String> resultColumn;
    @FXML
    private TableColumn<CheckResult, String> statusColumn;
    @FXML
    private TableColumn<CheckResult, String> noteColumn;
    @FXML
    private TableColumn<CheckResult, Void> actionColumn;
    @FXML
    private TextField searchField;
    @FXML
    private Button checkAllButton;
    @FXML
    private Button stopAllButton;
    @FXML
    private Button continueButton;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Circle statusLight;
    @FXML
    private Circle completionLight;
    @FXML
    private Label statusLabel;
    @FXML
    private Label summaryLabel;
    @FXML
    private VBox detailBox;
    @FXML
    private VBox mainContainer;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private Label detailIndex;
    @FXML
    private Label detailCategory;
    @FXML
    private Label detailItemName;
    @FXML
    private Label detailVersion;
    @FXML
    private Label detailPrerequisite;
    @FXML
    private Label detailProperty;
    @FXML
    private Label detailExpected;
    @FXML
    private Label detailResultValue;
    @FXML
    private Label detailStatus;
    @FXML
    private Label detailNote;
    @FXML
    private Button checkCategoryButton;

    private ScheduledExecutorService statusChecker;
    private boolean isPaused = false;
    private boolean lastConnected = false;
    /**
     * adb连接状态
     */
    private boolean isAdbConnected = false;
    private String softwareVersion;
    private Properties preferences;
    private File lastExportDirectory;

    public static final String APPROVED = "通过";
    public static final String FAILED = "失败";
    public static final String READ_ONLY = "只读";
    public static final String PENDING = "待处理";
    public static final String STOPED = "暂停";
    public static final String WRONG = "错误";
    public static final String ALL_STATUS = "全部";
    public static final String CHECKING = "检查中";
    public static final String ALL_CATEGORY = "所有类别";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTable();
        loadCheckItems();
        setupSearchFilter();
        setupCategoryFilter();
        setupStatusFilter();
        startAdbStatusChecker();

        // 添加行选择监听器
        resultTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && !newSelection.isRunning()) {
                showDetail(newSelection);
            }
        });

        // 初始隐藏详情区域
        detailBox.setVisible(false);

        // 设置SplitPane不可调整
        mainSplitPane.setDividerPositions(0.9, 0.1);
        checkCategoryButton.setDisable(true);

        // 加载首选项
        preferences = new Properties();
        try (InputStream is = new FileInputStream(PREF_FILE)) {
            preferences.load(is);
            String lastDir = preferences.getProperty("lastExportDirectory");
            if (lastDir != null) {
                lastExportDirectory = new File(lastDir);
            }
        } catch (IOException e) {
            // 文件不存在是正常情况
        }

        makeLabelsCopyable(
                detailIndex, detailCategory, detailItemName, detailVersion,
                detailPrerequisite, detailProperty, detailExpected,
                detailResultValue, detailStatus, detailNote
        );
    }

    private void initializeTable() {
        // 禁止列重新排序
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // 固定序号列在最左侧
        indexColumn.setSortable(true);
        resultTable.getSortOrder().add(indexColumn);
        // 固定操作列在最右侧
        actionColumn.setSortable(false);

        indexColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.02));
        categoryColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.06));
        itemNameColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.16));
        versionColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.03));
        prerequisiteColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.06));
        propertyColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.2));
        expectedColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.1));
        resultColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.1));
        statusColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.05));
        noteColumn.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.165));

        // 设置表格列
        indexColumn.setCellValueFactory(cellData -> cellData.getValue().indexProperty().asObject());
        categoryColumn.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        itemNameColumn.setCellValueFactory(cellData -> cellData.getValue().itemNameProperty());
        versionColumn.setCellValueFactory(cellData -> cellData.getValue().versionProperty());
        prerequisiteColumn.setCellValueFactory(cellData -> cellData.getValue().prerequisiteProperty());
        propertyColumn.setCellValueFactory(cellData -> cellData.getValue().propertyValueProperty());
        expectedColumn.setCellValueFactory(cellData -> cellData.getValue().expectedValueProperty());
        resultColumn.setCellValueFactory(cellData -> cellData.getValue().resultValueProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        noteColumn.setCellValueFactory(cellData -> cellData.getValue().noteProperty());

        // 禁用其他列的排序功能
        categoryColumn.setSortable(false);
        itemNameColumn.setSortable(false);
        versionColumn.setSortable(false);
        prerequisiteColumn.setSortable(false);
        propertyColumn.setSortable(false);
        expectedColumn.setSortable(false);
        resultColumn.setSortable(false);
        noteColumn.setSortable(false);
        actionColumn.setSortable(false);

        // 禁止列移动
        resultTable.getColumns().forEach(column -> column.setReorderable(false));

        // 设置所有列自动换行
        Callback<TableColumn<CheckResult, String>, TableCell<CheckResult, String>> wrapCellFactory =
                tc -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setWrapText(true);
                            setText(item);
                        }
                    }
                };

        categoryColumn.setCellFactory(wrapCellFactory);
        itemNameColumn.setCellFactory(wrapCellFactory);
        // 版本列自动换行
        versionColumn.setCellFactory(wrapCellFactory);
        // 前提列自动换行
        prerequisiteColumn.setCellFactory(wrapCellFactory);
        propertyColumn.setCellFactory(wrapCellFactory);
        expectedColumn.setCellFactory(wrapCellFactory);
        resultColumn.setCellFactory(wrapCellFactory);
        noteColumn.setCellFactory(wrapCellFactory);

        // 状态列着色
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (APPROVED.equals(item)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (FAILED.equals(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (WRONG.equals(item)) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if (READ_ONLY.equals(item)) {
                        setStyle("-fx-text-fill: #3498DB; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // 版本和前提列可编辑
        versionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        versionColumn.setOnEditCommit(event -> {
            CheckResult result = event.getRowValue();
            result.setVersion(event.getNewValue());
        });

        prerequisiteColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        prerequisiteColumn.setOnEditCommit(event -> {
            CheckResult result = event.getRowValue();
            result.setPrerequisite(event.getNewValue());
        });

        // 备注列可编辑
        noteColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        noteColumn.setOnEditCommit(event -> {
            CheckResult result = event.getRowValue();
            result.setNote(event.getNewValue());
        });

        // 添加操作列
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button checkButton = new Button("Check");

            {
                checkButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 3;");
                checkButton.setOnAction(event -> {
                    CheckResult result = getTableView().getItems().get(getIndex());
                    startSingleCheck(result);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CheckResult result = getTableView().getItems().get(getIndex());
                    checkButton.setDisable(!isAdbConnected || result.isRunning());
                    setGraphic(checkButton);
                }
            }
        });

        // 应用排序
        SortedList<CheckResult> sortedResults = new SortedList<>(filteredResults);
        sortedResults.comparatorProperty().bind(resultTable.comparatorProperty());
        resultTable.setItems(sortedResults);
    }

    /**
    * 显示选中项的详细信息
    */
    private void showDetail(CheckResult result) {
        if (!detailBox.isVisible()) {
            // 显示详情区域并调整SplitPane
            detailBox.setVisible(true);

            // 设置详情页最大高度为主窗体的30%
            double maxHeight = mainSplitPane.getHeight() * 0.3;
            detailBox.setMaxHeight(maxHeight);

            // 设置分割比例
            mainSplitPane.setDividerPositions(0.7);
        }

        // 设置详情到各个Label
        detailIndex.setText(String.valueOf(result.getIndex()));
        detailCategory.setText(result.getCategory());
        detailItemName.setText(result.getItemName());
        detailVersion.setText(result.getVersion());
        detailPrerequisite.setText(result.getPrerequisite());
        detailProperty.setText(result.getPropertyValue());
        detailExpected.setText(result.getExpectedValue());
        detailResultValue.setText(result.getResultValue());
        detailStatus.setText(result.getStatus());
        detailNote.setText(result.getNote());

        // 设置状态颜色
        if (APPROVED.equals(result.getStatus())) {
            detailStatus.setTextFill(Color.GREEN);
        } else if (FAILED.equals(result.getStatus())) {
            detailStatus.setTextFill(Color.RED);
        } else if (WRONG.equals(result.getStatus())) {
            detailStatus.setTextFill(Color.ORANGE);
        } else if (READ_ONLY.equals(result.getStatus())) {
            detailStatus.setTextFill(Color.BLUE);
        } else {
            detailStatus.setTextFill(Color.BLACK);
        }
    }

    private void loadCheckItems() {
        results.clear();
        int index = 1;
        for (CheckItem item : AdbService.createCheckItems()) {

            // 将多个期待值连接成字符串显示
            String expectedValues = item.getCommandExpectedMap().values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.joining("\n"));

            String displayCommand = String.join("\n", item.getAdbCommands());

            results.add(new CheckResult(
                    index++,
                    item.getCategory().getName(),
                    item.getName(),
                    item.getVersion(),
                    item.getPrerequisite(),
                    displayCommand,
                    expectedValues,
                    item.getNote()
            ));
        }
        updateSummary();
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredResults.setPredicate(result -> {
                if (newValue == null || newValue.isEmpty()) {return true;}

                String lowerCaseFilter = newValue.toLowerCase();
                return result.getItemName().toLowerCase().contains(lowerCaseFilter) ||
                        result.getCategory().toLowerCase().contains(lowerCaseFilter) ||
                        result.getNote().toLowerCase().contains(lowerCaseFilter) ||
                        result.getVersion().toLowerCase().contains(lowerCaseFilter) ||
                        result.getPrerequisite().toLowerCase().contains(lowerCaseFilter);
            });
        });
    }

    private void setupCategoryFilter() {
        Set<String> categories = new HashSet<>();
        for (CheckResult result : results) {
            categories.add(result.getCategory());
        }

        categoryFilter.getItems().add(ALL_CATEGORY);
        categoryFilter.getItems().addAll(categories);
        categoryFilter.getSelectionModel().selectFirst();

        categoryFilter.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            updateFilters();
            resortItems();
        });
    }

    private void resortItems() {
        // 按分类重新排序
        resultTable.getSortOrder().clear();
        resultTable.getSortOrder().add(categoryColumn);
        resultTable.getSortOrder().add(indexColumn);
        resultTable.sort();
    }

    /**
     * 设置状态过滤器
     * */
    private void setupStatusFilter() {
        statusFilter.getItems().addAll(ALL_STATUS, PENDING, CHECKING, APPROVED, FAILED, WRONG,READ_ONLY, STOPED);
        statusFilter.getSelectionModel().selectFirst();

        statusFilter.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            updateFilters();
        });
    }

    /**
     * 更新过滤条件
     * */
    private void updateFilters() {
        String category = categoryFilter.getSelectionModel().getSelectedItem();
        String status = statusFilter.getSelectionModel().getSelectedItem();

        filteredResults.setPredicate(result -> {
            // 类别过滤
            if (category != null && !ALL_CATEGORY.equals(category) &&
                    !result.getCategory().equals(category)) {
                return false;
            }
            // 状态过滤
            return status == null || ALL_STATUS.equals(status) ||
                    result.getStatus().equals(status);
        });
    }

    private void startAdbStatusChecker() {
        statusChecker = Executors.newSingleThreadScheduledExecutor();
        statusChecker.scheduleAtFixedRate(() -> {
            boolean connected = checkAdbConnection();
            Platform.runLater(() -> {
                if (connected) {
                    statusLight.setFill(Color.valueOf("#2ecc71"));
                    statusLabel.setText("已连接");
                    statusLabel.setStyle("-fx-text-fill: #27ae60;");
                    isAdbConnected = true;
                    enableAllCheckButtons(true);

                    // 启用检查按钮
                    checkAllButton.setDisable(false);
                    continueButton.setDisable(false);
                    stopAllButton.setDisable(false);
                    checkCategoryButton.setDisable(false);

                    // 如果从断开状态变为连接状态，重置所有检查
                    if (!lastConnected) {
                        resetAllChecks();
                    }
                } else {
                    statusLight.setFill(Color.valueOf("#e74c3c"));
                    statusLabel.setText("未连接");
                    statusLabel.setStyle("-fx-text-fill: #c0392b;");
                    isAdbConnected = false;

                    // 禁用检查按钮
                    checkAllButton.setDisable(true);
                    continueButton.setDisable(true);
                    stopAllButton.setDisable(true);
                    checkCategoryButton.setDisable(true);
                    enableAllCheckButtons(false);
                }
                lastConnected = connected;
            });
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void enableAllCheckButtons(boolean enable) {
        for (CheckResult result : results) {
            // 停止所有运行中的检查
            result.setRunning(false);
        }
        // 刷新表格以更新按钮状态
        resultTable.refresh();
    }

    /**
     * 重置所有检查
     * */
    private void resetAllChecks() {
        for (CheckResult result : results) {
            result.setResultValue("");
            result.setStatus(PENDING);
            result.setRunning(false);
        }
        runningTasks.clear();
        pendingTasks.clear();
        isPaused = false;
        updateSummary();
    }

    private boolean checkAdbConnection() {
        try {
            Process process = Runtime.getRuntime().exec("adb devices");
            process.waitFor(2, TimeUnit.SECONDS);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean devicesFound = false;
                while ((line = reader.readLine()) != null) {
                    if (line.endsWith("device")) {
                        devicesFound = true;
                    }
                }
                return devicesFound;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @FXML
    private void handleCheckAll() {
        if (!isAdbConnected) {
            showAlert("ADB 未连接", "请先连接设备");
            return;
        }

        isPaused = false;
        pendingTasks.clear();

        // 重置所有检查项状态
        for (CheckResult result : results) {
            result.setResultValue("");
            result.setStatus(PENDING);
            result.setRunning(false);
        }

        // 添加所有检查项到待处理列表
        for (CheckResult result : filteredResults) {
            pendingTasks.add(result);
        }

        // 开始执行待处理任务
        executePendingTasks();
    }

    @FXML
    private void handleStopAll() {

        for (CheckResult result : new ArrayList<>(runningTasks.keySet())) {
            if (!pendingTasks.contains(result)) {
                pendingTasks.addFirst(result);
            }
        }

        if (!isAdbConnected) {
            showAlert("ADB 未连接", "请先连接设备");
            return;
        }

        isPaused = true;

        // 停止正在运行的任务
        for (Map.Entry<CheckResult, Future<?>> entry : runningTasks.entrySet()) {
            if (!entry.getValue().isDone()) {
                entry.getValue().cancel(true);
                entry.getKey().setStatus(STOPED);
                entry.getKey().setRunning(false);
            }
        }
        runningTasks.clear();
        updateSummary();
    }

    @FXML
    private void handleContinue() {

        for (CheckResult result : results) {
            if (STOPED.equals(result.getStatus())) {
                result.setStatus(PENDING);
                if (!pendingTasks.contains(result)) {
                    pendingTasks.add(result);
                }
            }
        }

        if (!isAdbConnected) {
            showAlert("ADB 未连接", "请连接设备以继续检查");
            return;
        }

        if (isPaused && !pendingTasks.isEmpty()) {
            isPaused = false;
            executePendingTasks();
        }

        executePendingTasks();
    }

    @FXML
    private void handleCheckByCategory() {
        if (!isAdbConnected) {
            showAlert("ADB 未连接", "请连接设备以执行分类检查");
            return;
        }

        isPaused = false;
        pendingTasks.clear();

        String category = categoryFilter.getSelectionModel().getSelectedItem();
        if (category != null && !category.isEmpty() && !ALL_CATEGORY.equals(category)) {
            // 重置该类别的检查项状态
            for (CheckResult result : results) {
                if (result.getCategory().equals(category)) {
                    result.setResultValue("");
                    result.setStatus(PENDING);
                    result.setRunning(false);
                }
            }

            // 添加该类别的检查项到待处理列表
            for (CheckResult result : results) {
                if (result.getCategory().equals(category)) {
                    pendingTasks.add(result);
                }
            }
            executePendingTasks();
        }
    }

    private void executePendingTasks() {
        if (!pendingTasks.isEmpty()) {
            startSingleCheck(pendingTasks.getFirst());
        }
    }

    /**
     * 显示警告对话框
     * */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startSingleCheck(CheckResult result) {
        if (result.isRunning() || isPaused) {return;}

        // 重置该项状态
        result.setResultValue("");
        result.setStatus("检查中...");

        // 查找对应的CheckItem
        Optional<CheckItem> itemOpt = AdbService.createCheckItems().stream()
                .filter(i -> i.getName().equals(result.getItemName()) &&
                        i.getCategory().getName().equals(result.getCategory()))
                .findFirst();

        if (itemOpt.isPresent()) {
            CheckItem item = itemOpt.get();
            result.setRunning(true);

            // 滚动到当前检查项
            Platform.runLater(() -> {
                resultTable.scrollTo(result);
                resultTable.getSelectionModel().clearSelection();
                resultTable.getSelectionModel().select(result);
                resultTable.getFocusModel().focus(resultTable.getSelectionModel().getSelectedIndex());
            });

            Future<?> future = executor.submit(() -> {
                try {
                    if (isPaused) {return;}

                    StringBuilder allResults = new StringBuilder();
                    List<Boolean> commandResults = new ArrayList<>();
                    final boolean[] allPassed = {true};
                    final boolean[] anyPassed = {false};

                    // 判断是否需要使用shell会话执行
                    if (item.isShellSessionRequired()) {
                        String adbResult = AdbUtils.executeShellCommands(item.getAdbCommands());

                        // 简化输出格式
                        if (adbResult.trim().isEmpty()) {
                            allResults.append("输出: 空");
                        } else {
                            // 提取关键信息
                            String filteredResult = Arrays.stream(adbResult.split("\n"))
                                    .filter(line -> line.contains("MEOW") || line.contains("libARC"))
                                    .collect(Collectors.joining("\n"));

                            allResults.append("输出: ").append(filteredResult.trim());
                        }

                        // 评估结果
                        boolean currentPassed = false;
                        for (int i = 0; i < item.getAdbCommands().size(); i++) {
                            List<String> expectedValues = item.getExpectedValuesForCommand(i);
                            if (!expectedValues.isEmpty()) {
                                for (String expected : expectedValues) {
                                    if (adbResult.contains(expected)) {
                                        currentPassed = true;
                                        break;
                                    }
                                }
                            }
                        }


                        commandResults.add(currentPassed);

                        // 设置最终状态
                        if (item.getConditionType() == CheckService.ConditionType.QUERY_ONLY) {
                            allPassed[0] = true;
                        } else if (item.isAllMustPass()) {
                            allPassed[0] = currentPassed;
                        } else {
                            anyPassed[0] = currentPassed;
                        }
                    } else {
                        // 原有的单个命令执行逻辑
                        int commandIndex = 0;
                        int outputIndex = 1; // 添加输出编号

                        for (String command : item.getAdbCommands()) {
                            String adbResult = AdbUtils.executeAdbCommand(command);

                            // 简化输出格式 - 只显示结果
                            if (!adbResult.trim().isEmpty()) {
                                allResults.append("结果").append(outputIndex++).append(": ")
                                        .append(adbResult.trim()).append("\n");
                            } else {
                                allResults.append("结果").append(outputIndex++).append(": 空\n");
                            }

                            // 获取对应的期待值（如果有）
                            List<String> expectedValues = item.getExpectedValuesForCommand(commandIndex);

                            // 评估结果
                            if (!expectedValues.isEmpty()) {
                                boolean currentPassed = false;
                                for (String expectedValue : expectedValues) {
                                    currentPassed = evaluateCondition(item, adbResult, commandIndex, expectedValue);
                                    if (currentPassed) {break;} // 任意期待值满足即可
                                }

                                commandResults.add(currentPassed);

                                if (item.isAllMustPass()) {
                                    allPassed[0] = allPassed[0] && currentPassed;
                                } else {
                                    anyPassed[0] = anyPassed[0] || currentPassed;
                                }
                            }
                            commandIndex++;
                        }
                    }

                    Platform.runLater(() -> {
                        result.setResultValue(allResults.toString().trim());
                        result.setRunning(false);
                        runningTasks.remove(result);
                        pendingTasks.remove(result);
                        updateSummary();

                        // 设置最终状态
                        if (item.getConditionType() == CheckService.ConditionType.QUERY_ONLY) {
                            result.setStatus(READ_ONLY);
                        } else if (item.isAllMustPass()) {
                            result.setStatus(allPassed[0] ? APPROVED : FAILED);
                        } else {
                            result.setStatus(anyPassed[0] ? APPROVED : FAILED);
                        }

                        // 刷新详情页
                        if (detailBox.isVisible() && resultTable.getSelectionModel().getSelectedItem() == result) {
                            showDetail(result);
                        }

                        // 检查完成状态
                        if (pendingTasks.isEmpty() && runningTasks.isEmpty()) {
                            completionLight.setFill(Color.valueOf("#2ecc71"));
                        }

                        // 自动执行下一项
                        if (!pendingTasks.isEmpty() && !isPaused) {
                            startSingleCheck(pendingTasks.getFirst());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        result.setResultValue("Error: " + e.getMessage());
                        result.setStatus("Error");
                        result.setRunning(false);
                        runningTasks.remove(result);
                        pendingTasks.remove(result);
                        updateSummary();

                        // 继续执行下一个任务
                        if (!pendingTasks.isEmpty() && !isPaused) {
                            startSingleCheck(pendingTasks.getFirst());
                        }
                    });
                } finally {
                    // 确保任务状态更新
                    Platform.runLater(() -> {
                        if (result.isRunning()) {
                            result.setRunning(false);
                            runningTasks.remove(result);
                            pendingTasks.remove(result);
                            updateSummary();
                        }
                    });
                }
            });

            runningTasks.put(result, future);
            updateSummary();
        }
    }

    /**
     * 新增带期待值参数的方法
     * */
    private boolean evaluateCondition(CheckItem item, String actualValue, int commandIndex, String expectedValue) {
        List<String> expectedValues = expectedValue != null ?
                Collections.singletonList(expectedValue) :
                item.getExpectedValuesForCommand(commandIndex);

        switch (item.getConditionType()) {
            case EQUALS:
                for (String expected : expectedValues) {
                    if (actualValue.trim().equals(expected.trim())) {
                        return true;
                    }
                }
                return false;

            case CONTAINS:
                for (String expected : expectedValues) {
                    if (actualValue.contains(expected)) {
                        return true;
                    }
                }
                return false;

            case MULTI_CONDITION:
                for (String expected : expectedValues) {
                    if (!actualValue.contains(expected)) {
                        return false;
                    }
                }
                return true;

            case QUERY_ONLY:
                return true;

            case NOT_CONTAINS:
                for (String expected : expectedValues) {
                    if (actualValue.contains(expected)) {
                        return false;
                    }
                }
                return true;

            case LOG_FILTER:
                return evaluateLogFilter(item, actualValue, commandIndex);

            default:
                return false;
        }
    }

    /**
    * 日志筛选特殊处理
    * */
    private boolean evaluateLogFilter(CheckItem item, String logOutput, int commandIndex) {
        for (String expected : item.getExpectedValuesForCommand(commandIndex)) {
            if (logOutput.contains(expected)) {
                return true;
            }
        }
        return false;
    }


    private void updateSummary() {
        long total = results.size();
        long passed = results.stream().filter(r -> APPROVED.equals(r.getStatus())).count();
        long failed = results.stream().filter(r -> FAILED.equals(r.getStatus())).count();
        long infoed = results.stream().filter(r -> READ_ONLY.equals(r.getStatus())).count();
        long running = results.stream().filter(CheckResult::isRunning).count();
        long pending = results.stream().filter(r ->
                PENDING.equals(r.getStatus()) || STOPED.equals(r.getStatus())).count();
        long error = results.stream().filter(r -> WRONG.equals(r.getStatus())).count();

        summaryLabel.setText(String.format("全部: %d | 通过: %d | 失败: %d | 只读: %d | 错误: %d | 运行中: %d | 待处理: %d",
                total, passed, failed, infoed, error, running, pending));

        // 更新完成状态指示灯
        if (pending == 0 && running == 0 && (passed + failed + infoed + error) == total) {
            completionLight.setFill(Color.GREEN);
        } else {
            completionLight.setFill(Color.GRAY);
        }
    }


    private void makeLabelsCopyable(Label... labels) {
        for (Label label : labels) {
            // 设置标签可选择
            label.getStyleClass().add("copyable-label");

            // 添加上下文菜单
            ContextMenu contextMenu = new ContextMenu();
            MenuItem copyItem = new MenuItem("复制");
            copyItem.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(label.getText());
                clipboard.setContent(content);
            });
            contextMenu.getItems().add(copyItem);

            // 设置右键菜单
            label.setContextMenu(contextMenu);

            // 添加样式
            label.setOnMouseEntered(e -> label.setStyle("-fx-border-color: #3498db; -fx-border-width: 1;"));
            label.setOnMouseExited(e -> label.setStyle("-fx-border-color: transparent;"));
        }
    }

    @FXML
    private void exportResults() {
        softwareVersion = AdbUtils.executeAdbCommand("shell getprop ro.build.display.id").trim();


        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出检查结果");
        // 设置初始目录
        if (lastExportDirectory != null && lastExportDirectory.exists()) {
            fileChooser.setInitialDirectory(lastExportDirectory);
        } else {
            // 默认设置为用户主目录
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }

        fileChooser.setInitialFileName(String.format("%s_%s.xlsx",
                softwareVersion,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))));

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {

            // 记住目录
            lastExportDirectory = file.getParentFile();
            preferences.setProperty("lastExportDirectory", lastExportDirectory.getAbsolutePath());

            // 保存首选项
            try (OutputStream os = new FileOutputStream(PREF_FILE)) {
                preferences.store(os, "ADB Check Tool Preferences");
            } catch (IOException e) {
                System.err.println("保存首选项失败: " + e.getMessage());
            }

            // 创建进度指示器
            ProgressIndicator progressIndicator = new ProgressIndicator();
            // 不确定进度
            progressIndicator.setProgress(-1);

            Dialog<Void> progressDialog = new Dialog<>();
            progressDialog.getDialogPane().setContent(progressIndicator);
            progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            progressDialog.setTitle("导出结果");
            progressDialog.setHeaderText("正在导出结果，请稍候...");
            progressDialog.initModality(Modality.APPLICATION_MODAL);

            // 在新线程中执行导出
            new Thread(() -> {
                try {
                    exportToExcel(file);
                } finally {
                    // 确保在JavaFX线程中关闭对话框
                    Platform.runLater(progressDialog::close);
                }
            }).start();

            progressDialog.show();
        }
    }

    private void exportToExcel(File file) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("检查结果");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "分类", "检查项", "版本", "状态", "前提", "期待值", "结果值", "ADB命令 ", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 填充数据
            int rowNum = 1;
            for (CheckResult result : results) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(result.getIndex());
                row.createCell(1).setCellValue(result.getCategory());
                row.createCell(2).setCellValue(result.getItemName());
                row.createCell(3).setCellValue(result.getVersion());
                row.createCell(4).setCellValue(result.getStatus());
                row.createCell(5).setCellValue(result.getPrerequisite());
                row.createCell(6).setCellValue(result.getExpectedValue());
                row.createCell(7).setCellValue(result.getResultValue());
                row.createCell(8).setCellValue(result.getPropertyValue());
                row.createCell(9).setCellValue(result.getNote());
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);

                // 在JavaFX线程中显示成功消息
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("导出成功");
                    alert.setHeaderText(null);
                    alert.setContentText("结果已成功导出到: " + file.getAbsolutePath());
                    alert.showAndWait();
                });
            }
        } catch (IOException e) {
            // 在JavaFX线程中显示错误消息
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("导出失败");
                alert.setHeaderText(null);
                alert.setContentText("导出过程中发生错误: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (statusChecker != null) {
            statusChecker.shutdownNow();
        }
    }
}