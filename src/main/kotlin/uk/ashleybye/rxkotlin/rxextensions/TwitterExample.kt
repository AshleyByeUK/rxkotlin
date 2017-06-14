package uk.ashleybye.rxkotlin.rxextensions

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import twitter4j.StallWarning
import twitter4j.Status
import twitter4j.StatusDeletionNotice
import twitter4j.StatusListener
import twitter4j.TwitterStreamFactory
import uk.ashleybye.rxkotlin.helpers.Twitter4JHelper
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
//  doNoSubjectExample()
//  doSubjectExample()
  doConnectableObservableExample()
}

/**
 * Subscribes to a Twitter Status observable and prints the stream of statuses to the standard
 * output until the observer is disposed. This is achieved by calling `dispose()` after
 * sleeping for 10 seconds.
 */
private fun doNoSubjectExample() {
  val disposable = TwitterNoSubject().observe().subscribe(
      { status -> println("Status: {$status)}") },
      { error -> println("Error callback: $error") })

  TimeUnit.SECONDS.sleep(10)
  disposable.dispose()
}

/**
 * Subscribes to a Twitter Status Subject and prints the stream of statuses to the standard
 * output until the observer is disposed. This is achieved by calling `dispose()` after
 * sleeping for 10 seconds.
 */
private fun doSubjectExample() {
  val disposable = TwitterSubject().observe().subscribe(
      { status -> println("Status: {$status)}") },
      { error -> println("Error callback: $error") })

  TimeUnit.SECONDS.sleep(10)
  disposable.dispose()
}

/**
 * Using ConnectedObservable, we can manage multiple observers and share one connection to the
 * upstream observable. The `publish()` method returns a `ConnectedObservable` which is then
 * wrapped with a `refCount()` that keeps track of the number of observers. When the first
 * observer subscribes, the observable begins emitting. Any new subscribers do not use a new
 * connection and the same upstream connection is shared. The reverse is true when disposing of
 * observers. The `share()` method is an alias for `publish().refCount()`.
 *
 * This is demonstrated using two observers. The output showing when connection and disconnection
 * occur, the number of subscriptions and the number of disposals, is added to a list to avoid
 * having to trawl through the emitted output to find them.
 */
private fun doConnectableObservableExample() { // Alias share().
  val output = ArrayList<String>()
  val observable = Observable.create<Status> { emitter ->
    output.add("Establishing connection")
    val twitterStream = TwitterStreamFactory().instance
    val listener = object: StatusListener {
      override fun onStatus(status: Status?) {
        if (emitter.isDisposed) {
          twitterStream.shutdown()
        } else {
          emitter.onNext(status)
        }
      }

      override fun onException(ex: Exception?) {
        if (emitter.isDisposed) {
          twitterStream.shutdown()
        } else {
          emitter.onError(ex)
        }
      }

      override fun onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
        // Not implemented.
      }

      override fun onStallWarning(warning: StallWarning?) {
        // Not implemented.
      }

      override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice?) {
        // Not implemented.
      }

      override fun onScrubGeo(userId: Long, upToStatusId: Long) {
        // Not implemented.
      }
    }

    fun closeStream() {}

    Twitter4JHelper.addStatusListner(twitterStream, listener)
    twitterStream.sample()
    emitter.setCancellable {
      twitterStream::shutdown
      output.add("Disconnecting")
    }
  }

  // Publish() returns ConnectableObservable.
  val lazy = observable.publish().refCount()
  val sub1 = lazy.subscribe()
  output.add("Subscribed 1")
  TimeUnit.SECONDS.sleep(5)
  val sub2 = lazy.subscribe()
  output.add("Subscribed 2")
  TimeUnit.SECONDS.sleep(5)
  sub1.dispose()
  output.add("Disposed 1")
  TimeUnit.SECONDS.sleep(5)
  sub2.dispose()
  output.add("Disposed 2")

  for (string in output) {
    println(string)
  }
}

/**
 * Example of connecting to a stream without management of observers.
 */
private class TwitterNoSubject {
  /**
   * Create a Twitter Status observable.
   *
   * Creates an observable that subscribes to a Twitter stream, which can then be observed. Access to
   * the Twitter API requires authentication details to be configured; here `twitter4j.properties`
   * provides the necessary API keys. See [http://twitter4j.org/en/configuration.html](http://twitter4j.org/en/configuration.html)
   * for alternate options. To stop the stream, `setCancellable` is passed a method reference to
   * `twitterStream::shutdown`; we check to see whether an observer `isDisposed` and call the
   * `twitterStream.shutdown()` method if `true`.
   *
   * For multiple observers, a new network connection is opened for each
   * subscription; to avoid this, we would need to implement a way to only open one connection,
   * keep track of subscribers and start the stream accordingly. This can be done manually, but is
   * error prone; an alternate solution is to use a `Subject`.
   *
   * Note the implementation style for implementing anonymous interfaces. See link below.
   * Note that a Java wrapper is required when adding a `StatusListener` to the `TwitterStream`. This
   * prevents an `IllegalAccessError` being thrown. See link below.
   */
   fun observe() = Observable.create<Status> { emitter ->
    val twitterStream = TwitterStreamFactory().instance
    // See: https://stackoverflow.com/questions/37672023/how-to-create-an-instance-of-anonymous-interface-in-kotlin/37672334
    val listner = object : StatusListener {
      override fun onStatus(status: Status?) {
        if (emitter.isDisposed) {
          twitterStream.shutdown()
        } else {
          emitter.onNext(status)
        }
      }

      override fun onException(ex: Exception?) {
        if (emitter.isDisposed) {
          twitterStream.shutdown()
        } else {
          emitter.onError(ex)
        }
      }

      override fun onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
        // Not implemented.
      }

      override fun onStallWarning(warning: StallWarning?) {
        // Not implemented.
      }

      override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice?) {
        // Not implemented.
      }

      override fun onScrubGeo(userId: Long, upToStatusId: Long) {
        // Not implemented.
      }
    }

    // See: https://stackoverflow.com/questions/44541232/what-is-the-reason-for-twitter4j-streamlistner-illegalaccesserror-in-kotlin
    Twitter4JHelper.addStatusListner(twitterStream, listner)
    twitterStream.sample()

    emitter.setCancellable { twitterStream::shutdown }
  }
}


/**
 * Similar to the example [TwitterNoSubject], but this version uses a Subject to manage subscribers.
 * To keep the implementation simple, the connection to the Twitter API is eager (events will
 * already be flowing when the first subscriber subscribes) and we do not keep track of subscribers.
 */
private class TwitterSubject {
  val subject = PublishSubject.create<Status>()

  init {
    val twitterStream = TwitterStreamFactory().instance
    // See: https://stackoverflow.com/questions/37672023/how-to-create-an-instance-of-anonymous-interface-in-kotlin/37672334
    val listner = object : StatusListener {
      override fun onStatus(status: Status?) {
        subject.onNext(status)
      }

      override fun onException(ex: Exception?) {
        subject.onError(ex)
      }

      override fun onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
        // Not implemented.
      }

      override fun onStallWarning(warning: StallWarning?) {
        // Not implemented.
      }

      override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice?) {
        // Not implemented.
      }

      override fun onScrubGeo(userId: Long, upToStatusId: Long) {
        // Not implemented.
      }
    }

    Twitter4JHelper.addStatusListner(twitterStream, listner)

    twitterStream.sample()
  }

  fun observe(): Observable<Status> = subject
}
