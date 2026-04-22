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
 * Charles Proxy 注册机的跨平台入口类。
 *
 * <p>Behaviour / 行为：
 * <ul>
 *   <li>No arguments &rarr; launches a Swing GUI (macOS / Windows / Linux).<br>
 *       不传参数 → 启动 Swing 图形界面（macOS / Windows / Linux 均可）。</li>
 *   <li>{@code --cli &lt;name&gt;} or a single positional arg &rarr; prints the generated
 *       license key to stdout, matching the historical {@code CharlesKeygen} behaviour.<br>
 *       {@code --cli <name>} 或仅传一个参数 → 命令行模式，向 stdout 打印生成的注册码，
 *       兼容旧版 {@code CharlesKeygen} 的调用方式。</li>
 *   <li>{@code --help} / {@code -h} &rarr; prints usage. 打印用法说明。</li>
 * </ul>
 *
 * <p>Packaging / 打包：
 * <ul>
 *   <li>Built as a runnable fat JAR (see {@code build.sh} / {@code build.bat}).<br>
 *       通过 {@code build.sh} / {@code build.bat} 构建成可执行的 fat JAR。</li>
 *   <li>Wrapped as a native installer / app bundle via {@code jpackage} for each OS.<br>
 *       再用 {@code jpackage} 针对各平台打包成原生安装包或 App 包。</li>
 * </ul>
 */
public class Main {

    /** 程序名称，用于 GUI 标题、macOS Dock 名称等。 */
    public static final String APP_NAME = "Charles Keygen";
    /** 程序版本号，与 build 脚本、jpackage --app-version 保持一致。 */
    public static final String APP_VERSION = "1.0.0";

    public static void main(String[] args) {
        // -------------------------------------------------------------------
        // CLI mode / 命令行模式
        // 只要传了至少一个参数，就走 CLI 分支；解析完后直接返回，不启动 GUI。
        // -------------------------------------------------------------------
        if (args.length > 0) {
            String first = args[0];
            // --help / -h：打印用法并退出
            if ("--help".equals(first) || "-h".equals(first)) {
                printUsage();
                return;
            }
            String name;
            if ("--cli".equals(first) || "-c".equals(first)) {
                // 显式 CLI 模式，必须再跟一个 license 用户名
                if (args.length < 2) {
                    System.err.println("Error: --cli requires a license name.");
                    printUsage();
                    System.exit(2);
                    return;
                }
                name = args[1];
            } else {
                // Legacy single-positional-arg mode (preserves historical API).
                // 兼容旧版：只传一个位置参数也当作用户名。
                name = first;
            }

            // 真正的核心调用 —— 生成并打印注册码
            String key = CharlesKeygen.calcLicenseKey(name);
            System.out.println("Charles Proxy - License Key Generator [ALL VERSIONS]");
            System.out.println("* GENERATED LICENSE:");
            System.out.println("  Name: " + name);
            System.out.println("  Key:  " + key);
            return;
        }

        // -------------------------------------------------------------------
        // GUI mode / 图形界面模式
        // 无参数时走这里。必须先做平台相关初始化（macOS 菜单栏、系统 LAF），
        // 然后把窗口构建放到 EDT（事件调度线程）里执行。
        // -------------------------------------------------------------------
        applyPlatformBootstrap();
        SwingUtilities.invokeLater(Main::launchGui);
    }

    /** 打印 CLI 用法帮助。 */
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
    // GUI 构建
    // ------------------------------------------------------------------------

