from PyQt5.QtWidgets import QPushButton, QVBoxLayout
from .UIComponent import UIComponent

class Panel(UIComponent):
    def __init__(self, id : str, visible : bool):
        super().__init__(id, visible)
        self.__children: list[UIComponent] = []
        self.__panou = QVBoxLayout()

    def addChild(self, component : UIComponent) -> None:
        if component is not None:
            self.__children.append(component)

    def remvoeChild(self, component : UIComponent) -> None:
        try:
            self.__children.remove(component)
        except ValueError:
            pass

    def render(self, parent_layout: QVBoxLayout) -> None:
        if self.getVisible():
            parent_layout.addLayout(self.__panou)
            for child in self.__children:
                child.render(self.__panou)