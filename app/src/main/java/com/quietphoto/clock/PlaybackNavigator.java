package com.quietphoto.clock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class PlaybackNavigator {
    static final int NO_ITEM = -1;
    private static final int MAX_HISTORY = 50;

    private final Random random;
    private final List<Integer> deck = new ArrayList<Integer>();
    private final List<Integer> history = new ArrayList<Integer>();
    private int deckPosition;
    private int historyPosition = -1;
    private int itemCount;
    private boolean shuffle = true;

    PlaybackNavigator() {
        this(new Random());
    }

    PlaybackNavigator(Random random) {
        this.random = random;
    }

    void reset(int count) {
        itemCount = Math.max(0, count);
        history.clear();
        historyPosition = -1;
        refillDeck(NO_ITEM);
    }

    void setShuffle(boolean enabled) {
        shuffle = enabled;
    }

    void resetAt(int count, int currentIndex) {
        reset(count);
        if (currentIndex < 0 || currentIndex >= itemCount) return;
        history.add(currentIndex);
        historyPosition = 0;
        deck.remove(Integer.valueOf(currentIndex));
    }

    int next() {
        if (historyPosition + 1 < history.size()) {
            return history.get(++historyPosition);
        }
        if (itemCount == 0) {
            return NO_ITEM;
        }

        int previous = current();
        if (deckPosition >= deck.size()) {
            refillDeck(previous);
        }
        int selected = deck.get(deckPosition++);

        if (history.size() == MAX_HISTORY) {
            history.remove(0);
            historyPosition--;
        }
        history.add(selected);
        historyPosition = history.size() - 1;
        return selected;
    }

    int previous() {
        if (historyPosition <= 0) {
            return NO_ITEM;
        }
        return history.get(--historyPosition);
    }

    int current() {
        if (historyPosition < 0 || historyPosition >= history.size()) {
            return NO_ITEM;
        }
        return history.get(historyPosition);
    }

    private void refillDeck(int avoidFirst) {
        deck.clear();
        for (int i = 0; i < itemCount; i++) {
            deck.add(i);
        }
        if (shuffle) {
            Collections.shuffle(deck, random);
            if (itemCount > 1 && deck.get(0) == avoidFirst) {
                Collections.swap(deck, 0, 1);
            }
        }
        deckPosition = 0;
    }
}