    /**
     * Platform-specific setup that must run BEFORE the first AWT/Swing call.
     * 必须在创建第一个 AWT/Swing 组件之前执行的平台相关初始化。
     *
     * <p>在 macOS 上开启原生菜单栏、设置 Dock/关于窗口显示的应用名；在所有平台上
     * 切换到系统 Look &amp; Feel，让界面风格融入各自的桌面环境。</p>
     */
    private static void applyPlatformBootstrap() {
        // macOS 专属：必须在任何 AWT 调用前设置。
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");                       // 使用屏幕顶部菜单栏
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME); // 老版 JDK 的 About 名称
            System.setProperty("apple.awt.application.name", APP_NAME);                     // Dock 名称
            System.setProperty("apple.awt.application.appearance", "system");               // 跟随系统深色/浅色
        }

        // 所有平台：尽量使用系统原生外观，失败时回退到跨平台 Metal。
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to cross-platform LAF (Metal) — still works.
        }
    }

    /** 构建并显示主窗口。必须在 EDT 上调用（{@link #main} 中已用 invokeLater 包装）。 */
    private static void launchGui() {
        JFrame frame = new JFrame(APP_NAME + " v" + APP_VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 根容器：BorderLayout + 外边距，分上(header) / 中(表单+按钮) / 下(status)三段
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        // --- Header / 标题区 -------------------------------------------------
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

        // --- Input form / 表单区 --------------------------------------------
        JLabel nameLabel = new JLabel("License name:");
        JTextField nameField = new JTextField("TEAM-larack", 24);
        nameField.setFont(nameField.getFont().deriveFont(14f));

        JLabel keyLabel = new JLabel("Generated key:");
        JTextField keyField = new JTextField(24);
        keyField.setEditable(false);                                     // 只读显示生成结果
        keyField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));     // 等宽字体更易读
        keyField.setBackground(new Color(0xF5F5F5));

        // 用 GridBagLayout 做两行两列的对齐表单
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        form.add(nameLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(nameField, c);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        form.add(keyLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(keyField, c);

        // --- Buttons / 按钮区 ------------------------------------------------
        JButton generateBtn = new JButton("Generate");           // 生成注册码
        JButton copyBtn = new JButton("Copy Key");               // 仅复制 key
        JButton copyPairBtn = new JButton("Copy Name + Key");    // 复制 Name+Key 组合
        // 未生成前禁用两个复制按钮，避免空内容进入剪贴板
        copyBtn.setEnabled(false);
        copyPairBtn.setEnabled(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(copyBtn);
        buttons.add(copyPairBtn);
        buttons.add(generateBtn);

        // --- Status bar / 状态栏 --------------------------------------------
        JLabel status = new JLabel(" ");
        status.setForeground(new Color(0x2E7D32));              // 默认成功绿
        status.setBorder(new EmptyBorder(4, 2, 0, 2));

        // --- Compose / 组装 -------------------------------------------------
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(form, BorderLayout.CENTER);
        center.add(buttons, BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);

        // --- Wiring / 事件绑定 ----------------------------------------------
        // 生成按钮的业务逻辑，封装成 Runnable 方便在按钮点击 / 回车键两处复用。
        Runnable generate = () -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                // 空名字 → 提示错误，并禁用复制按钮
                keyField.setText("");
                copyBtn.setEnabled(false);
                copyPairBtn.setEnabled(false);
                status.setForeground(new Color(0xC62828));
                status.setText("Please enter a license name.");
                return;
            }
            try {
                // 调用核心算法生成 key
                String key = CharlesKeygen.calcLicenseKey(name);
                keyField.setText(key);
                copyBtn.setEnabled(true);
                copyPairBtn.setEnabled(true);
                status.setForeground(new Color(0x2E7D32));
                status.setText("Key generated. Paste the Name + Key into Charles → Help → Register.");
            } catch (Throwable t) {
                // 防御式处理：即使底层抛异常也不让 UI 崩溃
                keyField.setText("");
                copyBtn.setEnabled(false);
                copyPairBtn.setEnabled(false);
                status.setForeground(new Color(0xC62828));
                status.setText("Error: " + t.getMessage());
            }
        };

        // 点击 Generate 或在 name 输入框内按回车，都会触发 generate
        generateBtn.addActionListener((ActionEvent e) -> generate.run());
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) generate.run();
            }
        });

        // Copy Key：仅复制注册码字符串到剪贴板
        copyBtn.addActionListener(e -> {
            copyToClipboard(keyField.getText());
            status.setForeground(new Color(0x2E7D32));
            status.setText("License key copied to clipboard.");
        });
        // Copy Name + Key：复制可直接粘贴进 Charles 注册对话框的多行文本
        copyPairBtn.addActionListener(e -> {
            String text = "Registered Name: " + nameField.getText().trim()
                    + System.lineSeparator()
                    + "License Key:     " + keyField.getText();
            copyToClipboard(text);
            status.setForeground(new Color(0x2E7D32));
            status.setText("Name + Key copied to clipboard.");
        });

        // 把 Generate 设为默认按钮（在任何输入框按回车都会触发，部分 LAF 上有更明显高亮）
        frame.getRootPane().setDefaultButton(generateBtn);
        frame.setContentPane(root);
        frame.pack();
        // 保证宽度至少 560px，避免在 Windows 上按钮被挤压
        frame.setMinimumSize(new Dimension(560, frame.getHeight()));
        frame.setLocationRelativeTo(null);        // 居中显示
        frame.setVisible(true);

        // Generate once on startup so users immediately see a working key.
        // 启动后立即生成一次，让用户一打开窗口就能看到结果。
        generate.run();
    }

    /** 把字符串写入系统剪贴板。null 安全：传 null 直接忽略。 */
    private static void copyToClipboard(String text) {
        if (text == null) return;
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }
}
