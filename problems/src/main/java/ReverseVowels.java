import java.util.*;

public class ReverseVowels {
    public String reverseVowels(String s) {
            Set<Character> vowels = new HashSet<>(Arrays.asList('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U'));
        int n = s.length();
        StringBuilder sb = new StringBuilder(s);
        int p1 = 0;
        int p2 = n - 1;

        char a = s.charAt(0);
        char b = s.charAt(n-1);

        while (p1 < p2) {
            if (vowels.contains(a) && vowels.contains(b)) {
                //both are vowels. SWAP
                sb.setCharAt(p1, b);
                sb.setCharAt(p2, a);
                p1++;
                p2--;
            } else if (vowels.contains(a)) {
                p2--;
            } else if (vowels.contains(b)) {
                p1++;
            } else {
                p2--;
                p1++;
            }
            a = sb.charAt(p1);
            b = sb.charAt(p2);
        }

        return sb.toString();

    }

}
