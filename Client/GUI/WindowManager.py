from PyQt5.QtWidgets import QMessageBox
from .Window import Window

class WindowsManager:
    def __init__(self, windowCache : list[Window], currentWindow : Window) -> None:
        self.__windowCache = windowCache
        self.__currentWindow = currentWindow

    def cacheWindow(self, window : Window) -> None:
        if window is not None:
            self.__windowCache[window.getId()] = window

    def updateWindow(self, window : Window) -> None:
        if window is not None:
            self.__currentWindow = window
            self.__currentWindow.showWindow()

    """Afișează un pop-up de eroare elegant deasupra aplicatiei."""
    def showError(self, message: str) -> None:
        msg_box = QMessageBox()
        msg_box.setIcon(QMessageBox.Critical)
        msg_box.setWindowTitle("Eroare")
        msg_box.setText(message)
        """Blocheaza ecranul pana când userul apasă OK."""
        msg_box.exec_()