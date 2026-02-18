import java.util.ArrayDeque;
import java.util.Deque;

class Operation {
    enum OperationType {
        INSERT,
        DELETE
    }
    OperationType type;
    char character;
    int position;

    public Operation(OperationType type, char character, int position) {
        this.type = type;
        this.character = character;
        this.position = position;
    }
}

public class TextEditor {

    Deque<Operation> operationsStack = new ArrayDeque<>();
    StringBuilder sb = new StringBuilder();
    Deque<Operation> reverseOperationStack = new ArrayDeque<>();

    void delete(int position) {
        if (sb.length() < position || position < 0) {
            throw new IllegalArgumentException();
        }

        char c = sb.charAt(position);
        sb.deleteCharAt(position);
        operationsStack.add (new Operation(Operation.OperationType.DELETE, c, position));
        reverseOperationStack.clear();
    }

    void insert(char c, int position) {
        if (position > sb.length() || position < 0) {
            throw new IllegalArgumentException();
        }

        sb.insert(position, c);
        operationsStack.add(new Operation(Operation.OperationType.INSERT, c, position));
        reverseOperationStack.clear();
    }

    void undo() {
        // take it from the operation stack, add it to reverse stack
        if (operationsStack.isEmpty()) {
            return;
        }
        Operation lastOperation = operationsStack.pollLast();
        int position = lastOperation.position;
        char c = lastOperation.character;
        if (lastOperation.type == Operation.OperationType.INSERT) {
            sb.deleteCharAt(position);
            reverseOperationStack.add(new Operation(Operation.OperationType.DELETE, c, position));
        } else {
            sb.insert(position, c);
            reverseOperationStack.add(new Operation(Operation.OperationType.INSERT, c, position));
        }
    }

    void redo() {
        if (reverseOperationStack.isEmpty()) {
            return;
        }
        Operation lastReverseOperation = reverseOperationStack.pollLast();
        int position = lastReverseOperation.position;
        char c = lastReverseOperation.character;
        if (lastReverseOperation.type == Operation.OperationType.DELETE) {
            // insert
            sb.insert(position, c);
            operationsStack.add(new Operation(Operation.OperationType.INSERT, c, position));
        } else {
            sb.deleteCharAt(position);
            operationsStack.add(new Operation(Operation.OperationType.DELETE, c, position));
        }
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Text Editor Tests ---");

        testBasicInsertDelete();
        testUndoInsert();
        testUndoDelete();
        testRedoLogic();
        testRedoClearOnNewChange();
        testOutOfBounds();
        testComplexFlow();

        System.out.println("\n--- All Tests Finished ---");
    }

    public String getText() {
        return sb.toString();
    }

    private static void testBasicInsertDelete() {
        System.out.println("\nTest 1: Basic Insert & Delete");
        TextEditor editor = new TextEditor();

        editor.insert('H', 0);
        editor.insert('i', 1);
        assertEquals("Hi", editor.getText());

        editor.delete(1);
        assertEquals("H", editor.getText());
        System.out.println("PASS");
    }

    private static void testUndoInsert() {
        System.out.println("\nTest 2: Undo Insert");
        TextEditor editor = new TextEditor();

        editor.insert('A', 0);
        editor.undo();

        assertEquals("", editor.getText());
        System.out.println("PASS");
    }

    private static void testUndoDelete() {
        System.out.println("\nTest 3: Undo Delete");
        TextEditor editor = new TextEditor();

        editor.insert('A', 0); // Text: A
        editor.delete(0);      // Text: ""
        editor.undo();         // Should restore A

        assertEquals("A", editor.getText());
        System.out.println("PASS");
    }

    private static void testRedoLogic() {
        System.out.println("\nTest 4: Redo Logic");
        TextEditor editor = new TextEditor();

        editor.insert('X', 0); // Text: X
        editor.undo();         // Text: ""
        assertEquals("", editor.getText());

        editor.redo();         // Text: X
        assertEquals("X", editor.getText());

        // Test Redo of a Delete
        editor.delete(0);      // Text: ""
        editor.undo();         // Text: X
        editor.redo();         // Text: ""
        assertEquals("", editor.getText());

        System.out.println("PASS");
    }

