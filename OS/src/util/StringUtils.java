package util;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {
    public static String[] splitStringEveryNCharacters(String string, int n) {
        String remainingString = string;
        List<String> result = new ArrayList<>();

        while (!remainingString.isEmpty()) {
            int charsToRead = Math.min(remainingString.length(), n);
            result.add(remainingString.substring(0, charsToRead));
            remainingString = remainingString.substring(charsToRead);
        }

        return result.toArray(new String[0]);
    }

    private StringUtils() {}
}
