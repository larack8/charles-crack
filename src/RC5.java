//
// RC5-32/12/16 block cipher — implementation used by Charles Proxy.
// RC5-32/12/16 分组密码 —— Charles Proxy 所使用的版本。
//
// Parameters / 参数：
//   w = 32  word size in bits                  字长（bit）
//   r = 12  number of rounds                   轮数
//   b = 16  key length in bytes (implicit)     密钥字节长度（隐含）
//   c = 2   key words                          密钥拆成的 32-bit 字数
//   t = 26  expanded key table size = 2*(r+1)  扩展密钥表大小 = 2*(r+1)
//
// Source originally recovered from a .class file by IntelliJ IDEA
// (Fernflower decompiler), then cleaned up for reuse as a library.
// 本文件由 IntelliJ IDEA 的 Fernflower 反编译器从 .class 还原，
// 经过整理作为库复用。算法逻辑保持原样，仅添加注释帮助阅读。
//
// Reference / 参考：Ronald L. Rivest, "The RC5 Encryption Algorithm" (1994).
//
public class RC5 {

    // ---- RC5 公共参数 / Algorithm parameters ------------------------------
    public static final int w = 32;   // word size (bits)            字长
    public static final int r = 12;   // rounds                       轮数
    public static final int b = 8;    // bytes consumed per block     每块字节数 (2*w/8)
    public static final int c = 2;    // key words                    密钥字数
    public static final int t = 26;   // expanded key schedule size   扩展密钥数量 = 2*(r+1)

    /** Expanded key schedule S[0..25]. 扩展后的轮密钥表。 */
    public int[] S = new int[26];

    /**
     * Magic constant P_w for w=32  ——  Odd((e - 2) * 2^w).
     * 幻数 P_w（w=32 时）= Odd((e - 2) * 2^w)，即 0xB7E15163。
     * Stored here as a signed int: 0xB7E15163 == -1209970333.
     */
    public int P = -1209970333;

    /**
     * Magic constant Q_w for w=32  ——  Odd((phi - 1) * 2^w).
     * 幻数 Q_w（w=32 时）= Odd((φ - 1) * 2^w)，即 0x9E3779B9。
     * Stored here as a signed int: 0x9E3779B9 == -1640531527.
     */
    public int Q = -1640531527;

    public RC5() {
    }

    /**
     * Decrypt a byte array in 8-byte (64-bit) ECB blocks.
     * 以 8 字节（64-bit）为一块、ECB 模式解密字节数组。
     *
     * <p>输入长度必须是 8 的整数倍；每块的字节序为 big-endian，高 4 字节对应明文的
     * 第二个 32-bit 字（var8），低 4 字节对应第一个 32-bit 字（var7）—— 这与
     * {@link #RC5_EncryptArray(byte[])} 对称，双方协同使用即可。</p>
     *
     * @param input 待解密的密文字节；length 必须是 8 的倍数
     * @return      与 input 等长的明文
     */
    public byte[] RC5_DecryptArray(byte[] input) {
        byte[] output = new byte[input.length];
        int totalLen = input.length;
        int bytesInBlock = 0;       // 当前块内已累计的字节数 (0..8)
        long blockByteIdx = 0L;     // 当前块内字节下标，用于决定写高位字还是低位字
        int wordLow = 0;            // 当前块的低 32-bit 字 (A)
        int wordHigh = 0;           // 当前块的高 32-bit 字 (B)

        for (int i = 0; i < totalLen; ++i) {
            // 前 4 字节拼到 wordLow，后 4 字节拼到 wordHigh —— 都是 big-endian。
            if (blockByteIdx < 4L) {
                wordLow <<= 8;
                wordLow |= input[i] & 255;
            } else {
                wordHigh <<= 8;
                wordHigh |= input[i] & 255;
            }

            ++blockByteIdx;
            ++bytesInBlock;

            // 满 8 字节 → 解密一个块并写回
            if (bytesInBlock == 8) {
                int[] cipherBlock = new int[]{wordHigh, wordLow};
                int[] plainBlock = new int[]{0, 0};
                this.RC5_DECRYPT(cipherBlock, plainBlock);

                // big-endian 写回：plainBlock[1] 写到前 4 字节，plainBlock[0] 写到后 4 字节
                output[i - 7] = (byte) (plainBlock[1] >>> 24);
                output[i - 6] = (byte) (plainBlock[1] >>> 16);
                output[i - 5] = (byte) (plainBlock[1] >>> 8);
                output[i - 4] = (byte) plainBlock[1];
                output[i - 3] = (byte) (plainBlock[0] >>> 24);
                output[i - 2] = (byte) (plainBlock[0] >>> 16);
                output[i - 1] = (byte) (plainBlock[0] >>> 8);
                output[i]     = (byte) plainBlock[0];

                // 复位状态，迎接下一个块
                bytesInBlock = 0;
                blockByteIdx = 0L;
                wordLow = 0;
                wordHigh = 0;
            }
        }

        return output;
    }

