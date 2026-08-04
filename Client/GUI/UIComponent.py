from abc import ABC, abstractmethod
from PyQt5.QtWidgets import QVBoxLayout


class UIComponent(ABC):
    def __init__(self, id : str, visible : bool):
        self.__id = id
        self.__visible = visible

    @abstractmethod
    def render(self, parent_layout: QVBoxLayout) -> None:
        pass

    def getVisible(self) -> bool:
        return self.__visible

    def getId(self) -> str:
        return self.__id

    def setVisible(self, visible: bool):
        if visible is not None:
            self.__visible = visible

    def setId(self, id: str):
        if id is not None:
            self.__id = id