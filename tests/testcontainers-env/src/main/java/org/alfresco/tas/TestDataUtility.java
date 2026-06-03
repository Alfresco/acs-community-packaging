package org.alfresco.tas;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** A class providing utility methods for e2e testing. */
public class TestDataUtility
{
    private static final Random RANDOM = new Random();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Generate a random 'word' containing 16 alphabetic characters. */
    public static String getAlphabeticUUID()
    {
        return IntStream.range(0, 16)
                .map(i -> ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())))
                .mapToObj(Character::toString)
                .collect(Collectors.joining());
    }
}
