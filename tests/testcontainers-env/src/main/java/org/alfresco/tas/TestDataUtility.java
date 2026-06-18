/*
 * #%L
 * Alfresco Testcontainers Environment
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
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
