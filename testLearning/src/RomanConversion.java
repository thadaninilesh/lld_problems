import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RomanConversion {

    private static final int[] VALUES = {
            1000, 900, 500, 400,
            100, 90, 50, 40,
            10, 9, 5, 4, 1
    };

    private static final String[] SYMBOLS = {
            "M", "CM", "D", "CD,",
            "C", "XC", "L", "XL",
            "X", "IX", "V", "IV", "I"
    };

    private final Map<Integer, String> cache = new ConcurrentHashMap<>();

    public String convert(int number) {
        if (number < 1 || number > 3999) {
            throw new IllegalArgumentException();
        }
        return cache.computeIfAbsent(number, this::computeRoman);
    }

    private String computeRoman(int number) {
        StringBuilder roman = new StringBuilder();

        for (int i = 0; i < VALUES.length; i++) {
            while (number >= VALUES[i]) {
                number -= VALUES[i];
                roman.append(SYMBOLS[i]);
            }
        }
        return roman.toString();
    }

    public static void main(String[] args) {
        RomanConversion converter = new RomanConversion();
        System.out.println(converter.convert(3));    // Output: "III"
        System.out.println(converter.convert(4));    // Output: "IV"
        System.out.println(converter.convert(9));    // Output: "IX"
        System.out.println(converter.convert(58));   // Output: "LVIII"
        System.out.println(converter.convert(1994)); // Output: "MCMXCIV"
        System.out.println(converter.convert(1894)); // Output: "MDCCCXCIV"
        System.out.println(converter.convert(1394)); // Output: "MCCCXCIV"
    }
}
