package mthiessen.misc;

import java.io.Serializable;

public record Pair<K1, K2>(K1 k1, K2 k2) implements Serializable {}
