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

package io.github.neonorbit.dexplore;

import io.github.neonorbit.dexplore.util.DexException;
import io.github.neonorbit.dexplore.util.DexLog;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;

public final class DexEntry implements Comparable<DexEntry> {
  private final String dexName;
  private final DexContainer container;
  private       DexBackedDexFile dexFile;

  DexEntry(DexContainer container, String dexName) {
    this.dexName = dexName;
    this.container = container;
  }

  @Nonnull
  public String getDexName() {
    return this.dexName;
  }

  /**
   * @return the dex file associated with this entry
   * @throws DexException if fails to load the dex file
   */
  @Nonnull
  public DexBackedDexFile getDexFile() {
    if (this.dexFile == null) {
      try {
        DexLog.d("Loading Dex: " + this.dexName);
        this.dexFile = container.loadDexFile(this.dexName);
      } catch (IOException e) {
        throw new DexException("Failed to load dex file: " + this.dexName, e);
      }
    }
    return this.dexFile;
  }

  public boolean matches(String dexName) {
    return this.dexName.equals(dexName);
  }

  @Override
  public int compareTo(@Nonnull DexEntry o) {
    int compare = this.dexName.compareTo(o.dexName);
    if (compare != 0) return compare;
    return this.container.getPath().compareTo(o.container.getPath());
  }

  @Override
  public int hashCode() {
    return Objects.hash(dexName, container.getPath());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj instanceof DexEntry) {
      DexEntry another = (DexEntry) obj;
      return this.dexName.equals(another.dexName) &&
             this.container.getPath().equals(another.container.getPath());
    }
    return false;
  }

  @Override
  public String toString() {
    return container.getPath() + ":" + dexName;
  }
}
