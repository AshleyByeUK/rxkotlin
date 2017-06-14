package uk.ashleybye.rxkotlin.rxextensions

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import twitter4j.StallWarning
import twitter4j.Status
import twitter4j.StatusDeletionNotice
import twitter4j.StatusListener
import twitter4j.TwitterStreamFactory
import uk.ashleybye.rxkotlin.helpers.Twitter4JHelper

fun main(args: Array<String>) {
  doNoSubjectExample()
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
