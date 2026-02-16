import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implement a rate limitter
 * Requirements:
 * Implement a Rate Limiter to control the number of requests allowed within a configurable time window.
 * If the number of requests exceeds the limit, the Rate Limiter should block further requests until the window resets.
 * public class RateLimiter 1
 * boolean allow(String url) {
 *  return result //depending on whether request is allowed or not return true;
 * }
 * Additional questions:
 * How to make distributed rate limiter
 * How make it operate on user level, ip level
 * How will you implement burst handling
 */

public class RateLimiter {

    private final long timeWindowMillis;
    private final int maxRequests;

    private final Map<String, Deque<Long>> userRequestHistory = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long timeWindowMillis) {
        this.maxRequests = maxRequests;
        this.timeWindowMillis = timeWindowMillis;
    }

    public synchronized boolean allow(String url) {
        long currentTime = System.currentTimeMillis();
        userRequestHistory.putIfAbsent(url, new LinkedList<>());
        Deque<Long> history = userRequestHistory.get(url);

        while (!history.isEmpty() && (currentTime - history.peekFirst() > timeWindowMillis)) {
            history.pollFirst();
        }

        if (history.size() < maxRequests) {
            history.addLast(currentTime);
            return true;
        } else {
            return false; // rate limit exceeded
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Starting Rate Limiter Tests ---");

        testHappyPath();
        testRateLimitExceeded();
        testRefillAfterWindow();
        testGlobalVsPerUrlIsolation();

        testBasicLimiting();
        testUserIsolation();
        testTimeWindowReset();

        System.out.println("\n--- All Tests Completed ---");
    }

    /**
     * Test 1: Happy Path
     * Ensure we can make requests up to the limit.
     */
    private static void testHappyPath() {
        System.out.println("\n[Test 1] Happy Path (Under Limit)");
        // Allow 3 requests per 1000ms
        RateLimiter limiter = new RateLimiter(3, 1000);

        boolean req1 = limiter.allow("user1");
        boolean req2 = limiter.allow("user1");
        boolean req3 = limiter.allow("user1");

        if (req1 && req2 && req3) {
            System.out.println("PASS: Allowed 3 consecutive requests.");
        } else {
            System.err.println("FAIL: Should have allowed 3 requests. Results: " + req1 + ", " + req2 + ", " + req3);
        }
    }

    /**
     * Test 2: Rate Limit Exceeded
     * Ensure the 4th request is blocked when limit is 3.
     */
    private static void testRateLimitExceeded() {
        System.out.println("\n[Test 2] Rate Limit Exceeded");
        // Allow 2 requests per 500ms
        RateLimiter limiter = new RateLimiter(2, 500);

        limiter.allow("user1"); // 1st (OK)
        limiter.allow("user1"); // 2nd (OK)

        boolean blockedRequest = limiter.allow("user1"); // 3rd (Should Fail)

        if (!blockedRequest) {
            System.out.println("PASS: Successfully blocked the 3rd request.");
        } else {
            System.err.println("FAIL: Did not block the request exceeding the limit.");
        }
    }

    /**
     * Test 3: Refill After Window
     * Ensure requests are allowed again after the time window passes.
     */
    private static void testRefillAfterWindow() throws InterruptedException {
        System.out.println("\n[Test 3] Refill After Window");
        int windowMs = 200;
        RateLimiter limiter = new RateLimiter(1, windowMs);

        limiter.allow("user1"); // Consumes the 1 token
        boolean immediateTry = limiter.allow("user1"); // Should fail

        if (immediateTry) {
            System.err.println("FAIL: Immediate retry should have been blocked.");
            return;
        }

        System.out.println("LOG: Waiting " + (windowMs + 50) + "ms for window to expire...");
        Thread.sleep(windowMs + 50); // Wait for window to reset

        boolean delayedTry = limiter.allow("user1"); // Should pass now

        if (delayedTry) {
            System.out.println("PASS: Request allowed after time window expired.");
        } else {
            System.err.println("FAIL: Request still blocked after time window expired.");
        }
    }

    /**
     * Test 4: Isolation Check
     * Verify if limiting "user1" accidentally limits "user2".
     */
    private static void testGlobalVsPerUrlIsolation() {
        System.out.println("\n[Test 4] User/URL Isolation");
        RateLimiter limiter = new RateLimiter(1, 1000);

        limiter.allow("user1"); // Max out user1
        boolean user2Result = limiter.allow("user2"); // user2 should be fresh

        if (user2Result) {
            System.out.println("PASS: user2 was not blocked by user1's activity.");
        } else {
            System.err.println("FAIL: Rate Limiter is Global! user2 was blocked because user1 consumed the limit.");
        }
    }

    private static void testBasicLimiting() {
        System.out.println("\n[Test 5] Basic Limiting (Max 2 requests)");
        // Allow 2 requests per 1 second
        RateLimiter limiter = new RateLimiter(2, 1000);

        System.out.println("Req 1: " + (limiter.allow("user_A") ? "PASS" : "FAIL")); // Should Pass
        System.out.println("Req 2: " + (limiter.allow("user_A") ? "PASS" : "FAIL")); // Should Pass

        // This 3rd one should be blocked immediately
        boolean result = limiter.allow("user_A");
        if (!result) {
            System.out.println("Req 3: BLOCKED (Correct)");
        } else {
            System.err.println("Req 3: ALLOWED (Error: Should have been blocked)");
        }
    }

    private static void testUserIsolation() {
        System.out.println("\n[Test6] User Isolation");
        RateLimiter limiter = new RateLimiter(1, 1000);

        limiter.allow("user_A"); // User A uses up their limit

        // User B should still be allowed (separate bucket)
        if (limiter.allow("user_B")) {
            System.out.println("User B: ALLOWED (Correct - unaffected by User A)");
        } else {
            System.err.println("User B: BLOCKED (Error: Isolation failed)");
        }
    }

    private static void testTimeWindowReset() throws InterruptedException {
        System.out.println("\n[Test 7] Time Window Reset");
        int window = 500; // 500ms window
        RateLimiter limiter = new RateLimiter(1, window);

        limiter.allow("user_A"); // Use limit

        if (!limiter.allow("user_A")) {
            System.out.println("Immediate Retry: BLOCKED (Correct)");
        }

        System.out.println("Sleeping for " + (window + 100) + "ms to let window expire...");
        Thread.sleep(window + 100);

        if (limiter.allow("user_A")) {
            System.out.println("Retry after Sleep: ALLOWED (Correct)");
        } else {
            System.err.println("Retry after Sleep: BLOCKED (Error: Window didn't reset)");
        }
    }
}