    private static void testRedoClearOnNewChange() {
        System.out.println("\nTest 5: Clear Redo History on New Edit");
        //
        TextEditor editor = new TextEditor();

        editor.insert('A', 0); // Text: A
        editor.undo();         // Text: "" (Redo stack has 'A')

        editor.insert('B', 0); // Text: B (Should clear Redo stack)
        editor.redo();         // Should do NOTHING because we branched off

        assertEquals("B", editor.getText());
        System.out.println("PASS");
    }

    private static void testOutOfBounds() {
        System.out.println("\nTest 6: Boundary Conditions");
        TextEditor editor = new TextEditor();

        try {
            editor.insert('A', -1);
            throw new RuntimeException("Failed to catch negative index");
        } catch (IllegalArgumentException e) {}

        try {
            editor.insert('A', 50);
            throw new RuntimeException("Failed to catch index too large");
        } catch (IllegalArgumentException e) {}

        System.out.println("PASS");
    }

    private static void testComplexFlow() {
        System.out.println("\nTest 7: Complex Flow (Typing 'Hello')");
        TextEditor editor = new TextEditor();

        editor.insert('H', 0);
        editor.insert('e', 1);
        editor.insert('l', 2);
        editor.insert('l', 3);
        editor.insert('o', 4); // Hello

        editor.undo(); // Hell
        editor.undo(); // Hel

        editor.insert('p', 3); // Help

        assertEquals("Help", editor.getText());
        System.out.println("PASS");
    }

    private static void testInsertInMiddleUndoRedo() {
        System.out.println("\nTest 8: Insert in Middle -> Undo -> Redo");
        TextEditor editor = new TextEditor();

        // Setup "AC"
        editor.insert('A', 0);
        editor.insert('C', 1);
        assertEquals("AC", editor.getText());

        // Action: Insert 'B' in the middle (index 1)
        System.out.println("Action: Insert 'B' at index 1");
        editor.insert('B', 1);

        // Verify: "ABC"
        assertEquals("ABC", editor.getText());

        // Undo: Should return to "AC"
        System.out.println("Action: Undo");
        editor.undo();
        assertEquals("AC", editor.getText());

        // Redo: Should return to "ABC"
        System.out.println("Action: Redo");
        editor.redo();
        assertEquals("ABC", editor.getText());

        System.out.println("PASS");
    }

    private static void testDeleteFromMiddleUndoRedo() {
        System.out.println("\nTest 9: Delete from Middle -> Undo -> Redo");
        TextEditor editor = new TextEditor();

        // Setup "ABC"
        editor.insert('A', 0);
        editor.insert('B', 1);
        editor.insert('C', 2);
        assertEquals("ABC", editor.getText());

        // Action: Delete 'B' (index 1)
        System.out.println("Action: Delete at index 1");
        editor.delete(1);

        // Verify: "AC"
        assertEquals("AC", editor.getText());

        // Undo: Should restore 'B' at index 1 -> "ABC"
        System.out.println("Action: Undo");
        editor.undo();
        assertEquals("ABC", editor.getText());

        // Redo: Should delete 'B' again -> "AC"
        System.out.println("Action: Redo");
        editor.redo();
        assertEquals("AC", editor.getText());

        System.out.println("PASS");
    }

    private static void testMixedMiddleOperations() {
        System.out.println("\nTest 10: Mixed Middle Operations");
        TextEditor editor = new TextEditor();

        // Setup: "12345"
        editor.insert('1', 0);
        editor.insert('2', 1);
        editor.insert('3', 2);
        editor.insert('4', 3);
        editor.insert('5', 4);

        // 1. Insert 'X' between 2 and 3 (index 2) -> "12X345"
        editor.insert('X', 2);
        assertEquals("12X345", editor.getText());

        // 2. Delete '4' (which is now at index 4) -> "12X35"
        editor.delete(4);
        assertEquals("12X35", editor.getText());

        // Undo 1 (Restore '4') -> "12X345"
        editor.undo();
        assertEquals("12X345", editor.getText());

        // Undo 2 (Remove 'X') -> "12345"
        editor.undo();
        assertEquals("12345", editor.getText());

        // Redo 1 (Add 'X' back) -> "12X345"
        editor.redo();
        assertEquals("12X345", editor.getText());

        System.out.println("PASS");
    }


    // Helper for assertions
    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new RuntimeException("Assertion Failed! Expected: [" + expected + "] But got: [" + actual + "]");
        }
    }

}
