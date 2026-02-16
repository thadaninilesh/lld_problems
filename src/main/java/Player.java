import model.Song;

import java.util.*;

public class Player {

    private int songCounter = 0;

    // using concurrent hash map or synchronized blocks for better concurreny management
    private Map<Integer, Integer> songPlayCount = new HashMap<>();
    private Map<Integer, Song> songsInPlayer = new HashMap<>();
    private Map<Integer, List<Integer>> userHistory = new HashMap<>();


    // check for duplicates as an extension
    public int addSong(String songTitle) {
        songCounter++;
        songsInPlayer.put(songCounter, new Song(songTitle, songCounter));
        songPlayCount.put(songCounter, 0);
        return songCounter;
    }

    public void playSong(int songId, int userId) {
        if (songsInPlayer.containsKey(songId)) {
            songPlayCount.put(songId, songPlayCount.get(songId) + 1);

            userHistory.putIfAbsent(userId, new LinkedList<>());
            List<Integer> history = userHistory.get(userId);

            if (history.size() > 3) {
                history.remove(3); // we are deleting this the moment list size is above 3
            }
        }
    }

    public void printMostPlayedSongs() {
        PriorityQueue<Map.Entry<Integer, Integer>> pq = new PriorityQueue<>(
                (a,b) -> b.getValue().compareTo(a.getValue())
        );
        pq.addAll(songPlayCount.entrySet());
        int count = 0;
        while (!pq.isEmpty() && count < 5) {
            count++;
            Map.Entry<Integer, Integer> songData = pq.poll();
            Integer songId = songData.getKey();
            Integer songPlayCount = songData.getValue();
            Song song = songsInPlayer.get(songId);
            System.out.println("Song: " + song.getSongTitle() + " played " + songPlayCount + " times");
        }
    }

    public List<Integer> getLastThreeSongs(int userId) {
        return userHistory.getOrDefault(userId, new LinkedList<>());
    }

}
