// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingNavigableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An {@link OrderStatisticSet} with naive implementations of its functions.
 *
 * <p>The class wraps a {@link NavigableSet} object and delegates all methods inherited from the
 * <code>NavigableSet</code> interface to that. For the methods particular to the <code>
 * OrderStatisticSet</code> interface, it provides naive implementations that guarantee performance
 * only in O(n).
 *
 * @param <E> type of the elements of this set. See the Javadoc of {@link OrderStatisticSet} for
 *     possible constraints on this type
 * @see OrderStatisticSet
 */
final class NaiveOrderStatisticSet<E> extends ForwardingNavigableSet<E>
    implements OrderStatisticSet<E>, Serializable {

  private static final long serialVersionUID = -1941093176613766876L;

  @SuppressWarnings("serial") // This class only needs to be serializable if delegate is.
  private final NavigableSet<E> delegate;

  private NaiveOrderStatisticSet(NavigableSet<E> pDelegate) {
    delegate = pDelegate;
  }

  /** Creates a new empty OrderStatisticSet using natural ordering. */
  static <E> NaiveOrderStatisticSet<E> createSet() {
    return new NaiveOrderStatisticSet<>(new TreeSet<>());
  }

  /** Creates a new empty OrderStatisticSet using the given comparator. */
  static <E> NaiveOrderStatisticSet<E> createSet(Comparator<? super E> pComparator) {
    return new NaiveOrderStatisticSet<>(new TreeSet<>(checkNotNull(pComparator)));
  }

  /**
   * Creates a new OrderStatisticSet containing the same elements as the given Iterable, using
   * natural ordering.
   */
  static <E> NaiveOrderStatisticSet<E> createSetWithNaturalOrder(Iterable<E> pSet) {
    NavigableSet<E> delegate = new TreeSet<>();
    Iterables.addAll(delegate, pSet);
    return new NaiveOrderStatisticSet<>(delegate);
  }

  /**
   * Creates a new OrderStatisticSet containing the same elements and using the same order as the
   * given {@link SortedSet}.
   *
   * @param pSortedSet set to use elements and ordering of
   * @param <E> type of the elements of the given and new set
   * @return a new OrderStatisticSet containing the same elements and using the same order as the
   *     given set
   */
  static <E> NaiveOrderStatisticSet<E> createSetWithSameOrder(SortedSet<E> pSortedSet) {
    return new NaiveOrderStatisticSet<>(new TreeSet<>(checkNotNull(pSortedSet)));
  }

  /**
   * Creates a new OrderStatisticSet that is backed by the given {@link NavigableSet}. Any change to
   * the given navigable set will be reflected by the returned OrderStatisticSet, and any change to
   * the OrderStatisticSet will be reflected by the navigable set.
   *
   * @param pNavigableSet backing navigable set
   * @param <E> type of the elements of the given set
   * @return a new OrderStatisticSet view on the given navigable set
   */
  static <E> NaiveOrderStatisticSet<E> createView(NavigableSet<E> pNavigableSet) {
    return new NaiveOrderStatisticSet<>(checkNotNull(pNavigableSet));
  }

  @Override
  protected NavigableSet<E> delegate() {
    return delegate;
  }

  @Override
  public E getByRank(int pIndex) {
    return Iterables.get(delegate, pIndex);
  }

  @Override
  @CanIgnoreReturnValue
  public E removeByRank(int pIndex) {
    E elem = getByRank(pIndex);
    Preconditions.checkState(delegate.remove(elem), "Element could be retrieved, but not deleted");
    return elem;
  }

  @Override
  public int rankOf(E pObj) {
    checkNotNull(pObj);
    return Iterables.indexOf(delegate, o -> compare(o, pObj) == 0);
  }

  @SuppressWarnings("unchecked")
  private int compare(E pO1, E pO2) {
    Comparator<? super E> comparator = comparator();
    if (comparator != null) {
      return comparator.compare(pO1, pO2);
    } else {
      return ((Comparable<E>) pO1).compareTo(pO2);
    }
  }

  @Override
  public OrderStatisticSet<E> descendingSet() {
    return new NaiveOrderStatisticSet<>(super.descendingSet());
  }

  @Override
  public OrderStatisticSet<E> subSet(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
    return new NaiveOrderStatisticSet<>(
        super.subSet(fromElement, fromInclusive, toElement, toInclusive));
  }

  @Override
  public OrderStatisticSet<E> headSet(E toElement, boolean inclusive) {
    return new NaiveOrderStatisticSet<>(super.headSet(toElement, inclusive));
  }

  @Override
  public OrderStatisticSet<E> tailSet(E fromElement, boolean inclusive) {
    return new NaiveOrderStatisticSet<>(super.tailSet(fromElement, inclusive));
  }

  @Override
  public OrderStatisticSet<E> headSet(E toElement) {
    return headSet(toElement, /* inclusive= */ false);
  }

  @Override
  public OrderStatisticSet<E> subSet(E fromElement, E toElement) {
    return subSet(fromElement, /* fromInclusive= */ true, toElement, /* toInclusive= */ false);
  }

  @Override
  public OrderStatisticSet<E> tailSet(E fromElement) {
    return tailSet(fromElement, /* inclusive= */ true);
  }
}
