/* Charles Proxy Keygen */

/**
 * usage:
 *   $ javac CharlesKeygen.java RC5.java && java CharlesKeygen "0XDURAKI"
 *
 * @see https://github.com/duraki/charles-keygen
 * @see https://twitter.com/0xduraki
 */
public class CharlesKeygen {
    public static void main(String[] args) {
        if (args.length == 1) {
            String name = args[0];
            System.out.print("* GENERATED LICENSE:\n  [" + name + ":" + calcLicenseKey(name) + "]\n");
            return;
        }

        System.out.print("Charles Proxy - License Key Generator [ALL VERSIONS]\n");
        System.out.print("Proudly presented by >> Team 0xCHAOS\n");
        System.out.print("Usage:\n");
        System.out.print("$ CharlesKeygen \"TEAM0XCHAOS\"\n\n");

        System.out.print("Error: Missing License Name ...\n");
    }

    public static String calcLicenseKey(String licenseName) {
        int licenseKeyChecksum = 1418211210;
        int licenseNameChecksum = licenseNameChecksum(licenseName);

        licenseKeyChecksum ^= licenseNameChecksum;
        long bLicenseKey = licenseKeyChecksum;
        bLicenseKey <<= 32L;
        bLicenseKey >>>= 32L;
        bLicenseKey <<= 32L;
        bLicenseKey |= 0x1CAD6BCL;

        int keyEncMin = (int) (bLicenseKey & 0xFFFFFFFFFFFFFFFFL);
        int keyEncMax = (int) (bLicenseKey >>> 32L & 0xFFFFFFFFFFFFFFFFL);

        int[] keyEnc = new int[2];
        keyEnc[0] = keyEncMin;
        keyEnc[1] = keyEncMax;

        int[] keyDec = new int[2];
        RC5 decrypter = new RC5();
        decrypter.RC5_SETUP(-334581843, -1259282228);
        decrypter.RC5_DECRYPT(keyEnc, keyDec);

        long keyDecrypted = (keyDec[1] & 0xFFFFFFFFL) << 32L;
        keyDecrypted |= keyDec[0] & 0xFFFFFFFFL;

        int xorChecksum = xorChecksum(bLicenseKey);
        return String.format("%02X", xorChecksum) + String.format("%016X", keyDecrypted);
    }

    public static int licenseNameChecksum(String licenseName) {
        byte[] bArrayName = null;

        try {
            bArrayName = licenseName.getBytes("UTF-8");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        int nameLen = bArrayName.length;
        int n = nameLen + 4;

        if (n % 8 != 0)
            n += 8 - n % 8;
        byte[] arrbChecksum = new byte[n];
        System.arraycopy(bArrayName, 0, arrbChecksum, 4, nameLen);
        arrbChecksum[0] = (byte) (nameLen >> 24);
        arrbChecksum[1] = (byte) (nameLen >> 16);
        arrbChecksum[2] = (byte) (nameLen >> 8);
        arrbChecksum[3] = (byte) nameLen;
        RC5 r = new RC5();
        r.RC5_SETUP(1763497072, 2049034577);
        byte[] outputArray = r.RC5_EncryptArray(arrbChecksum);
        int n3 = 0;
        for (byte by : outputArray) {
            n3 ^= by;
            n3 = n3 << 3 | n3 >>> 29;
        }
        return n3;
    }

    private static int xorChecksum(long l) {
        long l2 = 0L;
        for (int i = 56; i >= 0; i -= 8)
            l2 ^= l >>> i & 0xFFL;

        return Math.abs((int) (l2 & 0xFFL));
    }

    public static byte[] hexToByteArray(String hexstring) {
        int len = hexstring.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(hexstring.charAt(i), 16) << 4) + Character.digit(hexstring.charAt(i + 1), 16));

        return data;
    }
}
