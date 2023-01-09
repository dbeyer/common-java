// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.Var;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * This is a {@link URLClassLoader} that behaves like a normal class loader except that it loads
 * some classes always by itself, even if the parent class loader would also have been available to
 * load them.
 *
 * <p>Normal class loaders follow the parent-first strategy, so they never load classes which their
 * parent could also load. This class loader follows the child-first strategy for a specific set of
 * classes (given by a pattern) and the parent-first strategy for the rest.
 *
 * <p>This class loader can be used if you want to load a component with its own class loader (so
 * that it can be garbage collected independently, for example), but the parent class loader also
 * sees the classes.
 *
 * @deprecated replaced with {@link Classes#makeExtendedURLClassLoader()} and {@link
 *     Classes.ClassLoaderBuilder#setDirectLoadClasses(Predicate)}.
 */
@Deprecated
@SuppressWarnings("AvoidObjectArrays") // deprecated class, usage inherited from super class
public class ChildFirstPatternClassLoader extends URLClassLoader {

  private final Predicate<String> loadInChild;

  /**
   * Create a new class loader.
   *
   * @param pLoadInChild The predicate telling which classes should never be loaded by the parent.
   * @param pUrls The sources where this class loader should load classes from.
   * @param pParent The parent class loader.
   */
  public ChildFirstPatternClassLoader(
      Predicate<String> pLoadInChild, URL[] pUrls, ClassLoader pParent) {
    super(pUrls, checkNotNull(pParent));
    loadInChild = checkNotNull(pLoadInChild);
  }

  /**
   * Create a new class loader.
   *
   * @param pClassPattern The pattern telling which classes should never be loaded by the parent.
   * @param pUrls The sources where this class loader should load classes from.
   * @param pParent The parent class loader.
   */
  public ChildFirstPatternClassLoader(Pattern pClassPattern, URL[] pUrls, ClassLoader pParent) {
    super(pUrls, checkNotNull(pParent));
    checkNotNull(pClassPattern);
    loadInChild = s -> pClassPattern.matcher(s).matches();
  }

  @Override
  protected Class<?> loadClass(String name, boolean pResolve) throws ClassNotFoundException {
    if (!loadInChild.test(name)) {
      return super.loadClass(name, pResolve);
    }

    // This is the same code as in {@link URLClassLoader#loadClass(String, boolean)
    // except that it never asks the parent class loader
    synchronized (getClassLoadingLock(name)) {
      // First, check if the class has already been loaded
      @Var Class<?> c = findLoadedClass(name);
      if (c == null) {
        // If still not found, then invoke findClass in order
        // to find the class.
        c = findClass(name);
      }
      if (pResolve) {
        resolveClass(c);
      }
      return c;
    }
  }
}
