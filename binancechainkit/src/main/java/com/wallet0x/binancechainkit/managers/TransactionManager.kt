package com.wallet0x.binancechainkit.managers

import com.wallet0x.binancechainkit.core.IStorage
import com.wallet0x.binancechainkit.core.Wallet
import com.wallet0x.binancechainkit.core.api.BinanceChainApi
import com.wallet0x.binancechainkit.models.SyncState
import com.wallet0x.binancechainkit.models.Transaction
import com.wallet0x.binancechainkit.models.TransactionFilterType
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.util.*


class TransactionManager(private val wallet: Wallet,
                         private val storage: IStorage,
                         private val binanceApi: BinanceChainApi
) {

    interface Listener {
        fun onSyncTransactions(transactions: List<Transaction>)
    }

    var listener: Listener? = null

    private val disposables = CompositeDisposable()

    // 3 months duration
    private val windowTime: Long = 88 * 24 * 3600 * 1000L
    private val binanceLaunchTime: Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { set(2019, 0, 1, 0, 0, 0) }.time.time

    fun getTransactions(
        symbol: String,
        filterType: TransactionFilterType? = null,
        fromTransactionHash: String? = null,
        limit: Int? = null
    ): Single<List<Transaction>> {
        var fromAddress: String? = null
        var toAddress: String? = null

        when (filterType) {
            TransactionFilterType.Incoming -> {
                toAddress = wallet.address
            }
            TransactionFilterType.Outgoing -> {
                fromAddress = wallet.address
            }

            else -> {
            }
        }

        return Single.just(storage.getTransactions(
            symbol,
            fromAddress,
            toAddress,
            fromTransactionHash,
            limit
        ))
    }

    fun getTransaction(transactionHash: String): Transaction? {
        return storage.getTransaction(transactionHash)
    }

    fun sync(account: String) {
        val syncedUntilTime = storage.syncState?.transactionSyncedUntilTime ?: binanceLaunchTime

        syncTransactionsPartially(account, syncedUntilTime)
            .subscribeOn(Schedulers.io())
            .subscribe({}, {
                it.printStackTrace()
            })
            .let { disposables.add(it) }
    }

    private fun syncTransactionsPartially(account: String, startTime: Long): Single<Unit> {
        // there is a time delay (usually few seconds) for the new transactions to be included in the response
        // that is why here we left one minute time window
        val currentTime = Date().time - 60_000

        return binanceApi.getTransactions(account, startTime)
            .flatMap {
                val syncedUntil = when {
                    it.size == 1000 -> it.last().blockTime.time
                    else -> Math.min(startTime + windowTime, currentTime)
                }

                storage.addTransactions(it)
                storage.syncState = SyncState(syncedUntil)

                listener?.onSyncTransactions(it)

                if (syncedUntil < currentTime) {
                    syncTransactionsPartially(account, syncedUntil)
                } else {
                    Single.just(Unit)
                }
            }
    }

    fun stop() {
        disposables.dispose()
    }

    fun send(symbol: String, to: String, amount: BigDecimal, memo: String): Single<String> {
        return binanceApi.send(symbol, to, amount, memo, wallet )
    }

}
