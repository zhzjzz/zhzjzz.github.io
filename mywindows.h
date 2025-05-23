#ifndef MYWINDOWS_H_INCLUDED
#define MYWINDOWS_H_INCLUDED
/*系统调用模块*/
#include <windows.h>
//函数声明
//1.初始化句柄
void initHandle();

//2.设置颜色
void setColor( int color);

//3.设置光标位置
void setPos(int x, int y);

//4.隐藏光标
void hideCursor();

#endif 

