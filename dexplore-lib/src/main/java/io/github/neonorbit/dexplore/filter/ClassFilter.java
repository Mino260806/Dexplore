/*
 * Copyright (C) 2022 NeonOrbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.neonorbit.dexplore.filter;

import io.github.neonorbit.dexplore.LazyDecoder;
import io.github.neonorbit.dexplore.util.AbortException;
import io.github.neonorbit.dexplore.util.DexUtils;
import io.github.neonorbit.dexplore.util.Utils;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ClassFilter extends BaseFilter<DexBackedClassDef> {
  private static final int M1 = -1;
  /** A {@code ClassFilter} instance that matches all dex classes. */
  public static final ClassFilter MATCH_ALL = new ClassFilter(builder());

  private final int flag;
  private final int skipFlag;
  private final String superClass;
  private final Pattern pkgPattern;
  private final Set<String> classNames;
  private final List<String> interfaces;

  private ClassFilter(Builder builder) {
    super(builder, Utils.isSingle(builder.classNames));
    this.flag = builder.flag;
    this.skipFlag = builder.skipFlag;
    this.pkgPattern = builder.pkgPattern;
    this.superClass = builder.superClass;
    this.classNames = builder.classNames;
    this.interfaces = builder.interfaces;
  }

  @Override
  public boolean verify(@Nonnull DexBackedClassDef dexClass,
                        @Nonnull LazyDecoder<DexBackedClassDef> decoder) {
    if (this == MATCH_ALL) return true;
    if (classNames != null && !classNames.contains(dexClass.getType())) return false;
    boolean result = (
            (flag == M1 || (dexClass.getAccessFlags() & flag) == flag) &&
            (skipFlag == M1 || (dexClass.getAccessFlags() & skipFlag) == 0) &&
            (superClass == null || superClass.equals(dexClass.getSuperclass())) &&
            (pkgPattern == null || pkgPattern.matcher(dexClass.getType()).matches()) &&
            (interfaces == null || dexClass.getInterfaces().equals(interfaces)) &&
            super.verify(dexClass, decoder)
    );
    if (unique && !result) {
      throw new AbortException("Class found but the filter didn't match");
    }
    return result;
  }

  public static ClassFilter ofClass(@Nonnull String clazz) {
    Objects.requireNonNull(clazz);
    return builder().setClasses(clazz).build();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends BaseFilter.Builder<Builder, ClassFilter> {
    private int flag = M1;
    private int skipFlag = M1;
    private Pattern pkgPattern;
    private String superClass;
    private Set<String> classNames;
    private List<String> interfaces;

    public Builder() {}

    private Builder(ClassFilter instance) {
      super(instance);
      this.flag = instance.flag;
      this.skipFlag = instance.skipFlag;
      this.pkgPattern = instance.pkgPattern;
      this.superClass = instance.superClass;
      this.classNames = instance.classNames;
      this.interfaces = instance.interfaces;
    }

    @Override
    protected boolean isDefault() {
      return super.isDefault()  &&
             flag == M1         &&
             skipFlag == M1     &&
             pkgPattern == null &&
             superClass == null &&
             interfaces == null &&
             classNames == null;
    }

    @Override
    protected Builder getThis() {
      return this;
    }

    @Override
    public ClassFilter build() {
      return isDefault() ? MATCH_ALL : new ClassFilter(this);
    }

    /**
     * Specify a list of packages that should be excluded during the search operation
     * @param packages list of packages to exclude
     * @param exception an exception list to allow sub packages
     *
     * @return this builder
     */
    public Builder skipPackages(@Nullable List<String> packages,
                                @Nullable List<String> exception) {
      if (packages == null || packages.isEmpty()) {
        this.pkgPattern = null;
      } else if (!Utils.isValidName(packages) ||
                 !(exception == null || Utils.isValidName(exception))) {
        throw new InvalidParameterException("Invalid Package Name");
      } else {
        this.pkgPattern = getPackagePattern(packages, exception);
      }
      return this;
    }

    public Builder setClasses(@Nonnull String... classes) {
      List<String> list = DexUtils.javaToDexTypeName(Utils.nonNullList(classes));
      this.classNames = list.isEmpty() ? null : Utils.optimizedSet(list);
      return this;
    }

    /**
     * Set class modifiers. eg: public, static, final etc...
     * @param modifiers see {@link java.lang.reflect.Modifier}
     *
     * @return this builder
     */
    public Builder setModifiers(int modifiers) {
      this.flag = modifiers;
      return this;
    }

    /**
     * Classes with matching modifiers will be skipped
     * @param modifiers see {@link #setModifiers(int)}
     * @see #setModifiers(int)
     *
     * @return this builder
     */
    public Builder skipModifiers(int modifiers) {
      this.skipFlag = modifiers;
      return this;
    }

    public Builder setSuperClass(@Nullable String superClass) {
      this.superClass = superClass == null ? null : DexUtils.javaToDexTypeName(superClass);
      return this;
    }

    public Builder defaultSuperClass() {
      this.superClass = DexUtils.javaClassToDexTypeName(Object.class);
      return this;
    }

    public Builder setInterfaces(@Nullable List<String> iFaces) {
      this.interfaces = iFaces == null ? null : Utils.optimizedList(DexUtils.javaToDexTypeName(iFaces));
      return this;
    }

    public Builder noInterfaces() {
      return setInterfaces(Collections.emptyList());
    }

    private static Pattern getPackagePattern(List<String> packages, List<String> exception) {
      Function<String, String> mapper = s -> 'L' + s.replaceAll("\\.", "/")
                                                    .replaceAll("\\$", "\\\\\\$")
                                           + '/';
      String regex = "^((?!";
      regex += packages.stream().map(mapper).collect(Collectors.joining("|"));
      regex += ")";
      if (exception != null && !exception.isEmpty()) {
        regex += '|' + exception.stream().map(mapper).collect(Collectors.joining("|"));
      }
      regex += ").*$";
      return Pattern.compile(regex);
    }
  }
}
