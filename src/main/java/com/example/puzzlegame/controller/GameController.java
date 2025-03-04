package com.example.puzzlegame.controller;

import com.example.puzzlegame.dto.GameDTO;
import com.example.puzzlegame.dto.MoveDTO;
import com.example.puzzlegame.entity.Direction;
import com.example.puzzlegame.entity.Game;
import com.example.puzzlegame.repository.GameRepository;
import com.example.puzzlegame.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final GameRepository gameRepository;

    @PostMapping("/start")
    public ResponseEntity<Game> startNewGame() {
        Game game = gameService.startNewGame();
        return ResponseEntity.ok(game);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<Game> getGame(@PathVariable Long gameId) {
        Game game = gameService.getGame(gameId);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<GameDTO> move(@PathVariable Long gameId, @RequestBody MoveDTO moveRequest) {
        Game game = gameService.getGame(gameId);  // 게임 ID로 게임 정보 불러오기
        int previousScore = game.getScore();  // 기존 점수 저장
        gameService.move(game, moveRequest.getDirection());  // 이동 처리
        int earnedScore = game.getScore() - previousScore;  // 이동 후 얻은 점수 계산

        GameDTO response = new GameDTO(
                game.getBoard(),
                game.getScore(),
                earnedScore,
                game.isGameOver(),
                game.isGameClear()
        );

        return ResponseEntity.ok(response);  // 이동 후 게임 상태 반환
    }



    @GetMapping("/{gameId}/board")
    public String getBoard(@PathVariable Long gameId) {
        int[][] board = gameService.getBoard(gameId);
        return gameService.boardToJson(board);  // 보드를 JSON 문자열로 반환
    }
}
