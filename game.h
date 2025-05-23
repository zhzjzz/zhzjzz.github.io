#ifndef GAME_H_INCLUDED
#define GAME_H_INCLUDED
/*游戏逻辑模块*/
#include<stdio.h>
#include<time.h>
//方块结构体
typedef struct block {
	int x;          
	int y; 
	int shape;
	int status;
} BLOCK;
//1.绘制游戏池边框
void windowPrint();

//2.游戏初始化
void gameInit();

//3.打印操作说明
void printInfo();

//4.打印分数
void printGrade(int score);

//5.计时
void gameTime(clock_t start_time);

//6.左边框
void borderLeft(int x, int y);

//7.打印方块
void printBlock(int x, int y, int shape, int status);

//8.删除方块
void deleteBlock(int x, int y, int shape, int status);

//9.产生第一个方块
void startBlock();

//10.产生下一个方块
void nextBlock();

//11.拷贝方块
void copyBlock();

//12.方块下移
//返回值：0表示可以下移，1表示不能下移
int downBlock();

//13.方块左移
void leftBlock();

//14.方块右移
void rightBlock();

//15.方块旋转
void changeBlock();

//16.直接落底
void bottomBlock();

//17.碰撞检查
int crash(int x, int y, int shape, int status);

//18.保存方块
void save();

//19.刷新游戏池
void updateGame();

//20.游戏暂停
void pause();

//21.消行检测
void lineClear();

//22.消行下移
void lineDown(int line);

//23.游戏结束
void printOver();

//24.游戏开始
void printFinish();

//25.重新开始游戏
void againGame();

//26.游戏结束界面
void endGame();

//27.游戏开始界面
void printStart(int x,int y);

//28.清除开始动画
void deleteStart(int x, int y);

//29.游戏开始动画
void printANimation(float time);

//30.游戏模式
void gameMode(float time);

//31.游戏模式选择
void gameModeChoose();

#endif // !GAME_H_INCLUDED

