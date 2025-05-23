#define _CRT_SECURE_NO_WARNINGS
#include "game.h"
#include "mywindows.h"
#include "data.h"
#include<conio.h>
#include<time.h>
#include<mmsystem.h>
/*实现游戏逻辑模块*/
int grade = 0; //分数
BLOCK cur_block; //当前方块
BLOCK next_block; //下一个方块
//1.绘制游戏池边框
void windowPrint() {
	//打印上边框
	for (int i = 0; i < 22; i++) {
		for (int j = 0; j < 26; j++) {
			if (windowShape[i][j] == 1) {
				setColor(0xc0);
				setPos(j, i);
				printf("%2s", "");
			}
		}
	}
}

//2.打印操作说明
void printInfo() {
	setColor(12);
	setPos(33, 9);
	printf("开始游戏");


	setColor(0x0a);
	setPos(33, 15);
	printf("操作说明");
	setPos(31, 17);
	printf("A/←:左移");
	setPos(31, 18);
	printf("D/→:右移");
	setPos(31, 19);
	printf("S/↓:下移");
	setPos(31, 20);
	printf("W/↑:旋转");
	setPos(31, 21);
	printf("回车直接下落");
	setPos(31, 22);
	printf("空格暂停");
}

//3.打印分数
void printGrade(int num) {
	switch (num) {
	case 0:
		break;
	case 1:
		grade += 100; break;
	case 2:
		grade += 300; break;
	case 3:
		grade += 500; break;
	case 4:
		grade += 800; break;
	}

}
//5.计时
void gameTime(clock_t start_time) {

}

//6.打印方块
void printBlock(int x,int y,int shape, int status) {
	for (int i = 0; i < 4; i++) {
		for (int j = 0; j < 4; j++) {
			if (block[shape][status][i][j] == 1) {
				/*setColor(0x01);
				setPos(x+j, y+i);
				printf("■");*/
				setColor(0x10);
				setPos(x+j,y+ i);
				printf("%2s", "");
			}
		}
	}
}

//7.删除方块
void deleteBlock(int x,int y,int shape, int status) {
	for (int i = 0; i < 4; i++) {
		for (int j = 0; j < 4; j++) {
			if (block[shape][status][i][j] == 1) {
				setColor(0x00);
				setPos(x+j,y+ i);
				printf("%2s", "");
			}
		}
	}
}

//8.产生第一个方块
void startBlock() {
	srand((unsigned)time(NULL));
	cur_block.x = 10; //初始位置
	cur_block.y = 1; //初始位置
	cur_block.shape = rand() % 7; //随机产生方块
	cur_block.status = 0; //初始状态
	printBlock(cur_block.x, cur_block.y,
		cur_block.shape, cur_block.status);
}

//9.产生下一个方块
void nextBlock() {
	next_block.x = 33; //初始位置
	next_block.y = 2; //初始位置
	deleteBlock(next_block.x, next_block.y,
		next_block.shape, next_block.status);
	next_block.shape = rand() % 7; //随机产生方块
	next_block.status = 0; //初始状态
	printBlock(next_block.x, next_block.y,
		next_block.shape, next_block.status);
}

//10.拷贝方块
void copyBlock() {
	cur_block = next_block; //拷贝下一个方块
	cur_block.x = 10; //初始位置
	cur_block.y = 1; //初始位置
	printBlock(cur_block.x, cur_block.y,
		cur_block.shape, cur_block.status);
	nextBlock(); //产生下一个方块
}

//11.方块下移
//返回值-1表示碰撞，0表示没有碰撞，-2表示游戏结束
int downBlock() {
	int res = crash(cur_block.x, cur_block.y,
		cur_block.shape, cur_block.status);
	if (res == -1) {
		//发生碰撞
		save();
		lineClear();
		updateGame();
		copyBlock();
		return -1;
	}
	else if (res == -2) {
		//游戏结束
		return -2;
	}
	else {
		deleteBlock(cur_block.x, cur_block.y,
			cur_block.shape, cur_block.status);
		cur_block.y++; //下移
		printBlock(cur_block.x, cur_block.y,
			cur_block.shape, cur_block.status);
		return 0;
	}

}

//12.方块左移
void leftBlock() {
	int res = crash(cur_block.x - 1, cur_block.y,
		cur_block.shape, cur_block.status);
	if (res == -1) {
		//发生碰撞
		return;
	}
	else {
		deleteBlock(cur_block.x, cur_block.y,
			cur_block.shape, cur_block.status);
		cur_block.x--; //左移
		printBlock(cur_block.x, cur_block.y,
			cur_block.shape, cur_block.status);
		return;
	}
}

//13.方块右移
void rightBlock() {
	int res = crash(cur_block.x + 1, cur_block.y,
		cur_block.shape, cur_block.status);
	if (res == -1) {
		//发生碰撞
		return;
	}
	else {
		deleteBlock(cur_block.x, cur_block.y,
			cur_block.shape, cur_block.status);
		cur_block.x++; //右移
		printBlock(cur_block.x, cur_block.y,
			cur_block.shape, cur_block.status);
		return;
	}
}

//14.方块旋转
void changeBlock() {
	if (crash(cur_block.x, cur_block.y,
		cur_block.shape, (cur_block.status + 1) % 4) == -1) {
		//发生碰撞
		return;
	}
	else {
		deleteBlock(cur_block.x, cur_block.y,
			cur_block.shape, cur_block.status);
		cur_block.status = (cur_block.status + 1) % 4; //旋转
		printBlock(cur_block.x, cur_block.y,
			cur_block.shape, cur_block.status);
		return;
	}

}

