public class IPFiltering {

    private final int WILDCARD = 256;

    TrieNode root = new TrieNode();

    private static class TrieNode {
        TrieNode[] children = new TrieNode[257];
        boolean isEndOfPattern = false;
    }

    public void addPattern(String ipAddress) {
        String[] pattern = split(ipAddress);
        TrieNode current = root;
        for (String p: pattern) {
            int index;
            if (p.equals("*")) {
                // this is wildcard
                index = WILDCARD;
            } else {
                index = Integer.parseInt(p);
            }
            if (current.children[index] == null) {
                current.children[index] = new TrieNode();
            }
            current = current.children[index];
        }
        current.isEndOfPattern = true;
    }

    public boolean matches(String ipAddress) {
        String[] pattern = split(ipAddress);
        boolean match = false;
        int []parts = new int[4];
        for (int i = 0; i < 4; i++) {
            parts[i] = Integer.parseInt(pattern[i]);
        }

        return search(root, parts, 0);
    }

    private boolean search(TrieNode node, int[] parts, int level) {
        if (level == 4) {
            return node.isEndOfPattern;
        }

        int currentOctet = parts[level];
        if (node.children[currentOctet] != null) {
            if (search(node.children[currentOctet], parts, ++level)) {
                return true;
            }
        }
        if (node.children[WILDCARD] != null) {
            if (search(node.children[WILDCARD], parts, ++level)) {
                return true;
            }
        }
        return false;
    }

    private String[] split(String pattern) {
        String[] splitPattern = new String[4];
        int count = 0;
        int n = pattern.length();
        int start = 0;
        for (int i = 0; i < n; i++) {
            if (pattern.charAt(i) == '.') {
                // received a . | add to pattern
                splitPattern[count++] = pattern.substring(start, i);
                start = i + 1;
            }

            splitPattern[count] = pattern.substring(start);
        }
        return splitPattern;
    }

    public static void main(String[] args) {
        IPFiltering filter = new IPFiltering();

        filter.addPattern("82.3.0.100");
        filter.addPattern("82.3.0.*");
        filter.addPattern("10.*.*.*");

        System.out.println(filter.matches("82.3.0.100")); // true (Exact match)
        System.out.println(filter.matches("82.3.0.255")); // true (Matches 82.3.0.*)
        System.out.println(filter.matches("82.3.0.1"));   // true (Matches 82.3.0.*)
        System.out.println(filter.matches("10.0.0.5"));   // true (Matches 10.*.*.*)
        System.out.println(filter.matches("192.168.1.1"));// false (No match)
    }

}