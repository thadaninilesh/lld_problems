import java.util.Stack;

/**
 * Simplified Text Editor Buffer with Undo/Redo
 * Context
 * You are tasked with building the core functionality of a simplified text editor that supports
 * 4 operations:
 * - insert a character at a position
 * - delete a character at a position
 * - undo
 * - redo
 * Deliverables
 * - Implement the TextBuffer class.
 * - Implement a suite of unit tests that thoroughly test TextBuffer.
 * Evaluation Criteria
 * - Correctness: Does your code correctly implement all the required operations, including undo/redo?
 * - Clarity and Readability: Is your code well-structured, add easy to understand?
 * - Efficiency: Have you chosen appropriate data structures for the task?
 * - Testing: Are your tests comprehensive?
 * - Error Handling: Does your code handle invalid inputs (e.g. out-of-bounds positions) gracefully?
 * public class TextBuffer {
 * Inserts the character c at the specified O-indexed position in the text buffer.
 * Example: If the buffer contains "abc" and you call insert('x', 1), the butter should become "axbc".
 * If position is out of bounds (less than 0 or greater than the current text length), throw exception.
 * void insert(char c, int position) throws IllegalArgumentException
 // TODO: implement logic
 * Deletes the character at the specified O-indexed position.
 * Example: If the buffer contains "axbc" and you call delete (1), the buffer should become "abc".
 * If position is out of bounds, throw exception.
 * void delete(int position)
 * throws IllegalArgumentException
 * // TODO: implement logic
 * }
 * Returns the current content of the text buffer as a string.
 * String getText () { TODO: implement logic return ""; }
 * Reverts the last insert or delete operation.
 * If there are no operations to undo, this method should do nothing (no exception should be thrown).
 * Consecutive calls to undo() should undo operations in reverse order of their execution.

 * void undo () { // TODO: implement logic }
 * Re-applies the last undone operation (i.e., the operation that was most recently undone by undo()).
 * If there are no operations to redo, this method should do nothing.
 * Consecutive calls to redo () should redo operations in the order they were undone.
 * If a new insert or delete is performed after an undo(), the redo history should be cleared.
 * void redo () { //TODO: imolement logic }
 * Example of how JUnit tests could look for this assignment:
 * import org.junit.jupiter.api.Test;
 * import static org.junit.jupiter.api.Assertions.*;
 * class TextBufferTest 1
 * @Test
 * void testInsertAndGetText() {
 *      TextBuffer buffer = new TextBuffer();
 *      buffer.insert('a, 0);
 *      buffer.insert('b', 1);
 *      buffer.insert('c', 2);
 *      buffer.insert('x', 1);
 *      assertEquals ("axbc", buffer.getText());
 *  }
 */

class Operations {
    enum OperationType {
        INSERT,
        DELETE
    }

    OperationType type;
    char character;
    int position;

    public Operations(OperationType type, char character, int position) {
        this.type = type;
        this.character = character;
        this.position = position;
    }
}

public class TextEditorBuffer {


    // @TODO: faster alternative to stack from 1.6+ versions. Stack is syncrhonized, this is not
    // Deque<Operations> stack = new ArrayDeque<>();
    // contains sequence of operations done. Used for undo
    private final Stack<Operations> operationsStack = new Stack<>();

    // store while doing undo so that redo is possible. Clear history once any insert/delete is performed
    private final Stack<Operations> reverseOperationsStack = new Stack<>();

    private final StringBuilder sb = new StringBuilder();

    public void insert(char c, int position) throws IllegalArgumentException {
        if (position < 0 || position > sb.length()) {
            throw new IllegalArgumentException("Position out of bounds");
        }
        sb.insert(position, c);
        operationsStack.push(new Operations(Operations.OperationType.INSERT, c, position));
        reverseOperationsStack.clear();
    }

    public void delete(int position) throws IllegalArgumentException {
        if (position < 0 || position >= sb.length()) {
            throw new IllegalArgumentException("Position out of bounds");
        }
        char deletedChar = sb.charAt(position);
        sb.deleteCharAt(position);
        operationsStack.push(new Operations(Operations.OperationType.DELETE, deletedChar, position));
        reverseOperationsStack.clear();
    }

    public void undo() {
        if (operationsStack.isEmpty()) {
            return;
        }
        Operations lastOperation = operationsStack.pop();
        if (lastOperation.type == Operations.OperationType.INSERT) {
            // delete
            int position = lastOperation.position;
            sb.deleteCharAt(position);
            reverseOperationsStack.push(new Operations(Operations.OperationType.DELETE, lastOperation.character, position));
        } else {
            // insert
            int position = lastOperation.position;
            char character = lastOperation.character;
            sb.insert(position, character);
            reverseOperationsStack.push(new Operations(Operations.OperationType.INSERT, lastOperation.character, position));
        }
    }

    public void redo() {
        if (reverseOperationsStack.isEmpty()) {
            return;
        }
        Operations lastUndoneOperation = reverseOperationsStack.pop();
        int position = lastUndoneOperation.position;
        char character = lastUndoneOperation.character;
        if (lastUndoneOperation.type == Operations.OperationType.DELETE) {
            // insert
            operationsStack.push(new Operations(Operations.OperationType.INSERT, character, position));
            sb.insert(position, character);
        } else {
            // delete
            operationsStack.push(new Operations(Operations.OperationType.DELETE, character, position));
            sb.deleteCharAt(position);
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
        TextEditorBuffer editor = new TextEditorBuffer();

        editor.insert('H', 0);
        editor.insert('i', 1);
        assertEquals("Hi", editor.getText());

        editor.delete(1);
        assertEquals("H", editor.getText());
        System.out.println("PASS");
    }

    private static void testUndoInsert() {
        System.out.println("\nTest 2: Undo Insert");
        TextEditorBuffer editor = new TextEditorBuffer();

        editor.insert('A', 0);
        editor.undo();

        assertEquals("", editor.getText());
        System.out.println("PASS");
    }

    private static void testUndoDelete() {
        System.out.println("\nTest 3: Undo Delete");
        TextEditorBuffer editor = new TextEditorBuffer();

        editor.insert('A', 0); // Text: A
        editor.delete(0);      // Text: ""
        editor.undo();         // Should restore A

        assertEquals("A", editor.getText());
        System.out.println("PASS");
    }

    private static void testRedoLogic() {
        System.out.println("\nTest 4: Redo Logic");
        TextEditorBuffer editor = new TextEditorBuffer();

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
        TextEditorBuffer editor = new TextEditorBuffer();

        editor.insert('A', 0); // Text: A
        editor.undo();         // Text: "" (Redo stack has 'A')

        editor.insert('B', 0); // Text: B (Should clear Redo stack)
        editor.redo();         // Should do NOTHING because we branched off

        assertEquals("B", editor.getText());
        System.out.println("PASS");
    }

    private static void testOutOfBounds() {
        System.out.println("\nTest 6: Boundary Conditions");
        TextEditorBuffer editor = new TextEditorBuffer();

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
        TextEditorBuffer editor = new TextEditorBuffer();

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
        TextEditorBuffer editor = new TextEditorBuffer();

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
        TextEditorBuffer editor = new TextEditorBuffer();

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
        TextEditorBuffer editor = new TextEditorBuffer();

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