/**
 * Charles Proxy license key generator — algorithm core.
 * Charles Proxy 许可证密钥生成器 —— 算法核心。
 *
 * <p>Given an arbitrary license name, derives a matching registration key that
 * Charles Proxy (all v3/v4/v5 releases to date) will accept in
 * {@code Help → Register}. 对任意注册名，推导出 Charles Proxy（v3/v4/v5 全版本）可接受的注册码，
 * 可填入 {@code Help → Register}（帮助 → 注册）对话框。</p>
 *
 * <p>High-level pipeline (对应下方代码段)：
 * <ol>
 *   <li>{@link #licenseNameChecksum(String)} —— 使用 RC5-32/12/16 对「长度前缀 + UTF-8 用户名」
 *       加密，再逐字节异或并循环左移 3 位，产出 32-bit 校验值。</li>
 *   <li>与魔数 {@code 0x54882CCA} 异或，得到 64-bit 密文的高 32 位；低 32 位固定为 {@code 0x01CAD6BC}。</li>
 *   <li>用另一组固定密钥做 RC5 解密，得到 64-bit 明文 = 真正的 license key 数字部分。</li>
 *   <li>前缀 2 位十六进制为 64-bit 密文的 XOR 字节校验，后 16 位十六进制为解密结果。</li>
 * </ol>
 *
 * <p>算法本身来自 Team larack 的逆向成果，本实现仅重新整理注释，逻辑与原版保持一致。
 * 仅供学习研究使用，请在试用期内购买正版。</p>
 */
public class CharlesKeygen {

    /**
     * CLI 入口：保留原始行为（传单个用户名 → 打印注册码）。
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            String name = args[0];
            System.out.print("* GENERATED LICENSE:\n  [" + name + ":" + calcLicenseKey(name) + "]\n");
            return;
        }

        System.out.print("Charles Proxy - License Key Generator [ALL VERSIONS]\n");
        System.out.print("Proudly presented by >> Team larack\n");
        System.out.print("Usage:\n");
        System.out.print("$ CharlesKeygen \"TEAM larack\"\n\n");

        System.out.print("Error: Missing License Name ...\n");
    }

    /**
     * Compute the registration key for {@code licenseName}.
     * 根据 {@code licenseName} 计算对应的注册码。
     *
     * <p>返回格式：{@code XX} + 16 位十六进制，共 18 位大写字符。前 2 位为校验字节、
     * 后 16 位为 64-bit RC5 解密结果。</p>
     */
    public static String calcLicenseKey(String licenseName) {
        // 魔数 0x54882CCA —— 来自 Charles 内部硬编码常量；与 name checksum 异或后得到
        // 真正 license 数字的高 32-bit。
        int licenseKeyChecksum = 1418211210;           // 0x54882CCA
        int licenseNameChecksum = licenseNameChecksum(licenseName);

        licenseKeyChecksum ^= licenseNameChecksum;

        // 拼装 64-bit 密文：高 32 位 = licenseKeyChecksum（无符号扩展），低 32 位固定 0x01CAD6BC。
        long bLicenseKey = licenseKeyChecksum;
        bLicenseKey <<= 32L;          // 先无符号化
        bLicenseKey >>>= 32L;
        bLicenseKey <<= 32L;          // 再放到高 32 位
        bLicenseKey |= 0x1CAD6BCL;    // 低 32 位魔数

        // 拆成两个 32-bit 字供 RC5_DECRYPT 使用
        int keyEncMin = (int) (bLicenseKey & 0xFFFFFFFFFFFFFFFFL);
        int keyEncMax = (int) (bLicenseKey >>> 32L & 0xFFFFFFFFFFFFFFFFL);

        int[] keyEnc = new int[2];
        keyEnc[0] = keyEncMin;   // 低 32 位
        keyEnc[1] = keyEncMax;   // 高 32 位

        // RC5 解密，密钥也是硬编码常量：-334581843 (0xEC12F54D)、-1259282228 (0xB4F9ED4C)
        int[] keyDec = new int[2];
        RC5 decrypter = new RC5();
        decrypter.RC5_SETUP(-334581843, -1259282228);
        decrypter.RC5_DECRYPT(keyEnc, keyDec);

        // 把解密后的两段 32-bit 字拼成 64-bit 明文（即 license key 的数字主体）
        long keyDecrypted = (keyDec[1] & 0xFFFFFFFFL) << 32L;
        keyDecrypted |= keyDec[0] & 0xFFFFFFFFL;

        // 校验字节 = 对 64-bit 密文逐字节 XOR 取低 8 位
        int xorChecksum = xorChecksum(bLicenseKey);
        return String.format("%02X", xorChecksum) + String.format("%016X", keyDecrypted);
    }

    /**
     * Hash a license name into a 32-bit checksum using RC5 encryption.
     * 用 RC5 加密「长度前缀 + 用户名」，再对字节做 XOR + 循环左移 3 位，折算成 32-bit 校验值。
     *
     * <p>密钥使用另一组硬编码常量：1763497072 (0x691B97F0)、2049034577 (0x7A221511)。</p>
     */
    public static int licenseNameChecksum(String licenseName) {
        byte[] bArrayName = null;

        try {
            bArrayName = licenseName.getBytes("UTF-8");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        int nameLen = bArrayName.length;

        // 8 字节对齐：前 4 字节存 nameLen（big-endian），其后是 UTF-8 字节，再按需零填充。
        int n = nameLen + 4;
        if (n % 8 != 0)
            n += 8 - n % 8;

        byte[] arrbChecksum = new byte[n];
        System.arraycopy(bArrayName, 0, arrbChecksum, 4, nameLen);
        arrbChecksum[0] = (byte) (nameLen >> 24);
        arrbChecksum[1] = (byte) (nameLen >> 16);
        arrbChecksum[2] = (byte) (nameLen >> 8);
        arrbChecksum[3] = (byte) nameLen;

        // RC5 加密（固定密钥）
        RC5 r = new RC5();
        r.RC5_SETUP(1763497072, 2049034577);
        byte[] outputArray = r.RC5_EncryptArray(arrbChecksum);

        // 折叠到 32-bit：累积器 n3，每步 XOR 当前字节再循环左移 3 位。
        int n3 = 0;
        for (byte by : outputArray) {
            n3 ^= by;
            n3 = n3 << 3 | n3 >>> 29;
        }
        return n3;
    }

    /**
     * Byte-wise XOR checksum over the 8 bytes of a 64-bit value.
     * 对 64-bit 值的 8 个字节逐个异或，返回低 8 位（0..255）。
     */
    private static int xorChecksum(long l) {
        long l2 = 0L;
        for (int i = 56; i >= 0; i -= 8)
            l2 ^= l >>> i & 0xFFL;

        return Math.abs((int) (l2 & 0xFFL));
    }

    /**
     * Hex string → byte[]. 工具方法：把形如 "0A1B2C" 的十六进制串解码为字节数组。
     */
    public static byte[] hexToByteArray(String hexstring) {
        int len = hexstring.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(hexstring.charAt(i), 16) << 4)
                    + Character.digit(hexstring.charAt(i + 1), 16));

        return data;
    }
}