    /**
     * Encrypt a byte array in 8-byte (64-bit) ECB blocks.
     * 以 8 字节为一块、ECB 模式加密字节数组。
     *
     * <p>与 {@link #RC5_DecryptArray(byte[])} 的打包/拆包格式完全对称。</p>
     *
     * @param input 明文字节；length 必须是 8 的倍数
     * @return      与 input 等长的密文
     */
    public byte[] RC5_EncryptArray(byte[] input) {
        byte[] output = new byte[input.length];
        int totalLen = input.length;
        int bytesInBlock = 0;
        long blockByteIdx = 0L;
        int wordLow = 0;
        int wordHigh = 0;

        for (int i = 0; i < totalLen; ++i) {
            if (blockByteIdx < 4L) {
                wordLow <<= 8;
                wordLow |= input[i] & 255;
            } else {
                wordHigh <<= 8;
                wordHigh |= input[i] & 255;
            }

            ++blockByteIdx;
            ++bytesInBlock;

            if (bytesInBlock == 8) {
                int[] plainBlock = new int[]{wordHigh, wordLow};
                int[] cipherBlock = new int[]{0, 0};
                this.RC5_ENCRYPT(plainBlock, cipherBlock);

                output[i - 7] = (byte) (cipherBlock[1] >>> 24);
                output[i - 6] = (byte) (cipherBlock[1] >>> 16);
                output[i - 5] = (byte) (cipherBlock[1] >>> 8);
                output[i - 4] = (byte) cipherBlock[1];
                output[i - 3] = (byte) (cipherBlock[0] >>> 24);
                output[i - 2] = (byte) (cipherBlock[0] >>> 16);
                output[i - 1] = (byte) (cipherBlock[0] >>> 8);
                output[i]     = (byte) cipherBlock[0];

                bytesInBlock = 0;
                blockByteIdx = 0L;
                wordLow = 0;
                wordHigh = 0;
            }
        }

        return output;
    }

    /**
     * RC5 core encryption of one 64-bit block.
     * RC5 核心加密：对单个 64-bit 块执行 12 轮运算。
     *
     * <pre>
     * A = A + S[0]
     * B = B + S[1]
     * for i = 1..r:
     *     A = ((A XOR B) <<< B) + S[2i]
     *     B = ((B XOR A) <<< A) + S[2i+1]
     * </pre>
     * 其中 "<<<" 为 32-bit 循环左移，移位量取低 5 位 (& 31)。
     *
     * @param in  { A, B } 明文两个 32-bit 字
     * @param out { A, B } 密文两个 32-bit 字（调用方提供长度 2 的数组）
     */
    void RC5_ENCRYPT(int[] in, int[] out) {
        int a = in[0] + this.S[0];
        int b = in[1] + this.S[1];

        for (int round = 1; round <= 12; ++round) {
            // A = ROL(A ^ B, B) + S[2*round]
            a = ((a ^ b) << (b & 31) | (a ^ b) >>> 32 - (b & 31)) + this.S[2 * round];
            // B = ROL(B ^ A, A) + S[2*round + 1]
            b = ((b ^ a) << (a & 31) | (b ^ a) >>> 32 - (a & 31)) + this.S[2 * round + 1];
        }

        out[0] = a;
        out[1] = b;
    }

