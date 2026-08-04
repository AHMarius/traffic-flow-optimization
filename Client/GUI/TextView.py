from PyQt5.QtWidgets import QLabel, QVBoxLayout
from .UIComponent import UIComponent

class TextView(UIComponent):
    def __init__(self, id : str, visible : bool, text : str):
        super().__init__(id, visible)
        self.__text = text
        self.__label_widget = QLabel(self.__text)

    def setText(self, text: str) -> None:
            self.__text = text
            self.__label_widget.setText(self.__text)

    def render(self, parent_layout: QVBoxLayout) -> None:
        if self.getVisible():
            parent_layout.addWidget(self.__label_widget)
        else:
            self.__label_widget.hide()