let gameId = null;
let score = 0;  // 기본 점수 변수

document.addEventListener("DOMContentLoaded", function() {
    if (!gameId) {
        startNewGame();  // 처음 페이지 로드 시 새로운 게임 시작
    }
});

let isKeyPressed = false; // 키 입력 중인지 체크

document.addEventListener("keydown", function(event) {
    if (isKeyPressed) return; // 이미 키가 눌려있다면 무시
    isKeyPressed = true; // 키 입력 중임을 표시

    let direction = null;
    switch (event.key) {
        case "ArrowLeft":
            direction = "LEFT";
            break;
        case "ArrowRight":
            direction = "RIGHT";
            break;
        case "ArrowUp":
            direction = "UP";
            break;
        case "ArrowDown":
            direction = "DOWN";
            break;
    }

    if (direction) {
        move(direction);
    }
});

document.addEventListener("keyup", function() {
    isKeyPressed = false; // 키를 떼면 다시 입력 가능하게 변경
});


function startNewGame() {
    fetch('/game/start', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => response.json())
        .then(data => {
            gameId = data.id;  // 게임 ID 저장
            score = 0;  // 게임 시작 시 점수 초기화
            updateScore();  // 점수 UI 초기화
            loadBoard();  // 보드 불러오기
        })
        .catch(error => console.error('Error starting new game:', error));
}

// 보드 데이터를 서버에서 받아와서 화면에 표시
function loadBoard() {
    fetch(`/game/${gameId}/board`)
        .then(response => response.json())
        .then(data => {
            console.log("받아온 보드 데이터:", data);  // 데이터 확인
            board = data;
            updateBoardUI();
        })
        .catch(error => console.error('Error loading board:', error));
}

// 보드 UI 업데이트
function updateBoardUI() {
    const boardDiv = document.getElementById('board');
    boardDiv.innerHTML = '';  // 기존 보드 초기화

    board.forEach(row => {
        row.forEach(cell => {
            const cellDiv = document.createElement('div');
            cellDiv.textContent = cell === 0 ? '' : cell;
            cellDiv.style.backgroundColor = getCellColor(cell);
            boardDiv.appendChild(cellDiv);
        });
    });
}

// 셀 색상 결정
function getCellColor(value) {
    switch (value) {
        case 0: return '#eee'; // 빈 칸
        case 2: return '#f0e68c';
        case 4: return '#f9d342';
        case 8: return '#f1a0a7';
        case 16: return '#f07262';
        case 32: return '#ff6347';
        case 64: return '#ff4500';
        case 128: return '#ff8c00';
        case 256: return '#ff7f50';
        case 512: return '#e64a19';
        case 1024: return '#ff5722';
        case 2048: return '#ff9800';
        default: return '#ddd';
    }
}

// 서버에 이동 요청
function move(direction) {
    fetch(`/game/${gameId}/move`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ direction: direction })
    })
        .then(response => response.json())
        .then(data => {
            console.log("서버 응답 데이터:", data);

            // 새롭게 얻은 점수만 추가
            if (data.earnedScore !== undefined) {
                score += data.earnedScore;  // 누적 점수에 추가
            }

            updateScore();  // 점수 UI 업데이트
            loadBoard();    // 보드 갱신

            // 게임 오버 체크
            if (data.gameOver) {
                alert("게임 오버! 다시 시작하려면 새 게임을 시작하세요.");
            }

            // 게임 클리어 체크
            if (data.gameClear) {
                alert("게임 클리어! 2048 타일을 만들었습니다.");
            }
        })
        .catch(error => console.error('Error moving:', error));
}
// 점수 UI 업데이트
function updateScore() {
    const scoreElement = document.getElementById('score');
    if (!scoreElement) {
        console.error("점수를 표시할 요소가 없습니다.");
        return;
    }
    scoreElement.textContent = score;
    console.log("점수 업데이트:", score); // 디버깅 로그 추가
}