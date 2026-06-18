import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HighAlchGui {

    private final WikiApiClient apiClient;
    private final Map<String, Long> buyLimits = new ConcurrentHashMap<>();
    private final long FOUR_HOURS_IN_MS = 4L * 60L * 60L * 1000L;

    private JFrame frame;
    private JPanel topPanel;
    private JTable table;
    private DefaultTableModel tableModel;
    private JScrollPane scrollPane;
    
    private JLabel statusLabel;
    private JLabel minVolLabel;
    private JLabel showTopLabel;
    private JButton fetchButton;
    private JToggleButton autoFetchToggle;
    private JToggleButton themeToggle; 
    private JTextField minVolumeField;
    private JComboBox<String> resultLimitDropdown;

    private int secondsSinceLastFetch = 0;
    private boolean isDarkMode = false;

    public HighAlchGui() {
        this.apiClient = new WikiApiClient();
        setupWindow();
        setupTopBar();
        setupTable();
        setupRightClickMenu();
        setupTimers();

        updateTheme();
        frame.setVisible(true);
    }

    private void setupWindow() {
        frame = new JFrame("osrs alc util");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1050, 500); 
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
    }

    private void setupTopBar() {
        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        themeToggle = new JToggleButton("Dark Mode");
        themeToggle.addActionListener(e -> {
            isDarkMode = themeToggle.isSelected();
            themeToggle.setText(isDarkMode ? "Light Mode" : "Dark Mode");
            updateTheme();
        });
        
        autoFetchToggle = new JToggleButton("Auto-Fetch: OFF");
        autoFetchToggle.addItemListener(e -> {
            autoFetchToggle.setText(autoFetchToggle.isSelected() ? "Auto-Fetch: ON" : "Auto-Fetch: OFF");
            if (autoFetchToggle.isSelected()) secondsSinceLastFetch = 0; 
            updateTheme(); 
        });
        
        minVolLabel = new JLabel(" Min Daily Volume:");
        minVolumeField = new JTextField("5000", 6);
        showTopLabel = new JLabel(" Show Top:");
        resultLimitDropdown = new JComboBox<>(new String[]{"25", "50", "100", "All"});
        resultLimitDropdown.setSelectedItem("25");
        
        fetchButton = new JButton("Fetch Prices");
        fetchButton.addActionListener(e -> startFetchingData());
        
        statusLabel = new JLabel("Ready.");

        topPanel.add(themeToggle);
        topPanel.add(new JLabel(" | ")); 
        topPanel.add(autoFetchToggle);
        topPanel.add(minVolLabel);
        topPanel.add(minVolumeField);
        topPanel.add(showTopLabel);
        topPanel.add(resultLimitDropdown);
        topPanel.add(new JLabel(" | ")); 
        topPanel.add(fetchButton);
        topPanel.add(statusLabel);
        
        frame.add(topPanel, BorderLayout.NORTH);
    }

    private void setupTable() {
        String[] columns = {"icon", "item", "buy price", "high alc", "PPI", "GE limit", "daily volume"};
        
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 0) ? ImageIcon.class : Object.class;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                int modelRow = convertRowIndexToModel(row);
                String itemName = (String) getModel().getValueAt(modelRow, 1);
                
                // Base Theme Colors
                if (!isRowSelected(row)) {
                    Color lightBg = row % 2 == 0 ? Color.WHITE : new Color(240, 245, 250);
                    Color darkBg = row % 2 == 0 ? new Color(40, 42, 45) : new Color(50, 52, 55);
                    c.setBackground(isDarkMode ? darkBg : lightBg);
                    c.setForeground(isDarkMode ? Color.LIGHT_GRAY : Color.BLACK);
                }

                // Buy Limit Cooldown Logic
                if (buyLimits.containsKey(itemName)) {
                    long timeLeftMs = buyLimits.get(itemName) - System.currentTimeMillis();
                    
                    if (timeLeftMs > 0) {
                        if (!isRowSelected(row)) {
                            c.setBackground(isDarkMode ? new Color(100, 45, 45) : new Color(255, 200, 200));
                            c.setForeground(isDarkMode ? new Color(255, 200, 200) : Color.DARK_GRAY);
                        }
                        
                        if (column == 1 && c instanceof JLabel) {
                            long hours = timeLeftMs / (3600000);
                            long minutes = (timeLeftMs % (3600000)) / 60000;
                            ((JLabel) c).setText(String.format("%s (%dh %02dm)", itemName, hours, minutes));
                        }
                    } else {
                        buyLimits.remove(itemName);
                        if (column == 1 && c instanceof JLabel) ((JLabel) c).setText(itemName); 
                    }
                } else if (column == 1 && c instanceof JLabel) {
                    ((JLabel) c).setText(itemName);
                }
                return c;
            }
        };
        
        table.setRowHeight(28); 
        table.getColumnModel().getColumn(0).setMaxWidth(34);
        table.getColumnModel().getColumn(0).setMinWidth(34);
        table.setAutoCreateRowSorter(true); 

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        
        // Disable scrollbars and retain scroll
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        frame.add(scrollPane, BorderLayout.CENTER);
    }

    private void updateTheme() {
        Color bg = isDarkMode ? new Color(30, 32, 35) : null;
        Color fg = isDarkMode ? Color.WHITE : Color.BLACK;
        Color inputBg = isDarkMode ? new Color(60, 62, 65) : Color.WHITE;

        frame.getContentPane().setBackground(bg);
        topPanel.setBackground(bg);
        statusLabel.setForeground(fg);
        minVolLabel.setForeground(fg);
        showTopLabel.setForeground(fg);

        minVolumeField.setBackground(inputBg);
        minVolumeField.setForeground(fg);
        minVolumeField.setCaretColor(fg); 
        resultLimitDropdown.setBackground(inputBg);
        resultLimitDropdown.setForeground(fg);

        table.setBackground(isDarkMode ? new Color(40, 42, 45) : Color.WHITE);
        table.setForeground(fg);
        scrollPane.getViewport().setBackground(isDarkMode ? new Color(30, 32, 35) : Color.WHITE);
        
        table.getTableHeader().setBackground(isDarkMode ? new Color(50, 52, 55) : null);
        table.getTableHeader().setForeground(fg);

        if (autoFetchToggle.isSelected()) {
            autoFetchToggle.setBackground(isDarkMode ? new Color(45, 90, 45) : new Color(200, 255, 200));
            autoFetchToggle.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        } else {
            autoFetchToggle.setBackground(isDarkMode ? inputBg : null);
            autoFetchToggle.setForeground(fg);
        }

        fetchButton.setBackground(isDarkMode ? inputBg : null);
        fetchButton.setForeground(fg);
        themeToggle.setBackground(isDarkMode ? inputBg : null);
        themeToggle.setForeground(fg);
        frame.repaint();
    }

    private void setupRightClickMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem markItem = new JMenuItem("mark bought out");
        JMenuItem clearItem = new JMenuItem("clear timer");

        markItem.addActionListener(e -> {
            for (int row : table.getSelectedRows()) {
                buyLimits.put((String) table.getValueAt(row, 1), System.currentTimeMillis() + FOUR_HOURS_IN_MS);
            }
            table.repaint(); 
        });

        clearItem.addActionListener(e -> {
            for (int row : table.getSelectedRows()) {
                buyLimits.remove((String) table.getValueAt(row, 1));
            }
            table.repaint(); 
        });

        popupMenu.add(markItem);
        popupMenu.add(clearItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger() && table.getSelectedRow() != -1) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void setupTimers() {
        new Timer(1000, e -> {
            table.repaint(); 
            if (autoFetchToggle.isSelected()) {
                secondsSinceLastFetch++;
                if (secondsSinceLastFetch >= 60) {
                    startFetchingData();
                    secondsSinceLastFetch = 0;
                }
            }
        }).start();
    }

    private ImageIcon getScaledIcon(String filePath, int width, int height) {
        File imgFile = new File(filePath);
        if (imgFile.exists()) {
            Image img = new ImageIcon(imgFile.getAbsolutePath()).getImage();
            return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        }
        return null;
    }

    private void startFetchingData() {
        if (!fetchButton.isEnabled()) return;

        int minVolume;
        try {
            minVolume = Integer.parseInt(minVolumeField.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid number for volume.");
            return;
        }

        String selectedLimitStr = (String) resultLimitDropdown.getSelectedItem();
        int maxResults = selectedLimitStr.equals("All") ? -1 : Integer.parseInt(selectedLimitStr);

        fetchButton.setEnabled(false);
        statusLabel.setIcon(null); 
        statusLabel.setText("Fetching from api");
        tableModel.setRowCount(0); 

        // network logic on background thread
        new Thread(() -> {
            try {
                List<AlchItem> items = apiClient.fetchTopAlchs(minVolume, maxResults);
                
                SwingUtilities.invokeLater(() -> {
                    for (AlchItem item : items) {
                        tableModel.addRow(new Object[]{
                            getScaledIcon("images/" + item.id + ".png", 24, 24),
                            item.name, 
                            String.format("%,d", item.buyPrice), 
                            String.format("%,d", item.alchValue), 
                            String.format("%,d", item.profit), 
                            String.format("%,d", item.limit), 
                            String.format("%,d", item.volume)
                        });
                    }
                    
                    statusLabel.setIcon(getScaledIcon("images/561.png", 24, 24));
                    statusLabel.setText(String.format(" Nature Rune: %,d gp", apiClient.getCurrentNatPrice()));
                    fetchButton.setEnabled(true);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setIcon(null);
                    statusLabel.setText("Error fetching data! Check console.");
                    fetchButton.setEnabled(true);
                });
                ex.printStackTrace();
            }
        }).start();
    }
}
