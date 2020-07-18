package jp.ac.titech.itpro.sdl.circulardotter

interface Receiver<Message> {
    fun receive(message: Message)
}

