import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 * Cross-platform entry point for Charles Proxy Keygen.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>No arguments &rarr; launches a Swing GUI (macOS / Windows / Linux).</li>
 *   <li>{@code --cli &lt;name&gt;} or a single positional arg &rarr; prints the generated
 *       license key to stdout, matching the historical {@code CharlesKeygen} behaviour.</li>
 *   <li>{@code --help} / {@code -h} &rarr; prints usage.</li>
 * </ul>
 *
 * <p>Packaging:
 * <ul>
 *   <li>Built as a runnable fat JAR (see {@code build.sh} / {@code build.bat}).</li>
 *   <li>Wrapped as a native installer / app bundle via {@code jpackage} for each OS.</li>
 * </ul>
 */
public class Main {

    public static final String APP_NAME = "Charles Keygen";
    public static final String APP_VERSION = "1.0.0";

    public static void main(String[] args) {
        // CLI mode -------------------------------------------------------------
        if (args.length > 0) {
            String first = args[0];
            if ("--help".equals(first) || "-h".equals(first)) {
                printUsage();
                return;
            }
            String name;
            if ("--cli".equals(first) || "-c".equals(first)) {
                if (args.length < 2) {
                    System.err.println("Error: --cli requires a license name.");
                    printUsage();
                    System.exit(2);
                    return;
                }
                name = args[1];
            } else {
                // Legacy single-positional-arg mode (preserves historical API).
                name = first;
            }

            String key = CharlesKeygen.calcLicenseKey(name);
            System.out.println("Charles Proxy - License Key Generator [ALL VERSIONS]");
            System.out.println("* GENERATED LICENSE:");
            System.out.println("  Name: " + name);
            System.out.println("  Key:  " + key);
            return;
        }

        // GUI mode -------------------------------------------------------------
        // Cosmetics that matter before the first Swing component is created.
        applyPlatformBootstrap();

        SwingUtilities.invokeLater(Main::launchGui);
    }

    private static void printUsage() {
        System.out.println(APP_NAME + " v" + APP_VERSION);
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar charles-keygen.jar                 # launch GUI");
        System.out.println("  java -jar charles-keygen.jar <license-name>  # print key to stdout");
        System.out.println("  java -jar charles-keygen.jar --cli <name>    # explicit CLI mode");
        System.out.println("  java -jar charles-keygen.jar --help          # show this help");
    }

    // ------------------------------------------------------------------------
    // GUI
    // ------------------------------------------------------------------------

    private static void applyPlatformBootstrap() {
        // macOS — use the native menubar and set the dock/app name BEFORE any AWT call.
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);
            System.setProperty("apple.awt.application.name", APP_NAME);
            System.setProperty("apple.awt.application.appearance", "system");
        }

        // Use the system Look & Feel on every platform so the UI blends in natively.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to cross-platform LAF (Metal) — still works.
        }
    }

    private static void launchGui() {
        JFrame frame = new JFrame(APP_NAME + " v" + APP_VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        // --- Header -------------------------------------------------------
        JLabel title = new JLabel("Charles Proxy License Generator");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel(
                "<html><body style='width: 420px'>"
                + "Enter any license name. A registration key will be generated for all "
                + "historical &amp; current Charles v5 releases. "
                + "For personal / research use only."
                + "</body></html>");
        subtitle.setForeground(new Color(0x555555));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);

        // --- Input form ---------------------------------------------------
        JLabel nameLabel = new JLabel("License name:");
        JTextField nameField = new JTextField("TEAM0XCHAOS", 24);
        nameField.setFont(nameField.getFont().deriveFont(14f));

        JLabel keyLabel = new JLabel("Generated key:");
        JTextField keyField = new JTextField(24);
        keyField.setEditable(false);
        keyField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        keyField.setBackground(new Color(0xF5F5F5));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        form.add(nameLabel, c);
        c.gridx = 1; c.weightx = 1;
        form.add(nameField, c);
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        form.add(keyLabel, c);
        c.gridx = 1; c.weightx = 1;
        form.add(keyField, c);

        // --- Buttons ------------------------------------------------------
        JButton generateBtn = new JButton("Generate");
        JButton copyBtn = new JButton("Copy Key");
        JButton copyPairBtn = new JButton("Copy Name + Key");
        copyBtn.setEnabled(false);
        copyPairBtn.setEnabled(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(copyBtn);
        buttons.add(copyPairBtn);
        buttons.add(generateBtn);

        // --- Status bar ---------------------------------------------------
        JLabel status = new JLabel(" ");
        status.setForeground(new Color(0x2E7D32));
        status.setBorder(new EmptyBorder(4, 2, 0, 2));

        // --- Compose ------------------------------------------------------
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(form, BorderLayout.CENTER);
        center.add(buttons, BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);

        // --- Wiring -------------------------------------------------------
        Runnable generate = () -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                keyField.setText("");
                copyBtn.setEnabled(false);
                copyPairBtn.setEnabled(false);
                status.setForeground(new Color(0xC62828));
                status.setText("Please enter a license name.");
                return;
            }
            try {
                String key = CharlesKeygen.calcLicenseKey(name);
                keyField.setText(key);
                copyBtn.setEnabled(true);
                copyPairBtn.setEnabled(true);
                status.setForeground(new Color(0x2E7D32));
                status.setText("Key generated. Paste the Name + Key into Charles → Help → Register.");
            } catch (Throwable t) {
                keyField.setText("");
                copyBtn.setEnabled(false);
                copyPairBtn.setEnabled(false);
                status.setForeground(new Color(0xC62828));
                status.setText("Error: " + t.getMessage());
            }
        };

        generateBtn.addActionListener((ActionEvent e) -> generate.run());
        nameField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) generate.run();
            }
        });

        copyBtn.addActionListener(e -> {
            copyToClipboard(keyField.getText());
            status.setForeground(new Color(0x2E7D32));
            status.setText("License key copied to clipboard.");
        });
        copyPairBtn.addActionListener(e -> {
            String text = "Registered Name: " + nameField.getText().trim()
                    + System.lineSeparator()
                    + "License Key:     " + keyField.getText();
            copyToClipboard(text);
            status.setForeground(new Color(0x2E7D32));
            status.setText("Name + Key copied to clipboard.");
        });

        frame.getRootPane().setDefaultButton(generateBtn);
        frame.setContentPane(root);
        frame.pack();
        frame.setMinimumSize(new Dimension(560, frame.getHeight()));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Generate once on startup so users immediately see a working key.
        generate.run();
    }

    private static void copyToClipboard(String text) {
        if (text == null) return;
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }
}
