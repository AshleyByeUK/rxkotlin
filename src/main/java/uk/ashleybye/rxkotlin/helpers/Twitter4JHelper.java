package uk.ashleybye.rxkotlin.helpers;

import twitter4j.StatusListener;
import twitter4j.TwitterStream;

public class Twitter4JHelper {
  public static void addStatusListner(TwitterStream stream, StatusListener listner) {
    stream.addListener(listner);
  }
}
