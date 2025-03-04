package com.example.puzzlegame.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@NoArgsConstructor
@Setter
@Getter
public class GameDTO {

    private String board;
    private int score;
    private int earnedScore;
    private boolean gameOver;
    private boolean gameClear;

    public GameDTO(String board, int score, int earnedScore, boolean gameOver, boolean gameClear) {
        this.board = board;
        this.score = score;
        this.earnedScore = earnedScore;
        this.gameOver = gameOver;
        this.gameClear = gameClear;
    }
}
