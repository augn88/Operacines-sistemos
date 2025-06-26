package util;

public final class RegisterUtils {
    public static int parseRegisterToInt(String register) {
        return Integer.parseInt(register, 16);
    }

    private static boolean parseRegisterBooleanValue(String register) {
        return Boolean.parseBoolean(register);
    }

    public static String toHexWithPadding(int value, int amountOfChars) {
        return String.format("%0" + amountOfChars +  "x", value).toUpperCase();
    }

    private RegisterUtils() {}
}
