# :rabbit: 콩알콩알 오목 게임:
소켓 서버를 사용하여 사용자 간 실시간 상호작용이 가능하고 전략적 사고를 통해 재미와 경쟁심을 동시에 제공하는 2인용 오목 게임을 개발하게 되었습니다.

![image](https://github.com/user-attachments/assets/669fd709-8c89-474b-948d-47ad66f541b4)


<br/>

## 👉🏻 주요 기능
- 돌 놓기: 플레이어가 자신의 차례에 돌을 놓습니다.
- 승리 조건: 5개 이상의 돌이 연속으로 놓이면 승리합니다.
- 금지된 수: 삼삼, 사사, 장목과 같은 금지된 수를 감지합니다.
- 무르기 요청: 상대방에게 무르기를 요청하고, 수락 또는 거절할 수 있습니다.
- 재생 모드: 게임 종료 후 기록을 다시 볼 수 있는 기능을 제공합니다.
- 채팅 관리 : 실시간으로 메시지를 주고받을 수 있으며, 상대방의 메시지를 확인할 수 있습니다.


<br/>

## 👉🏻 실행 방법
1. 서버 실행 (GomokuServer):
GomokuServer를 실행하여 서버를 시작합니다.
서버는 두 명의 플레이어가 연결될 때까지 대기 상태로 유지됩니다.
2. 클라이언트 실행 (GameApp):
플레이어는 각자의 PC에서 GameApp을 실행합니다.
GameApp 실행 후 "Start Game" 버튼을 클릭하면 GomokuClient가 실행되며, 서버에 연결을 시도합니다.
3. 두 번째 클라이언트 실행:
다른 플레이어도 GameApp을 실행하고 동일하게 서버에 연결합니다.
두 명의 플레이어가 모두 연결되면 서버에서 각 플레이어의 역할(Player 1 또는 Player 2)을 지정하고 게임이 시작됩니다.
4. 게임 진행:
서버는 두 클라이언트 간 게임 상태를 동기화하며, 돌 놓기, 승리 여부, 금지된 수 감지 등을 처리합니다.
클라이언트는 서버의 응답에 따라 UI를 갱신하며 게임을 진행합니다.
5. 게임 종료 후 기록 재생:
게임 종료 시 클라이언트에서 재생 모드가 활성화되어 게임 기록을 다시 볼 수 있습니다.

<br/>

## 📝 페이지 구성
<details>
<summary>메인 페이지</summary>
<div markdown="1">
<br/> 메인 화면<br/>
<img width="580" alt="메인 화면" src="https://github.com/user-attachments/assets/3cccf098-241c-4c5a-b87e-8db80ae43755">

</div>
</details>
<details>
<summary>게임 설명 페이지</summary>
<div markdown="1">
<br/>게임 설명 화면<br/>
<img width="580" alt="게임 설명 화면" src="https://github.com/user-attachments/assets/19b06081-1541-4fe5-a762-5b7d81014cb7">

</div>
</details>
<details>
<summary>Player1 게임 화면</summary>
<div markdown="1">
<br/>Player1 게임 화면<br/>
<img width="580" alt="Player1 게임 화면" src="https://github.com/user-attachments/assets/537bdd53-3e76-4c0a-957b-a97b5ff6c51b">

</div>
</details>
<details>
<summary>Player2 게임 화면</summary>
<div markdown="1">
<br/>Player2 게임 화면<br/>
<img width="580" alt="Player2 게임 화면" src="https://github.com/user-attachments/assets/9b61dc59-0e48-4827-b5cd-e799c6b05c6c">

</div>
</details>

<br/>

## 💻 Teachnology
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Socket.io](https://img.shields.io/badge/Socket.io-black?style=for-the-badge&logo=socket.io&badgeColor=010101)

![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)
![KakaoTalk](https://img.shields.io/badge/kakaotalk-ffcd00.svg?style=for-the-badge&logo=kakaotalk&logoColor=000000)
![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-%235865F2.svg?style=for-the-badge&logo=discord&logoColor=white)

