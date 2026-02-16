public class GameOfLife {

    public static void main(String[] args) {

        int[][] board = {
                {0, 1, 0},
                {0, 0, 1},
                {1, 1, 1},
                {0, 0, 0}
        };

        new GameOfLife().gameOfLife(board);
    }

    public void gameOfLife(int [][]board) {
        int rows = board.length;
        int cols = board[0].length;
        int []neighbors = {0,1,-1};

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int liveNeighbors = 0;

                //Check all 8 neighbors
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        if (!(neighbors[i] == 0 && neighbors[j] == 0)) {
                            int nr = r + neighbors[i];
                            int nc = c + neighbors[j];

                            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols &&
                                    (board[nr][nc] == 1 || board[nr][nc] == 2)) {
                                liveNeighbors++;
                            }
                        }
                    }
                }

                if (board[r][c] == 1 && (liveNeighbors < 2 || liveNeighbors > 3)) {
                    board[r][c] = 2;
                }

                if (board[r][c] == 0 && liveNeighbors == 3){
                    board[r][c] = 3;
                }
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == 2) board[r][c] = 0;
                else if (board[r][c] == 3) board[r][c] = 1;
            }
        }

    }

}
