package com.example.puzzlegame.service;

import com.example.puzzlegame.dto.GameDTO;
import com.example.puzzlegame.entity.Direction;
import com.example.puzzlegame.entity.Game;
import com.example.puzzlegame.repository.GameRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameService {

    private int[][] board;


    private final GameRepository gameRepository;
    private final Random random = new Random();


    public Game startNewGame() {
        Game game = new Game();
        board = initializeBoard();  // board 초기화
        game.setBoard(boardToJson(board));  // board를 JSON 형식으로 저장
        game.setScore(0);
        game.setGameOver(false);
        game.setGameClear(false);
        return gameRepository.save(game);
    }

    public Game getGame(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        // board를 JSON에서 int[][] 배열로 변환하여 반환
        board = jsonToBoard(game.getBoard());
        return game;
    }

    private int[][] initializeBoard() {
        int[][] board = new int[4][4];
        addRandomNumber(board);
        addRandomNumber(board);
        return board;
    }

    private void addRandomNumber(int[][] board) {
        List<int[]> emptyCells = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (board[i][j] == 0) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            int[] pos = emptyCells.get(random.nextInt(emptyCells.size()));
            board[pos[0]][pos[1]] = (random.nextDouble() < 0.9) ? 2 : 4;

            // ✅ 디버깅 로그 추가
            System.out.println("추가된 숫자: " + board[pos[0]][pos[1]] + " 위치: [" + pos[0] + "," + pos[1] + "]");
        }
    }

    public String boardToJson(int[][] board) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(board);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting board to JSON");
        }
    }

    private int[][] jsonToBoard(String boardJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(boardJson, int[][].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to board");
        }
    }


    private int[] moveRow(int[] row) {
        List<Integer> newRow = new ArrayList<>();
        for(int num : row) {
            if (num != 0){
                newRow.add(num);
            }
        }
        while (newRow.size() < 4) {
            newRow.add(0);
        }
        return newRow.stream().mapToInt(i -> i).toArray();
    }

    private int[] mergeRow(int[] row, Game game) {
        for (int i = 0; i < 3; i++) {
            if (row[i] != 0 && row[i] == row[i + 1]) {
                row[i] *= 2;
                game.setScore(game.getScore() + row[i]); // 합쳐진 값만큼 점수 증가
                row[i + 1] = 0;
                i++;  // 다음 칸은 이미 합쳐졌으므로 건너뜀
            }
        }
        return moveRow(row);
    }

    // 한 행에서 오른쪽으로 이동하고 합치는 로직

    private int[] reverseRow(int[] row) {
        int[] reversedRow = new int[4];
        for (int i = 0; i < 4; i++) {
            reversedRow[i] = row[3 - i];
        }
        return reversedRow;
    }


    public ResponseEntity<GameDTO> move(Game game, Direction direction) {
        int[][] board = jsonToBoard(game.getBoard());
        int[][] previousBoard = copyBoard(board);
        int previousScore = game.getScore();  // 기존 점수 저장

        // 방향에 따른 보드 이동 로직 처리
        switch (direction) {
            case LEFT:
                for (int i = 0; i < 4; i++) {
                    board[i] = mergeRow(moveRow(board[i]), game);
                }
                break;
            case RIGHT:
                for (int i = 0; i < 4; i++) {
                    board[i] = reverseRow(mergeRow(moveRow(reverseRow(board[i])), game));
                }
                break;
            case UP:
                for (int col = 0; col < 4; col++) {
                    int[] column = new int[4];
                    for (int row = 0; row < 4; row++) {
                        column[row] = board[row][col];
                    }
                    column = mergeRow(moveRow(column), game);
                    for (int row = 0; row < 4; row++) {
                        board[row][col] = column[row];
                    }
                }
                break;
            case DOWN:
                for (int col = 0; col < 4; col++) {
                    int[] column = new int[4];
                    for (int row = 0; row < 4; row++) {
                        column[row] = board[row][col];
                    }
                    column = reverseRow(mergeRow(moveRow(reverseRow(column)), game));
                    for (int row = 0; row < 4; row++) {
                        board[row][col] = column[row];
                    }
                }
                break;
        }

        // 이동이 없었으면 게임 오버 체크 후 종료
        if (isSameBoard(previousBoard, board)) {
            // 게임 오버 체크
            if (isGameOver(board)) {
                game.setGameOver(true);
            }
            gameRepository.save(game);

            int earnedScore = game.getScore() - previousScore;  // 새 점수에서 기존 점수를 뺀 값
            return ResponseEntity.ok(new GameDTO(game.getBoard(), game.getScore(), earnedScore, game.isGameOver(), game.isGameClear()));
        }

        // 보드에 새로운 숫자 추가
        addRandomNumber(board);
        game.setBoard(boardToJson(board));

        // 게임 오버 체크
        if (isGameOver(board)) {
            game.setGameOver(true);
        }

        // 게임 클리어 체크 (2048 타일을 만들었을 때)
        if (checkGameClear(board)) {
            game.setGameClear(true);  // 게임 클리어 상태 설정
        }

        int earnedScore = game.getScore() - previousScore;  // 새 점수에서 기존 점수를 뺀 값

        gameRepository.save(game);  // 게임 상태 저장

        // 게임 응답 반환
        return ResponseEntity.ok(new GameDTO(game.getBoard(), game.getScore(), earnedScore, game.isGameOver(), game.isGameClear()));
    }

    private boolean checkGameClear(int[][] board) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == 2048) {
                    return true;  // 2048 타일이 있으면 게임 클리어
                }
            }
        }
        return false;  // 없으면 게임 클리어 아님
    }



    private boolean isSameBoard(int[][] board1, int[][] board2) {
        for (int i = 0; i < board1.length; i++) {
            for (int j = 0; j < board1[i].length; j++) {
                if (board1[i][j] != board2[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[][] copyBoard(int[][] board) {
        int[][] newBoard = new int[board.length][board[0].length];
        for (int i = 0; i < board.length; i++) {
            newBoard[i] = board[i].clone();
        }
        return newBoard;
    }


    public int[][] getBoard(Long gameId) {
        Game game = getGame(gameId);  // 이미 정의된 getGame 메서드 사용
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(game.getBoard(), int[][].class);  // JSON 문자열을 int[][] 배열로 변환
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting board from JSON", e);
        }
    }

    private boolean isGameOver(int[][] board) {
        // 1. 빈 칸(0)이 하나라도 있으면 게임 오버가 아님
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (board[i][j] == 0) {
                    return false;  // 빈 칸이 있으면 아직 게임 가능
                }
            }
        }

        // 2. 가로로 같은 숫자가 있는지 확인 (좌우 이동 가능 여부)
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == board[i][j + 1]) {
                    return false;  // 가로 방향으로 합칠 수 있는 숫자가 있음
                }
            }
        }

        // 3. 세로로 같은 숫자가 있는지 확인 (위아래 이동 가능 여부)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                if (board[i][j] == board[i + 1][j]) {
                    return false;  // 세로 방향으로 합칠 수 있는 숫자가 있음
                }
            }
        }

        // 위 조건을 모두 통과하면 게임 오버
        return true;
    }

}
