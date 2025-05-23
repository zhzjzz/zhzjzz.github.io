#include"mywindows.h"
HANDLE handle;
/*实现系统调用模块*/
//初始化句柄
void initHandle() {
	handle = GetStdHandle(STD_OUTPUT_HANDLE);
	if (!(handle != INVALID_HANDLE_VALUE)) {
		printf("获取句柄失败\n");
		exit(1);
	}
}

//2.设置颜色
void setColor(int color) {
	SetConsoleTextAttribute(handle, color);
}

//3.设置光标位置
void setPos(int x, int y) {
	COORD coord = { x * 2,y };
	SetConsoleCursorPosition(handle, coord);
}

//4.隐藏光标
void hideCursor() {
	CONSOLE_CURSOR_INFO info;
	info.bVisible = FALSE; //光标不可见
	info.dwSize = 1; //光标大小
	SetConsoleCursorInfo(handle, &info);
}
