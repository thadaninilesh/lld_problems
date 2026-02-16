public class IPFilter {

    private static final int WILDCARD_INDEX = 256;

    private final TrieNode root = new TrieNode();

    private static class TrieNode {
        TrieNode[] children = new TrieNode[257]; // 0-255 for octets, 256 for wildcard
        boolean isEndOfPattern = false;
    }

    public void addPattern(String ipPattern) {
        String[] parts = fastSplit(ipPattern);
        TrieNode current = root;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int index;

            if (part.equals("*")) {
                index = WILDCARD_INDEX;
            } else {
                index = Integer.parseInt(part);
            }
            if(current.children[index] == null) {
                current.children[index] = new TrieNode();
            }
            current = current.children[index];
        }
        current.isEndOfPattern = true;
    }

    public boolean matches(String ipAddress) {
        String[] parts = fastSplit(ipAddress);
        int[] ipParts = new int[4];
        for (int i = 0; i < parts.length; i++) {
            ipParts[i] = Integer.parseInt(parts[i]);
        }
        return search(root, ipParts, 0);
    }

    private boolean search(TrieNode node, int[] parts, int level) {
        if (level == 4) {
            return node.isEndOfPattern;
        }

        int currentOctet = parts[level];
        if (node.children[currentOctet] != null) {
            if (search(node.children[currentOctet], parts, level + 1)) {
                return true;
            }
        }
        if (node.children[WILDCARD_INDEX] != null) {
            if (search(node.children[WILDCARD_INDEX], parts, level + 1)) {
                return true;
            }
        }

        return false;
    }

    private String[] fastSplit(String ipPattern) {
        String[] parts = new String[4];

        int len = ipPattern.length();
        int start = 0;
        int count = 0;

        for (int i = 0; i < len; i++) {
            if (ipPattern.charAt(i) == '.') {
                parts[count++] = ipPattern.substring(start, i);
                start = i + 1;
            }
        }
        parts[count] = ipPattern.substring(start);

        return parts;
    }

    public static void main(String[] args) {
        IPFilter filter = new IPFilter();

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
