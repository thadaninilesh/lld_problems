import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Roman Numbers Conversion: Arabic to Roman
 * Problem Statement:
 * Implement a function that converts an integer (Arabic numeral) to its Roman numeral representation.
 * Requirements:
 * - The function should accept an integer in the range 1 to 3999 (inclusive).
 * - The output must be a valid
 * Roman numeral string.
 * - Roman numerals are based on the following symbols:
 * Symbol | Value
 * I 1
 * V 5
 * X 10
 * L 50
 * C 100
 * D 500
 * M 1000
 * Roman numerals are written by combining these symbols and using subtractive notation where appropriate (e.g., IV for 4, IX for 9).
 * The input will always be a valid integer between 1 and 3999 (inclusive).
 * Examples:
 * Input: 3
 * Output: "III"
 * Input: 4
 * Output: "IV"
 * Input: 9
 * Output: "IX"
 * Input: 58
 * Output:
 * "LVIII"
 * Input: 1994
 * Output: "MCMXCIV"
 * public class NumeralConverter {
 *     public String convert (int integer) {throw new UnsupportedOperationException ()}
 * }
 * Followup questions:
 * - If this conversion function is called very frequently in a high-traffic environment, how would you avoid recalculating the same results repeatedly?
 * - What kind of cache would you use (in-memory, distributed, etc.) and why?
 * - How would you decide the cache size and eviction policy?
 * - If you use an in-memory cache (e.g., a HashMap), how would you ensure thread safety in a multi-threaded environment?
 * - If two threads request the same conversion at the same time and the value is not yet cached, how would you prevent duplicate computation?
 * */

public class RomanConverter {

    private static final int[] VALUES = {
            1000, 900, 500, 400,
            100, 90, 50, 40,
            10, 9, 5, 4,
            1
    };

    private static final String[] SYMBOLS = {
            "M", "CM", "D", "CD",
            "C", "XC", "L", "XL",
            "X", "IX", "V", "IV",
            "I"
    };

    private final Map<Integer, String> cache = new ConcurrentHashMap<>();

    public String convert(int number) {
        if (number < 1 || number > 3999) {
            throw new IllegalArgumentException("Input must be between 1 and 3999 (inclusive).");
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
        RomanConverter converter = new RomanConverter();
        System.out.println(converter.convert(3));    // Output: "III"
        System.out.println(converter.convert(4));    // Output: "IV"
        System.out.println(converter.convert(9));    // Output: "IX"
        System.out.println(converter.convert(58));   // Output: "LVIII"
        System.out.println(converter.convert(1994)); // Output: "MCMXCIV"
        System.out.println(converter.convert(1894)); // Output: "MDCCCXCIV"
        System.out.println(converter.convert(1394)); // Output: "MCCCXCIV"
    }

}