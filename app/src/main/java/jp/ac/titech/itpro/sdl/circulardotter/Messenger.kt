package jp.ac.titech.itpro.sdl.circulardotter

interface Receiver<Message> {
    fun receive(message: Message)
}

interface Sender<R : Receiver<Message>, Message> {
    fun send(receiver: R)
}