// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.Ordering;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.EqualsTester;
import com.google.errorprone.annotations.Var;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("MemberName")
public class PathCopyingPersistentTreeMapTest {

  private static final TestStringSortedMapGenerator mapGenerator =
      new TestStringSortedMapGenerator() {

        @Override
        protected SortedMap<String, String> create(Map.Entry<String, String>[] pEntries) {
          @Var PersistentSortedMap<String, String> result = PathCopyingPersistentTreeMap.of();
          for (Map.Entry<String, String> entry : pEntries) {
            result = result.putAndCopy(entry.getKey(), entry.getValue());
          }
          return result;
        }
      };

  public static junit.framework.Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new JUnit4TestAdapter(PathCopyingPersistentTreeMapTest.class));

    suite.addTest(
        NavigableMapTestSuiteBuilder.using(mapGenerator)
            .named("PathCopyingPersistentTreeMap")
            .withFeatures(
                MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionSize.ANY)
            .createTestSuite());

    return suite;
  }

  private @Nullable PersistentSortedMap<String, String> map;

  @Before
  public void setUp() {
    map = PathCopyingPersistentTreeMap.of();
  }

  @After
  public void tearDown() {
    map = null;
  }

  private void put(String key, String value) {
    PersistentSortedMap<String, String> oldMap = map;
    int oldMapSize = oldMap.size();
    String oldMapStr = oldMap.toString();

    map = map.putAndCopy(key, value);
    ((PathCopyingPersistentTreeMap<?, ?>) map).checkAssertions();

    assertThat(oldMap).hasSize(oldMapSize);
    assertThat(oldMap.toString()).isEqualTo(oldMapStr);

    assertThat(map).containsEntry(key, value);

    if (oldMap.containsKey(key)) {
      assertThat(map).hasSize(oldMap.size());

      if (oldMap.get(key).equals(value)) {
        assertThat(map.toString()).isEqualTo(oldMap.toString());
        new EqualsTester().addEqualityGroup(map, oldMap).testEquals();

      } else {
        assertThat(map).isNotEqualTo(oldMap);
      }

    } else {
      assertThat(map).hasSize(oldMap.size() + 1);
      assertThat(map).isNotEqualTo(oldMap);
    }
  }

  private void remove(String key) {
    PersistentSortedMap<String, String> oldMap = map;
    int oldMapSize = oldMap.size();
    String oldMapStr = oldMap.toString();

    map = map.removeAndCopy(key);
    ((PathCopyingPersistentTreeMap<?, ?>) map).checkAssertions();

    assertThat(oldMap).hasSize(oldMapSize);
    assertThat(oldMap.toString()).isEqualTo(oldMapStr);

    assertThat(map.containsKey(key)).isFalse();

    if (oldMap.containsKey(key)) {
      assertThat(map).hasSize(oldMap.size() - 1);
      assertThat(map).isNotEqualTo(oldMap);

    } else {
      assertThat(map).hasSize(oldMap.size());
      assertThat(map.toString()).isEqualTo(oldMap.toString());
      new EqualsTester().addEqualityGroup(map, oldMap).testEquals();
    }
  }

  @Test
  public void testEmpty() {
    assertThat(map.toString()).isEqualTo("{}");
    assertThat(map).isEmpty();
    assertThat(map).hasSize(0);
    assertThat(map.hashCode()).isEqualTo(0);
  }

  private void putABCD() {
    put("a", "1");
    assertThat(map.toString()).isEqualTo("{a=1}");
    assertThat(map.firstKey()).isEqualTo("a");
    assertThat(map.lastKey()).isEqualTo("a");

    put("b", "2");
    assertThat(map.toString()).isEqualTo("{a=1, b=2}");
    assertThat(map.firstKey()).isEqualTo("a");
    assertThat(map.lastKey()).isEqualTo("b");

    put("c", "3");
    assertThat(map.toString()).isEqualTo("{a=1, b=2, c=3}");
    assertThat(map.firstKey()).isEqualTo("a");
    assertThat(map.lastKey()).isEqualTo("c");

    put("d", "4");
    assertThat(map.toString()).isEqualTo("{a=1, b=2, c=3, d=4}");
    assertThat(map.firstKey()).isEqualTo("a");
    assertThat(map.lastKey()).isEqualTo("d");
  }

  private void removeDCBA() {
    remove("d");
    remove("c");
    remove("b");
    remove("a");
  }

  private void putDCBA() {
    put("d", "1");
    assertThat(map.toString()).isEqualTo("{d=1}");
    assertThat(map.firstKey()).isEqualTo("d");
    assertThat(map.lastKey()).isEqualTo("d");

    put("c", "2");
    assertThat(map.toString()).isEqualTo("{c=2, d=1}");
    assertThat(map.firstKey()).isEqualTo("c");
    assertThat(map.lastKey()).isEqualTo("d");

    put("b", "3");
    assertThat(map.toString()).isEqualTo("{b=3, c=2, d=1}");
    assertThat(map.firstKey()).isEqualTo("b");
    assertThat(map.lastKey()).isEqualTo("d");

    put("a", "4");
    assertThat(map.toString()).isEqualTo("{a=4, b=3, c=2, d=1}");
    assertThat(map.firstKey()).isEqualTo("a");
    assertThat(map.lastKey()).isEqualTo("d");
  }

  private void removeABCD() {
    remove("a");
    remove("b");
    remove("c");
    remove("d");
  }

  @Test
  public void testRight() {
    putABCD();
    removeDCBA();
    testEmpty();
  }

  @Test
  public void testLeft() {
    putDCBA();
    removeABCD();
    testEmpty();
  }

  @Test
  public void testRightLeft() {
    putABCD();
    removeABCD();
    testEmpty();
  }

  @Test
  public void testLeftRight() {
    putDCBA();
    removeDCBA();
    testEmpty();
  }

  @Test
  public void testInner() {
    put("a", "1");
    assertThat(map.toString()).isEqualTo("{a=1}");

    put("z", "2");
    assertThat(map.toString()).isEqualTo("{a=1, z=2}");

    put("b", "3");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, z=2}");

    put("y", "4");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, y=4, z=2}");

    put("c", "5");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, c=5, y=4, z=2}");

    put("x", "6");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, c=5, x=6, y=4, z=2}");

    put("d", "7");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, c=5, d=7, x=6, y=4, z=2}");

    put("w", "8");
    assertThat(map.toString()).isEqualTo("{a=1, b=3, c=5, d=7, w=8, x=6, y=4, z=2}");
  }

  @Test
  public void testOuter() {
    put("d", "1");
    assertThat(map.toString()).isEqualTo("{d=1}");

    put("w", "2");
    assertThat(map.toString()).isEqualTo("{d=1, w=2}");

    put("c", "3");
    assertThat(map.toString()).isEqualTo("{c=3, d=1, w=2}");

    put("x", "4");
    assertThat(map.toString()).isEqualTo("{c=3, d=1, w=2, x=4}");

    put("b", "5");
    assertThat(map.toString()).isEqualTo("{b=5, c=3, d=1, w=2, x=4}");

    put("y", "6");
    assertThat(map.toString()).isEqualTo("{b=5, c=3, d=1, w=2, x=4, y=6}");

    put("a", "7");
    assertThat(map.toString()).isEqualTo("{a=7, b=5, c=3, d=1, w=2, x=4, y=6}");

    put("z", "8");
    assertThat(map.toString()).isEqualTo("{a=7, b=5, c=3, d=1, w=2, x=4, y=6, z=8}");
  }

  @Test
  public void testRandom() {
    int iterations = 50;
    Random rnd = new Random(3987432434L); // static seed for reproducibility
    NavigableMap<String, String> comparison = new TreeMap<>();

    // Insert nodes
    for (int i = 0; i < iterations; i++) {
      String key = Integer.toString(rnd.nextInt());
      String value = Integer.toString(rnd.nextInt());

      put(key, value);
      comparison.put(key, value);
      checkEqualTo(comparison);
      checkPartialMaps(comparison, rnd);
    }

    // random put/remove operations
    for (int i = 0; i < iterations; i++) {
      String key = Integer.toString(rnd.nextInt());

      if (rnd.nextBoolean()) {
        String value = Integer.toString(rnd.nextInt());
        put(key, value);
        comparison.put(key, value);
      } else {
        remove(key);
        comparison.remove(key);
      }

      checkEqualTo(comparison);
      checkPartialMaps(comparison, rnd);
    }

    // clear map
    while (!map.isEmpty()) {
      String key = rnd.nextBoolean() ? map.firstKey() : map.lastKey();
      remove(key);
      comparison.remove(key);
      checkEqualTo(comparison);
      checkPartialMaps(comparison, rnd);
    }

    testEmpty();
  }

  private void checkPartialMaps(NavigableMap<String, String> comparison, Random rnd) {
    String key1 = Integer.toString(rnd.nextInt());
    String key2 = Integer.toString(rnd.nextInt());

    checkEqualTo(
        comparison.tailMap(key1, /* inclusive= */ true), map.tailMap(key1, /* pInclusive= */ true));
    checkEqualTo(
        comparison.tailMap(key2, /* inclusive= */ true), map.tailMap(key2, /* pInclusive= */ true));
    checkEqualTo(
        comparison.tailMap(key1, /* inclusive= */ false),
        map.tailMap(key1, /* pInclusive= */ false));
    checkEqualTo(
        comparison.tailMap(key2, /* inclusive= */ false),
        map.tailMap(key2, /* pInclusive= */ false));
    checkEqualTo(
        comparison.headMap(key1, /* inclusive= */ true), map.headMap(key1, /* pInclusive= */ true));
    checkEqualTo(
        comparison.headMap(key2, /* inclusive= */ true), map.headMap(key2, /* pInclusive= */ true));
    checkEqualTo(
        comparison.headMap(key1, /* inclusive= */ false),
        map.headMap(key1, /* pInclusive= */ false));
    checkEqualTo(
        comparison.headMap(key2, /* inclusive= */ false),
        map.headMap(key2, /* pInclusive= */ false));

    String lowKey = Ordering.natural().min(key1, key2);
    String highKey = Ordering.natural().max(key1, key2);
    checkEqualTo(
        comparison.subMap(lowKey, /* pFromInclusive= */ true, highKey, /* pToInclusive= */ true),
        map.subMap(lowKey, /* pFromInclusive= */ true, highKey, /* pToInclusive= */ true));
    checkEqualTo(
        comparison.subMap(lowKey, /* pFromInclusive= */ true, highKey, /* pToInclusive= */ false),
        map.subMap(lowKey, /* pFromInclusive= */ true, highKey, /* pToInclusive= */ false));
    checkEqualTo(
        comparison.subMap(lowKey, /* pFromInclusive= */ false, highKey, /* pToInclusive= */ true),
        map.subMap(lowKey, /* pFromInclusive= */ false, highKey, /* pToInclusive= */ true));
    checkEqualTo(
        comparison.subMap(lowKey, /* pFromInclusive= */ false, highKey, /* pToInclusive= */ false),
        map.subMap(lowKey, /* pFromInclusive= */ false, highKey, /* pToInclusive= */ false));
  }

  private void checkEqualTo(NavigableMap<String, String> comparison) {
    checkEqualTo(comparison, map);
  }

  private static void checkEqualTo(
      NavigableMap<String, String> comparison, NavigableMap<String, String> testMap) {
    assertThat(testMap).isEqualTo(comparison);
    assertThat(testMap.hashCode()).isEqualTo(comparison.hashCode());
    assertWithMessage("isEmpty()").that(testMap.isEmpty()).isEqualTo(comparison.isEmpty());
    assertThat(testMap).hasSize(comparison.size());
    checkEqualTo(comparison.entrySet(), testMap.entrySet());
    checkEqualTo(comparison.keySet(), testMap.keySet());
    checkEqualTo(comparison.values(), testMap.values());
    if (!comparison.isEmpty()) {
      assertWithMessage("firstKey()").that(testMap.firstKey()).isEqualTo(comparison.firstKey());
      assertWithMessage("lastKey()").that(testMap.lastKey()).isEqualTo(comparison.lastKey());
    }
  }

  private static <T> void checkEqualTo(Set<T> comparison, Set<T> set) {
    assertThat(set).isEqualTo(comparison);
    assertThat(set.hashCode()).isEqualTo(comparison.hashCode());
    checkEqualTo((Collection<T>) comparison, (Collection<T>) set);
  }

  private static <T> void checkEqualTo(Collection<T> comparison, Collection<T> set) {
    // equals() and hashCode() is undefined for Collections
    assertWithMessage("isEmpty()").that(set.isEmpty()).isEqualTo(comparison.isEmpty());
    assertThat(set).hasSize(comparison.size());
    assertThat(set).containsExactlyElementsIn(comparison).inOrder();
  }

  @Test
  public void testSubmapSubmap() {
    map = map.putAndCopy("a", "a").putAndCopy("b", "b").putAndCopy("c", "c");

    NavigableMap<String, String> submap = map.subMap("aa", "c");
    assertThat(submap).containsExactly("b", "b");

    // The bounds of further submap calls may be at most those of the original call
    @Var NavigableMap<String, String> subsubmap = submap.subMap("aaa", true, "c", false);
    assertThat(subsubmap).containsExactly("b", "b");

    subsubmap = submap.subMap("aa", /* fromInclusive= */ true, "bb", /* toInclusive= */ false);
    assertThat(subsubmap).containsExactly("b", "b");

    subsubmap = submap.subMap("aaa", /* fromInclusive= */ true, "bb", /* toInclusive= */ false);
    assertThat(subsubmap).containsExactly("b", "b");

    assertThrows(IllegalArgumentException.class, () -> submap.subMap("a", "c"));
    assertThrows(IllegalArgumentException.class, () -> submap.subMap("aa", "d"));
    assertThrows(IllegalArgumentException.class, () -> submap.subMap("a", "d"));
  }

  @Test
  public void testEntrySetContains() {
    PersistentMap<String, Integer> first =
        PathCopyingPersistentTreeMap.<String, Integer>of().putAndCopy("c", 3).putAndCopy("d", 4);
    PersistentMap<String, Integer> second =
        PathCopyingPersistentTreeMap.<String, Integer>of().putAndCopy("b", 2).putAndCopy("c", 3);

    // Here we want to test the containsAll method, does we call it explicitly
    // instead of letting Truth check containment (which is not guaranteed to call containsAll).
    assertThat(second.entrySet().containsAll(first.entrySet())).isFalse();
    assertThat(first.entrySet().containsAll(second.entrySet())).isFalse();
  }
}
