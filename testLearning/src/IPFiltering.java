public class IPFiltering {

    private final int WILDCARD = 256;

    TrieNode root = new TrieNode();

    private static class TrieNode {
        TrieNode[] children = new TrieNode[257];
        boolean isEndOfPattern = false;
    }

    public void addPattern(String ipAddress) {
        String[] pattern = ipAddress.split("\\.");//split(ipAddress);
        TrieNode current = root;

        for (String p: pattern) {
            int idx;
            if (p.equals("*")) {
                idx = WILDCARD;
            } else {
                idx = Integer.parseInt(p);
            }

            if (current.children[idx] == null) {
                current.children[idx] = new TrieNode();
            }
            current = current.children[idx];
        }
        current.isEndOfPattern = true;
    }

    public boolean matches(String ipAddress) {
        String[] pattern = split(ipAddress);
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

        int current = parts[level];
        if (node.children[current] != null) {
            if(search(node.children[current], parts, level + 1)) {
                return true;
            }
        }

        if (node.children[WILDCARD] != null) {
            return true;
        }
        return false;
    }

    private String[] split(String pattern) {
        String[] splitPattern = new String[4];
        int start = 0;
        int idx = 0;
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '.') {
                // add to splitPattern
                splitPattern[idx++] = pattern.substring(start, i);
                start = i + 1;
            }
        }
        splitPattern[idx] = pattern.substring(start);
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