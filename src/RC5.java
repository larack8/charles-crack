//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler) — cleaned up for library use.
//

public class RC5 {
    public static final int w = 32;
    public static final int r = 12;
    public static final int b = 8;
    public static final int c = 2;
    public static final int t = 26;
    public int[] S = new int[26];
    public int P = -1209970333;
    public int Q = -1640531527;

    public RC5() {
    }

    public byte[] RC5_DecryptArray(byte[] var1) {
        byte[] var2 = new byte[var1.length];
        int var3 = var1.length;
        int var4 = 0;
        long var5 = 0L;
        int var7 = 0;
        int var8 = 0;

        for (int var9 = 0; var9 < var3; ++var9) {
            if (var5 < 4L) {
                var7 <<= 8;
                var7 |= var1[var9] & 255;
            } else {
                var8 <<= 8;
                var8 |= var1[var9] & 255;
            }

            ++var5;
            ++var4;
            if (var4 == 8) {
                int[] var10 = new int[]{var8, var7};
                int[] var11 = new int[]{0, 0};
                this.RC5_DECRYPT(var10, var11);
                var2[var9 - 7] = (byte) (var11[1] >>> 24);
                var2[var9 - 6] = (byte) (var11[1] >>> 16);
                var2[var9 - 5] = (byte) (var11[1] >>> 8);
                var2[var9 - 4] = (byte) var11[1];
                var2[var9 - 3] = (byte) (var11[0] >>> 24);
                var2[var9 - 2] = (byte) (var11[0] >>> 16);
                var2[var9 - 1] = (byte) (var11[0] >>> 8);
                var2[var9] = (byte) var11[0];
                var4 = 0;
                var5 = 0L;
                var7 = 0;
                var8 = 0;
            }
        }

        return var2;
    }

    public byte[] RC5_EncryptArray(byte[] var1) {
        byte[] var2 = new byte[var1.length];
        int var3 = var1.length;
        int var4 = 0;
        long var5 = 0L;
        int var7 = 0;
        int var8 = 0;

        for (int var9 = 0; var9 < var3; ++var9) {
            if (var5 < 4L) {
                var7 <<= 8;
                var7 |= var1[var9] & 255;
            } else {
                var8 <<= 8;
                var8 |= var1[var9] & 255;
            }

            ++var5;
            ++var4;
            if (var4 == 8) {
                int[] var10 = new int[]{var8, var7};
                int[] var11 = new int[]{0, 0};
                this.RC5_ENCRYPT(var10, var11);
                var2[var9 - 7] = (byte) (var11[1] >>> 24);
                var2[var9 - 6] = (byte) (var11[1] >>> 16);
                var2[var9 - 5] = (byte) (var11[1] >>> 8);
                var2[var9 - 4] = (byte) var11[1];
                var2[var9 - 3] = (byte) (var11[0] >>> 24);
                var2[var9 - 2] = (byte) (var11[0] >>> 16);
                var2[var9 - 1] = (byte) (var11[0] >>> 8);
                var2[var9] = (byte) var11[0];
                var4 = 0;
                var5 = 0L;
                var7 = 0;
                var8 = 0;
            }
        }

        return var2;
    }

    void RC5_ENCRYPT(int[] var1, int[] var2) {
        int var3 = var1[0] + this.S[0];
        int var4 = var1[1] + this.S[1];

        for (int var5 = 1; var5 <= 12; ++var5) {
            var3 = ((var3 ^ var4) << (var4 & 31) | (var3 ^ var4) >>> 32 - (var4 & 31)) + this.S[2 * var5];
            var4 = ((var4 ^ var3) << (var3 & 31) | (var4 ^ var3) >>> 32 - (var3 & 31)) + this.S[2 * var5 + 1];
        }

        var2[0] = var3;
        var2[1] = var4;
    }

    void RC5_DECRYPT(int[] var1, int[] var2) {
        int var3 = var1[1];
        int var4 = var1[0];

        for (int var5 = 12; var5 > 0; --var5) {
            var3 = (var3 - this.S[2 * var5 + 1] >>> (var4 & 31) | var3 - this.S[2 * var5 + 1] << 32 - (var4 & 31)) ^ var4;
            var4 = (var4 - this.S[2 * var5] >>> (var3 & 31) | var4 - this.S[2 * var5] << 32 - (var3 & 31)) ^ var3;
        }

        var2[1] = var3 - this.S[1];
        var2[0] = var4 - this.S[0];
    }

    void RC5_SETUP(int var1, int var2) {
        int[] var4 = new int[]{var1, var2};
        this.S[0] = this.P;

        for (int var5 = 1; var5 < 26; ++var5) {
            this.S[var5] = this.S[var5 - 1] + this.Q;
        }

        int var7 = 0;
        int var6 = 0;
        int var10 = 0;
        int var8 = 0;

        for (int var9 = var8; var7 < 78; var6 = (var6 + 1) % 2) {
            var9 = this.S[var10] = this.S[var10] + var9 + var8 << 3 | this.S[var10] + var9 + var8 >>> 29;
            var8 = var4[var6] = var4[var6] + var9 + var8 << (var9 + var8 & 31) | var4[var6] + var9 + var8 >>> 32 - (var9 + var8 & 31);
            ++var7;
            var10 = (var10 + 1) % 26;
        }
    }

    public static String hex(int var0) {
        return String.format("0x%s", Integer.toHexString(var0)).replace(' ', '0');
    }
}
