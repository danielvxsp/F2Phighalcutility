import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // "best practice" 
        SwingUtilities.invokeLater(() -> new HighAlchGui());
    }
}
