from PyQt5.QtWidgets import QPushButton, QVBoxLayout
from .UIComponent import UIComponent

class Button(UIComponent):
    def __init__(self, id : str, visible : bool, label : str):
        super().__init__(id, visible)
        self.__label = label
        self.__buton = QPushButton(self.__label)
        self.__buton.clicked.connect(self.onClick)

    def render(self, parent_layout: QVBoxLayout) -> None:
        if self.getVisible():
            parent_layout.addWidget(self.__buton)
        else:
            self.__buton.hide()

    def onClick(self) -> None:
        print("Button clicked")