//15.直接落底
void bottomBlock() {

	while (1) {
		int res = crash(cur_block.x + 1, cur_block.y,
			cur_block.shape, cur_block.status);
		if (res == -1) {
			//发生碰撞
			save();
			lineClear();
			updateGame();
			copyBlock();
			return;
		}
		else {
			cur_block.y++; //下移
		}
	}
}

//16.碰撞检查

int crash(int x, int y, int shape, int status) {
	for (int i = 0; i < 4; i++) {
		for (int j = 0; j < 4; j++) {
			if (block[shape][status][i][j] == 1) {
				int wx = x + j;
				int wy = y + i;
				if (windowShape[wy][wx] == 1) {
					return -1;
				}
			}
		}
	}
	return 0; // 没有碰撞
}

//17.保存方块
void save() {
	for (int i = 0; i < 4; i++) {
		for (int j = 0; j < 4; j++) {
			if (block[cur_block.shape][cur_block.status][i][j] == 1) {
				int wx = cur_block.x + j;
				int wy = cur_block.y + i;
				windowShape[wy][wx] = 1;
			}
		}
	}
}

//18.刷新游戏池
void updateGame() {
	for (int i = 20; i > 0; i--) {
		for (int j = 1; j < 16; j++) {
			if (i < 4 && windowShape[i][j] == 1) {
				printOver();
				printFinish();
				return;
			}
				
			if (windowShape[i][j] == 1) {
				setColor(0x0a);
				setPos(j, i);
				printf("■");
			}
			else {
				setColor(0x00);
				setPos(j, i);
				printf("%2s", "");
			}
		}
	}
}

//19.游戏暂停
void pause() {
	while (1) {
		if (_getch() == 32) {
			break;
		}
	}
}

//20.消行检测
void lineClear() {
	int number = 0;
	for (int i = 20; i > 0; i--) {
		int total = 0;
		for (int j = 1; j < 16; j++) {
			if (windowShape[i][j] == 0) {
				break;
			}
			else {
				total++;
			}
		}
		if (total == 15) {
			lineDown(i);
			i++;
			number++;
		}
	}
	printGrade(number);
}

//21.消行下移
void lineDown(int line) {
	for (int i = line; i > 1; i--) {
		for (int j = 1; j < 16; j++) {
			windowShape[i][j] = windowShape[i - 1][j];
		}
	}
}

//22.游戏结束
void printOver() {
	for (int i = 20; i > 0; i--) {

		for (int j = 1; j < 16; j++) {
			setColor(0x2e);
			setPos(j, i);
			printf("游戏结束");
		}
	}
}

//23.游戏开始
void printFinish() {
	setColor(0x0f);
	setPos(20, 8);
	printf("游戏结束");
	setPos(20, 9);
	printf("按Y重新开始 ");
	setPos(20, 10);
	printf("按N退出游戏");
	switch (getch()) {
	case 'Y':
	case 'y':
		againGame();
		break;
	case 'N':
	case 'n':
		endGame();
		break;
	default:
		printFinish();
	}
}

//24.重新开始游戏
void againGame() {
	//清除游戏池
	for (int i = 1; i < 21; i++) {
		for (int j = 1; j < 16; j++) {
			windowShape[i][j] = 0;
		}
	}
	system("cls");
	gameInit();

}

//25.游戏结束界面
void endGame() {
	system("cls");
	setPos(21, 8);
	setColor(12);
	printf("游戏结束");

	setPos(21, 10);
	printf("分数：%d", grade);
	setPos(21, 12);
	printf("按任意键退出");
	_getch();
	exit(0);
}

//26.游戏开始界面
void printStart() {
	setPos(25, 15);
	printf("回车开始游戏");

}

//27.清除开始动画
void deleteStart(int x, int y) {

}
//30.游戏模式
//time:游戏速度 n:游戏模式
void gameMode(float time) {
	clock_t startTime = clock();
	clock_t time1, time2;
	time1 = clock();
	while (1) {
		if (_kbhit()) {
			switch (getch()) {
				//方块变形
			case'w':
			case'W':
			case 72:
				changeBlock();
				break;
				//方块左移
			case'a':
			case'A':
			case 75:
				leftBlock();
				break;
				//方块右移
			case'd':
			case'D':
			case 77:
				rightBlock();
				break;
				//方块下移
			case's':
			case'S':
			case 80:
				downBlock();
				break;
				//直接落底
			case 13:
				bottomBlock();
				break;
				//暂停
			case 32:
				pause();
				break;
				//退出
			case 27:
				endGame();
				break;
			}
		}
		time2 = clock();
		if ((time2 - time1) > time * CLOCKS_PER_SEC) {

			if (downBlock() == -2) {
				break;
			}
			time1 = time2;

		}
	}

}
//31.游戏模式选择
void gameModeChoose() {
	system("cls");
	setColor(0x0a);
	setPos(20, 8);
	printf("输入任意数字开始游戏");

	int n;
	scanf("%1d", &n);
	while (getchar() != '\n');
	if (n >= 0 && n <= 9) {
		system("cls");
		windowPrint();
		printInfo();

		printGrade(0);
		startBlock();
		nextBlock();
		gameMode(0.65);
		
		printOver();
		printFinish();
	}
	else {
		setPos(20, 10);
		printf("输入错误，请重新输入");
		gameModeChoose();
	}
}

//31.游戏初始化
void gameInit() {
	//初始化窗口
	initHandle();
	hideCursor();
	gameModeChoose();



}