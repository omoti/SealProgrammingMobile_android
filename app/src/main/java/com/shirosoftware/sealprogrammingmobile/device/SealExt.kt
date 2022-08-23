package com.shirosoftware.sealprogrammingmobile.device

import com.shirosoftware.sealprogrammingmobile.domain.Seal

fun Seal.toSerialCommand(): Char {
    return when (this) {
        Seal.Forward -> SealCommand.forward
        Seal.Back -> SealCommand.back
        Seal.Left -> SealCommand.left
        Seal.Right -> SealCommand.right
        Seal.Stop -> SealCommand.stop
        Seal.Light -> SealCommand.light
        Seal.Horn -> SealCommand.horn
    }
}
