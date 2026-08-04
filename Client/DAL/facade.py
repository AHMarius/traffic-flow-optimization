class DataBaseFacade:
    def __init__(self, repository, dispatcher):
        self.__repository = repository
        self.__dispatcher = dispatcher
        self.__pending = map()
    def submit(self, request, priority):