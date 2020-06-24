package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.network.Network

interface TerminalScreenHandlerInterface {

    val network: Network
    val terminalSlots: MutableList<TerminalSlot>

}