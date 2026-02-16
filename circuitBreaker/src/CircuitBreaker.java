import java.util.Deque;
import java.util.LinkedList;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
  * Overview
  * Implement a Circuit Breaker pattern to prevent cascading failures when calling external services.
  * The circuit breaker counts failures and stops calling a tailing service after a threshold is reached.
  * ## Requirements
  * Implement a circuit breaker with two states:
  * - Passing (default): Function calls pass through normally
  * - Blocking: Function calls are blocked, returning immediate errors
  * ## Functionality:
  * - Accept a function to protect and a failure threshold
  * - Track consecutive failures
  * - When passing: Execute the function and return its result
  * - When failures reach threshold: Switch to blocking state
  * - When blocking: Return errors without calling the function
  * ## Basic Usage Example:
  * val circuitBreaker =
  * CircuitBreaker(callExternalServ
  * ice, failureThreshold = 3)
  * val result =
  * circuitBreaker.execute(paramete rs)
*/
 public class CircuitBreaker {

    enum State { CLOSED, OPEN, HALF_OPEN }

     private final int failureThreshold;
     private final Supplier<String> protectedFunction;
     private final LongSupplier clock;

     private State state = State.CLOSED;

     private int consecutiveFailures = 0;
     private int openAttempts = 0;
     private long nextRetryTime = 0;

     private final Deque<Long> failureTimestamps = new LinkedList<>();

    public CircuitBreaker(int failureThreshold, Supplier<String> protectedFunction, LongSupplier clock) {
        this.failureThreshold = failureThreshold;
        this.protectedFunction = protectedFunction;
        this.clock = clock;
    }

    private State getState() {
        return state;
    }

    public String execute() {
        // 1. Check if we should try to close
         if (state == State.OPEN) {

            if (clock.getAsLong() >= nextRetryTime) {
                state = State.HALF_OPEN;
            } else {
                throw new RuntimeException("Circuit is OPEN. Calls are blocked.");
            }
        }

        // 2. Execute the protected function
        try {
            String result = protectedFunction.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw new RuntimeException(e);
        }
    }

    private void onSuccess() {
        state = State.CLOSED;
        consecutiveFailures = 0;
        openAttempts = 0; // Reset backoffs
        failureTimestamps.clear();
    }

    public void onFailure() {
        long now = clock.getAsLong();
        consecutiveFailures++;
        failureTimestamps.addLast(now);

        // clean old errors (>30s)
        while (!failureTimestamps.isEmpty() && now - failureTimestamps.peekFirst() > 30000) {
            failureTimestamps.poll();
        }

        // check if we should trip the circuit
        if (consecutiveFailures >= failureThreshold) {
            tripCircuit();
        }
    }

    private void tripCircuit() {
        state = State.OPEN;
        openAttempts++;

        // Backoff: Exponential backoff with cap at 1 second
        long backOff = 1000 * (long) Math.pow(2, openAttempts - 1);
        nextRetryTime = clock.getAsLong() + backOff;
    }

    public static void main(String[] args) {
        long[] currentTime = {0};

        LongSupplier mockClock = () -> currentTime[0];

        boolean[] shouldFail = {true};
        Supplier<String> service = () -> {
            if (shouldFail[0]) {
                throw new RuntimeException("Service failure");
            }
            return "Success";
        };

        CircuitBreaker cb = new CircuitBreaker(3, service, mockClock);

        // 1. Try Opening (Consecutive)
        try {
            cb.execute();
        } catch (Exception e) {
            System.out.println("State "  + cb.getState());
        }
        try { cb.execute(); } catch (Exception e) {
            System.out.println("State " + cb.getState());
        }
        try { cb.execute(); } catch (Exception e) {
            System.out.println("State " + cb.getState());
        }

        try {
            cb.execute();
            System.out.println("Fail: Should be open " + cb.getState());
        } catch (Exception e) {
            System.out.println("Pass: Circuit opened correctly " + cb.getState()); // Circuit is OPEN. Calls are blocked.
        }

        // 2. When shoudFail is faile
        currentTime[0] += 1001; // Advance time past backoff
        shouldFail[0] = false; // Service recovers

        System.out.println("Pass: Recovery result -> " + cb.execute() + " " + cb.getState()); // Should succeed

        CircuitBreaker cbWindow = new CircuitBreaker(3, service, mockClock);

        try { cbWindow.execute(); } catch (Exception e) {
            System.out.println("State " + cbWindow.getState());
        }
        currentTime[0] += 10000; // +10s
        try { cbWindow.execute(); } catch (Exception e) {
            System.out.println("State " + cbWindow.getState());
        }
        currentTime[0] += 10000; // +10s
        try { cbWindow.execute(); } catch (Exception e) {
            System.out.println("State " + cbWindow.getState());
        }

        try {
            cbWindow.execute();
            System.out.println("Pass: Should be closed as no failures are execute function due to time-window failures "  + " " + cbWindow.getState());
        } catch (Exception e) {
            System.out.println("Fail: Circuit opened due to time-window failures " + cbWindow.getState()); // Circuit is OPEN. Calls are blocked.
        }

        // 3. Test Re-closing & Backoff strategy
        // Backoff for 1st trip is 1000ms
        currentTime[0] += 1001; // Advance time past backoff
        shouldFail[0] = false; // Service recovers

        System.out.println("Pass: Recovery result -> " + cb.execute() + " " + cb.getState()); // Should succeed

        CircuitBreaker cbWindow2 = new CircuitBreaker(3, service, mockClock);
        shouldFail[0] = true;
        try { cbWindow2.execute(); } catch (Exception e) {
            System.out.println("State " + cbWindow2.getState());
        }
        currentTime[0] += 10000; // +10s
        try { cbWindow2.execute(); } catch (Exception e) {
            System.out.println("State " + cbWindow2.getState());
        }
        currentTime[0] += 10000; // +10s
        try { cbWindow2.execute(); } catch (Exception e) {
            System.out.println("State " + cbWindow2.getState());
        }

        try {
            cbWindow2.execute();
            System.out.println("FAIL: Window logic failed "  + " " + cbWindow2.getState());
        } catch (Exception e) {
            System.out.println("PASS: Window logic worked. " + cbWindow2.getState()); // Circuit is OPEN. Calls are blocked.
        }


    }
}