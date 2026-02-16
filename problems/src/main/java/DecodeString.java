import java.util.Stack;

public class DecodeString {
    public String decodeString(String s) {
        Stack<Integer> countStack = new Stack<>();
        Stack<StringBuilder> stringStack =  new Stack<>();
        StringBuilder currentString = new StringBuilder();
        int k = 0;

        for (char c: s.toCharArray()) {
            if (Character.isDigit(c)) {
                k = k * 10 + (c - '0');
            } else if (c == '[') {
                // push the number
                countStack.push(k);
                stringStack.push(currentString);

                currentString = new StringBuilder();
                k = 0;
            } else if (c == ']') {
                StringBuilder decodedString = currentString;

                currentString = stringStack.pop();
                int currentK = countStack.pop();


                for (int i = 0; i < currentK; i++) {
                    currentString.append(decodedString);
                }
            } else {
                currentString.append(c);
            }
        }

        return currentString.toString();
    }

    public static void main(String[] args) {
        String s = "3[a2[c]]";
        System.out.println(new DecodeString().decodeString(s));
    }
}
