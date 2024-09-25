package com.wallet0x.binancechainkit.core


import com.wallet0x.binancechainkit.BinanceChainKit
import com.wallet0x.binancechainkit.core.api.MessageType
import com.wallet0x.binancechainkit.helpers.Crypto
import com.wallet0x.hdwalletkit.ECKey
import com.wallet0x.hdwalletkit.HDWallet
import com.wallet0x.hdwalletkit.Utils

class Wallet(hdWallet: HDWallet, networkType: BinanceChainKit.NetworkType) {

    var sequence: Long = 0
    var accountNumber: Int = 0
    var chainId: String = ""
    private val privateKey = hdWallet.privateKey( accountNumber, 0, 0 )
    val publicKey = ECKey.pubKeyFromPrivKey( privateKey.privKey, true )
    private val publicKeyHash = Utils.sha256Hash160( publicKey )
    val address = Crypto.encodeAddress( networkType.addressPrefix, publicKeyHash)
    private val pubKeyPrefix = MessageType.PubKey.typePrefixBytes
    var pubKeyForSign = byteArrayOf().plus(pubKeyPrefix).plus(33.toByte()).plus(publicKey)

    fun incrementSequence() {
        sequence += 1
    }

    fun sign(message: ByteArray): ByteArray{
        return Crypto.sign(message,this.privateKey.privKey)
    }

}