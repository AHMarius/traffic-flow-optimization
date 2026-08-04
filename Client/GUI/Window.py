from PyQt5.QtWidgets import QMainWindow, QWidget, QVBoxLayout
from .Panel import Panel


class Window:
    def __init__(self, windowsId: int, panelList: list[Panel]):
        self.__windowsId = windowsId
        self.__panelList = panelList

        self.__main_window = QMainWindow()
        self.__main_window.setWindowTitle(f"Fereastra {self.__windowsId}")
        self.__main_window.resize(600, 400)

        self.__central_widget = QWidget()
        self.__main_layout = QVBoxLayout()
        self.__central_widget.setLayout(self.__main_layout)
        self.__main_window.setCentralWidget(self.__central_widget)

    def showWindow(self) -> None:
        for panel in self.__panelList:
            panel.render(self.__main_layout)

        self.__main_window.show()

    def getId(self) -> int:
        return self.__windowsId