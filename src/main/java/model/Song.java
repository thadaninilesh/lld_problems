package model;

public class Song {

    private String songTitle;
    private int songId;

    public Song(String songTitle, int songId) {
        this.songTitle = songTitle;
        this.songId = songId;
    }

    public String getSongTitle() {
        return songTitle;
    }
}
