from . import WindowManager
from .WindowManager import WindowsManager
from .Window import Window

class GUIFacade(WindowsManager):
    def __init__(self, windowCache : list[Window], currentWindow : Window, dataBase, windowManager : WindowManager):
        super().__init__(windowCache, currentWindow)
        self.__dataBase = dataBase
        self.__windowManager = windowManager

    def makeRequest(self, request, priority) -> None:
        pass