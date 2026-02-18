import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TrendingVideo {

    static class LikeLog {
        String videoId;
        long time;

        public LikeLog(String videoId, long time) {
            this.videoId = videoId;
            this.time = time;
        }
    }

    static class VideoStats {
        private final String videoId;
        private final long uploadTime;
        private final String uploadUserId;
        private final List<Long> likes = new ArrayList<>();

        public VideoStats(String videoId, long uploadTime, String uploadUserId) {
            this.videoId = videoId;
            this.uploadTime = uploadTime;
            this.uploadUserId = uploadUserId;
        }

        public int getActiveLikeCount(long time) {
            long threshold = time - (24*60*60*1000);
            int idx = Collections.binarySearch(likes, threshold);
            if (idx < 0) {
                idx = -(idx) - 1;
            }
            return likes.size() - idx;
        }

        public void addLike(long time) {
            likes.add(time);
        }
    }

    private final Map<String, VideoStats> videoStatsMap = new ConcurrentHashMap<>();
    private final List<LikeLog> allLikes = new ArrayList<>();

    public void uploadLog(String videoId, String userId, long time) {
        videoStatsMap.put(videoId, new VideoStats(videoId, time, userId));
    }

    public void likeLog(String videoId, String userId, long time) {
        videoStatsMap.get(videoId).addLike(time);
        allLikes.add(new LikeLog(videoId, time));
    }

    // Time Complexity: O(V * log L) where V is videos, L is max likes per video
    public String getTrendingVideo(long currentTime) {
        String trendingVideo = null;
        long latestUpload = -1;
        long maxLikes = -1;

        for (VideoStats stats: videoStatsMap.values()) {
            int currentLikes = stats.getActiveLikeCount(currentTime);
            if (currentLikes > maxLikes) {
                maxLikes = currentLikes;
                trendingVideo = stats.videoId;
                latestUpload = stats.uploadTime;
            } else if (currentLikes == maxLikes && maxLikes != -1) {
                if (stats.uploadTime > latestUpload) {
                    trendingVideo = stats.videoId;
                    latestUpload = stats.uploadTime;
                }
            }
        }
        return maxLikes > 0 ? trendingVideo : null;
    }

    static class Event {
        long time;
        String videoId;
        int type; // +1 or -1

        public Event(long time, String videoId, int type) {
            this.time = time;
            this.videoId = videoId;
            this.type = type;
        }
    }

    // This uses the "Line Sweep" algorithm to replay history efficiently
    public Set<String> getStarredUser() {
        Set<String> starredUsers = new HashSet<>();

        List<Event> events = new ArrayList<>();
        for (LikeLog like: allLikes) {
            events.add(new Event(like.time, like.videoId, 1));
            events.add(new Event(like.time + (24*60*60*1000), like.videoId, -1));
        }

        events.sort(Comparator.comparingLong(a -> a.time));

        Map<String, Integer> currentLikes = new HashMap<>();

        for (Event e: events) {
            currentLikes.put(e.videoId, currentLikes.getOrDefault(e.videoId, 0) + 1);

            // Optimization: Only re-evaluate winner if the modified video was the winner
            // OR if the modified video's score is now potentially the new max
            // For interview simplicity, scanning active videos is okay, but using a Heap is better.

            // Simple Scan for Winner (O(V) per event) - can be optimized with MaxHeap

            String roundWinner = null;
            int currentMax = -1;
            long winnerUploadTime = -1;

            for (Map.Entry<String, Integer> entry: currentLikes.entrySet()) {
                int score = entry.getValue();
                String videoId = entry.getKey();
                long uploadTime = videoStatsMap.get(videoId).uploadTime;

                if (score > currentMax) {
                    currentMax = score;
                    roundWinner = videoId;
                    winnerUploadTime = uploadTime;
                } else if (score == currentMax && currentMax > 0) {
                    if (uploadTime > winnerUploadTime) {
                        roundWinner = videoId;
                        winnerUploadTime = uploadTime;
                    }
                }
            }

            if (roundWinner != null) {
                String uploadUserId = videoStatsMap.get(roundWinner).uploadUserId;
                starredUsers.add(uploadUserId);
            }
        }

        return starredUsers;

    }

    public static void main(String[] args) {
        TrendingVideo vp = new TrendingVideo();
        long T0 = 1000000; // Start time reference

        // 1. Uploads
        System.out.println("--- Uploading Videos ---");
        vp.uploadLog("V1", "Alice", T0);          // Oldest
        vp.uploadLog("V2", "Bob",   T0 + 100);    // Newer
        vp.uploadLog("V3", "Charlie", T0 + 200);  // Newest

        // 2. Initial Likes
        System.out.println("--- Adding Initial Likes ---");
        vp.likeLog("V1", "User1", T0 + 1000); // V1 has 1 like

        // Check 1: V1 should be trending
        String trend1 = vp.getTrendingVideo(T0 + 1500);
        System.out.println("T+1500: Trending is " + trend1 + " (Expected: V1)");

        // 3. Tie-Breaker Test
        vp.likeLog("V2", "User2", T0 + 2000); // V2 has 1 like

        // Check 2: V1 and V2 have 1 like. V2 is newer. V2 should win.
        String trend2 = vp.getTrendingVideo(T0 + 2500);
        System.out.println("T+2500: Trending is " + trend2 + " (Expected: V2 - Tie Breaker)");

        // 4. Sliding Window Test (Expiry)
        long ONE_DAY = 24L * 60 * 60 * 1000;

        // Move time to just AFTER V1's like expires
        // V1 like time: T0 + 1000 -> Expires at T0 + 1000 + ONE_DAY
        long timeCheck3 = T0 + 1000 + ONE_DAY + 1;

        // V1 count = 0. V2 count = 1 (since V2 like was at T0+2000, not expired yet).
        String trend3 = vp.getTrendingVideo(timeCheck3);
        System.out.println("After V1 Expiry: Trending is " + trend3 + " (Expected: V2)");

        // 5. Star Calculation Test
        // Alice was trending at T+1500.
        // Bob was trending at T+2500.
        // Charlie never got likes, so never trended.
        System.out.println("\n--- Calculating Stars ---");
        Set<String> stars = vp.getStarredUser();
        System.out.println("Starred Users: " + stars);

        boolean passed = stars.contains("Alice") && stars.contains("Bob") && !stars.contains("Charlie");
        System.out.println("Test Passed: " + passed);
    }



}