    /**
     * RC5 core decryption of one 64-bit block — inverse of {@link #RC5_ENCRYPT}.
     * RC5 核心解密：12 轮反向运算，还原 {@link #RC5_ENCRYPT} 的结果。
     *
     * <p>注意：此实现沿用原始反编译代码的取字顺序 —— 解密时 in[1] 作为 A、
     * in[0] 作为 B，解密结果也按 out[1]=A、out[0]=B 写回。Array 打包端
     * （见 {@link #RC5_EncryptArray}/{@link #RC5_DecryptArray}）已经与之匹配。</p>
     */
    void RC5_DECRYPT(int[] in, int[] out) {
        int a = in[1];
        int b = in[0];

        for (int round = 12; round > 0; --round) {
            // A = ROR(A - S[2*round+1], B) XOR B
            a = (a - this.S[2 * round + 1] >>> (b & 31) | a - this.S[2 * round + 1] << 32 - (b & 31)) ^ b;
            // B = ROR(B - S[2*round],   A) XOR A
            b = (b - this.S[2 * round] >>> (a & 31) | b - this.S[2 * round] << 32 - (a & 31)) ^ a;
        }

        out[1] = a - this.S[1];
        out[0] = b - this.S[0];
    }

    /**
     * Key schedule — expand a 64-bit key (two 32-bit words) into S[0..25].
     * 密钥扩展 —— 将 64-bit 密钥（两个 32-bit 字）展开为 S[0..25]。
     *
     * <p>算法：<br>
     * 1) S[0] = P_w, S[i] = S[i-1] + Q_w<br>
     * 2) 以 3*max(c, 2*(r+1)) = 3*26 = 78 次混合迭代，将密钥字 L[0], L[1]
     *    与 S 表交叉异或 / 旋转。</p>
     *
     * @param keyWord0 密钥的第一个 32-bit 字 L[0]
     * @param keyWord1 密钥的第二个 32-bit 字 L[1]
     */
    void RC5_SETUP(int keyWord0, int keyWord1) {
        int[] L = new int[]{keyWord0, keyWord1};

        // Step 1 —— 用 P、Q 初始化 S 表
        this.S[0] = this.P;
        for (int i = 1; i < 26; ++i) {
            this.S[i] = this.S[i - 1] + this.Q;
        }

        // Step 2 —— 78 次混合：同时更新 S 表和 L 数组
        int iter = 0;      // 当前迭代次数（0..77）
        int lIdx = 0;      // L 数组下标，在 0/1 间轮转
        int sIdx = 0;      // S 表下标，在 0..25 间轮转
        int aAccum = 0;    // 上一轮得到的 S[sIdx]（对应 RC5 论文里的 A）
        int bAccum = 0;    // 上一轮得到的 L[lIdx]（对应论文里的 B）

        for (int tmp = bAccum; iter < 78; lIdx = (lIdx + 1) % 2) {
            // S[sIdx] = ROL(S[sIdx] + A + B, 3)
            tmp = this.S[sIdx] = this.S[sIdx] + tmp + bAccum << 3
                    | this.S[sIdx] + tmp + bAccum >>> 29;
            aAccum = tmp;

            // L[lIdx] = ROL(L[lIdx] + A + B, A + B)
            bAccum = L[lIdx] = L[lIdx] + aAccum + bAccum << (aAccum + bAccum & 31)
                    | L[lIdx] + aAccum + bAccum >>> 32 - (aAccum + bAccum & 31);

            ++iter;
            sIdx = (sIdx + 1) % 26;
        }
    }

    /**
     * Pretty-print an int as {@code 0x...} hex. 工具方法：把 int 格式化为 "0x..." 十六进制串。
     */
    public static String hex(int value) {
        return String.format("0x%s", Integer.toHexString(value)).replace(' ', '0');
    }
}
