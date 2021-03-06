package social.sunrise.app.database.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sunrisechoir.graphql.PostsQuery
import com.sunrisechoir.graphql.ThreadsSummaryQuery
import com.sunrisechoir.patchql.PatchqlApollo
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.AsyncSubject
import java.util.concurrent.TimeUnit
import social.sunrise.app.dao.Notifications as NotificationsDao


// I don't like this code and it's ok to be mad at me about it.
// Sorry.
// I've had trouble with the notifications queries being waaay to heavy. Patchql needs to expose something more useful for queries.
class NotificationsDaoImpl(private val patchqlApollo: PatchqlApollo) : NotificationsDao {

    // This query is disabled for now.
    override fun getThreadsNotifications(queryBuilder: () -> ThreadsSummaryQuery.Builder): Pair<LiveData<Int>, () -> Unit> {

        val liveNotifications: MutableLiveData<Int> = MutableLiveData(0)
        var disposable: Disposable? = null

        val reset = {}
        // Disabled here.
        val reset_: () -> Unit = {
            liveNotifications.postValue(0)

            val currentCursor: String? = null
            val asyncSubject: Observable<String> = AsyncSubject.create { obs ->
                val query = queryBuilder().before(currentCursor).last(1).build()
                patchqlApollo.query(query) {
                    it.map {
                        it.data() as ThreadsSummaryQuery.Data
                    }.map {
                        it.threads().edges().first().cursor()
                    }.onSuccess {
                        obs.onNext(it.orEmpty())
                    }.onFailure {
                        println("error in getThreadsNotification: $it")
                        //obs.onError(it)
                    }

                }
            }

            if (disposable != null) {
                disposable?.dispose()
            }

            disposable = asyncSubject
                .observeOn(Schedulers.newThread())
                .flatMap { cursor ->
                    //Rx doesn't let us emit a null value :(
                    //So we're going to treat the empty string as null
                    //Ugh this makes me sad
                    val nullableCursor: String? = if (cursor.isEmpty()) null else cursor

                    val query = queryBuilder().after(nullableCursor).first(1000).build()

                    Observable.interval(2000, TimeUnit.MILLISECONDS)
                        .flatMap {
                            AsyncSubject.create<Int> { obs ->
                                patchqlApollo.query(query) {
                                    it.map {
                                        it.data() as ThreadsSummaryQuery.Data
                                    }.map {
                                        it.threads().edges().size
                                    }.onSuccess {
                                        obs.onNext(it)
                                    }.onFailure {
                                        println("error in getThreadsNotification: $it")
                                        if (disposable != null) {
                                            disposable?.dispose()
                                        }
                                    }
                                }
                            }

                        }
                }
                .distinctUntilChanged()
                .doOnError { println("ooop, got an error here: $it") }
                .subscribe { t ->
                    if (t > 99 && disposable != null) {
                        disposable?.dispose()
                    }
                    liveNotifications.postValue(t)
                }

        }
        reset()

        return Pair(liveNotifications, reset)
    }

    override fun getPostsNotifications(queryBuilder: () -> PostsQuery.Builder): Pair<LiveData<Int>, () -> Unit> {
        val liveNotifications: MutableLiveData<Int> = MutableLiveData(0)
        var disposable: Disposable? = null

        liveNotifications.postValue(0)


        val reset: () -> Unit = {
            val currentCursor: String? = null
            val asyncSubject: Observable<String> = AsyncSubject.create { obs ->
                val query = queryBuilder().before(currentCursor).last(1).build()
                patchqlApollo.query(query) {
                    it.map {
                        it.data() as PostsQuery.Data
                    }.mapCatching {
                        it.posts().edges().first().cursor()
                    }.onSuccess {
                        obs.onNext(it!!)
                    }.onFailure {
                        //obs.onNext(null)
                    }

                }
            }

            if (disposable != null)
                disposable?.dispose()

            disposable = asyncSubject
                .observeOn(Schedulers.newThread())
                .flatMap { cursor ->
                    //Rx doesn't let us emit a null value :(
                    //So we're going to treat the empty string as null
                    //Ugh this makes me sad
                    val nullableCursor: String? = if (cursor.isEmpty()) null else cursor

                    val query = queryBuilder().after(nullableCursor).first(
                        99
                    ).build()

                    Observable.interval(5000, TimeUnit.MILLISECONDS)
                        .flatMap {
                            AsyncSubject.create<Int> { obs ->
                                patchqlApollo.query(query) {
                                    it.map {
                                        it.data() as PostsQuery.Data
                                    }.map {
                                        it.posts().edges().size
                                    }.onSuccess {
                                        println("got size ${it}")
                                        obs.onNext(it)
                                    }.onFailure {
                                        println("error in getThreadsNotification: $it")
                                        if (disposable != null) {
                                            disposable?.dispose()
                                        }
                                    }
                                }
                            }
                        }
                }
                .distinctUntilChanged()
                .doOnError { println("ooop, got an error here: $it") }
                .subscribe { t ->
                    if (t > 99 && disposable != null) {
                        disposable?.dispose()
                    }
                    liveNotifications.postValue(t)
                }

        }
        reset()

        return Pair(liveNotifications, reset)
    }
}