/*
 * There is a new alien language which uses the Latin alphabet. However, the order among letters are unknown to you. You receive a list of non-empty words from the dictionary, where words are sorted lexicographically by the rules of this new language. Derive the order of letters in this language.
Example 1:
Input:
[
"wrt",
"wrf",
"er",
"ett",
"rftt"
]
Output: "wertf"

Example 2:
Input:
[
"z",
"x"
]
Output: "zx"

Example 3:
Input:
[
"z",
"x",
"z"
]
Output: ""

* Using BFS / Kahn's Algorithm
* Graphy theory problem
*
* */

import java.util.*;

public class AlienLanguage {

    public static void main(String[] args) {
        String alienOrder = new AlienLanguage().alienOrder(new String[]{"wrt",
                "wrf",
                "er",
                "ett",
                "rftt"});
        System.out.println(alienOrder);
    }

    public String alienOrder(String[] words) {
        // 1. Initialize DS
        Map<Character, Set<Character>> adjacency = new HashMap<>();
        Map<Character, Integer> inDegree = new HashMap<>();

        for (String word: words) {
            for (char c: word.toCharArray()) {
                inDegree.put(c, 0);
                adjacency.putIfAbsent(c, new HashSet<>());
            }
        }

        // 2. Build the graph
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i], w2 = words[i+1];
            if (w1.length() > w2.length() && w1.startsWith((w2))) {
                return "";
            }

            for (int j = 0; j < Math.min(w1.length(), w2.length()); j++) {
                char parent = w1.charAt(j);
                char child = w2.charAt(j);
                if (parent != child) {
                    if (!adjacency.get(parent).contains(child)) {
                        adjacency.get(parent).add(child);
                        inDegree.put(child, inDegree.get(child) + 1);
                    }
                    break;
                }
            }
        }

        // 3. BFS | Kahn's Algo
        Queue<Character> queue = new LinkedList<>();
        for (char c: inDegree.keySet()) {
            if (inDegree.get(c) == 0)
                queue.add(c);
        }

        StringBuilder sb = new StringBuilder();
        while (!queue.isEmpty()) {
            Character curr = queue.poll();
            sb.append(curr);

            for (char neighbor: adjacency.get(curr)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0)
                    queue.add(neighbor);
            }
        }

        return sb.length() == inDegree.size() ? sb.toString() : "";
    }


}
