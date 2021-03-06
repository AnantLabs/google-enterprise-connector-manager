// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.instantiator;

import com.google.common.collect.ImmutableMap;
import com.google.enterprise.connector.logging.NDC;
import com.google.enterprise.connector.persist.ConnectorStamps;
import com.google.enterprise.connector.persist.PersistentStore;
import com.google.enterprise.connector.persist.Stamp;
import com.google.enterprise.connector.persist.StoreContext;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Checks for changes in a persistent store. Intended to be run both
 * manually to handle local servlet changes, and periodically to check
 * for remote connector manager changes.
 *
 * @see com.google.enterprise.connector.persist.PersistentStore
 * @see ChangeListener
 */
// TODO: Change StoreContext to String and x.getConnectorName() to x.
class ChangeDetectorImpl implements ChangeDetector {
  private final PersistentStore store;
  private final ChangeListener listener;

  /** The stamps from the previous run. */
  private ImmutableMap<StoreContext, ConnectorStamps> inMemoryInventory =
      ImmutableMap.of();

  /** A sorted set of the keys of {@code inMemoryInventory}. */
  private SortedSet<StoreContext> inMemoryInstances =
      new TreeSet<StoreContext>();

  /**
   * Constructs the detector.
   *
   * @param store the persistent store to look for changes in
   * @param listener the change listener to notify of changes
   */
  ChangeDetectorImpl(PersistentStore store, ChangeListener listener) {
    this.store = store;
    this.listener = listener;
  }

  @Override
  public synchronized void detect() {
    NDC.push("Change");
    try {
      ImmutableMap<StoreContext, ConnectorStamps> persistentInventory =
          store.getInventory();
      SortedSet<StoreContext> persistentInstances =
          new TreeSet<StoreContext>(persistentInventory.keySet());

      // Compare the last known (inMemory) inventory with the new inventory
      // from the persistent store. Notify ChangeListeners of any differences.
      // Save in memory, the new inventory of unchanged items and successfully
      // applied changes.
      inMemoryInventory = compareInventoriesAndNotifyListeners(
          inMemoryInstances.iterator(), persistentInstances.iterator(),
          persistentInventory);
      inMemoryInstances = persistentInstances;

    } finally {
      NDC.pop();
    }
  }

  /**
   * Gets the next element of an {@code Iterator} iterator, or
   * {@code null} if there are no more elements.
   *
   * @return the next element or {@code null}
   */
  private StoreContext getNext(Iterator<StoreContext> it) {
    return it.hasNext() ? it.next() : null;
  }

  /**
   * Iterates over the sorted sets of instance names to find additions
   * and deletions. When matching names are found, compare the version
   * stamps for changes in the individual persisted objects.
   *
   * @param mi the sorted keys to the in-memory instances
   * @param pi the sorted keys to the persistent instances
   * @param persistentInventory the persistent object stamps
   * @return a new inventory of stamps, derived from the
   *         persistentInventory, but reflecting instantiation failures.
   */
  private ImmutableMap<StoreContext, ConnectorStamps>
        compareInventoriesAndNotifyListeners(
        Iterator<StoreContext> mi, Iterator<StoreContext> pi,
        ImmutableMap<StoreContext, ConnectorStamps> persistentInventory) {
    // This map will accumulate items for the new in-memory inventory.
    // Generally, this map will end up being identical to the
    // persistentInventory. However, failed connector instantiations
    // may cause changes to be dropped from this map, so that they may
    // be retried next time around.
    ImmutableMap.Builder<StoreContext, ConnectorStamps> mapBuilder =
        new ImmutableMap.Builder<StoreContext, ConnectorStamps>();

    StoreContext m = getNext(mi);
    StoreContext p = getNext(pi);
    while (m != null && p != null) {
      // Compare instance names.
      int diff = m.getConnectorName().compareTo(p.getConnectorName());
      NDC.pushAppend((diff < 0 ? m : p).getConnectorName());
      try {
        if (diff == 0) {
          // Compare the inMemory vs inPStore ConnectorStamps for a
          // connector instance. Notify ChangeListeners for items whose
          // Stamps have changed.
          ConnectorStamps stamps = compareInstancesAndNotifyListeners(
             m, p, inMemoryInventory.get(m), persistentInventory.get(p));

          // Remember the new ConnetorStamps for our new inMemory inventory.
          mapBuilder.put(p, stamps);

          // Advance to the next connector instance.
          m = getNext(mi);
          p = getNext(pi);
        } else if (diff < 0) {
          listener.connectorRemoved(m.getConnectorName());
          m = getNext(mi);
        } else { // diff > 0
          try {
            listener.connectorAdded(p.getConnectorName(),
                store.getConnectorConfiguration(p));
            mapBuilder.put(p, persistentInventory.get(p));
          } catch (InstantiatorException e) {
            // Forget about this one and retry on the next time around.
            pi.remove();
          }
          p = getNext(pi);
        }
      } finally {
        NDC.pop();
      }
    }
    while (m != null) {
      NDC.pushAppend(m.getConnectorName());
      try {
        listener.connectorRemoved(m.getConnectorName());
      } finally {
        NDC.pop();
      }
      m = getNext(mi);
    }
    while (p != null) {
      NDC.pushAppend(p.getConnectorName());
      try {
        listener.connectorAdded(p.getConnectorName(),
            store.getConnectorConfiguration(p));
        mapBuilder.put(p, persistentInventory.get(p));
      } catch (InstantiatorException e) {
        // Forget about this one and retry on the next time around.
        pi.remove();
      } finally {
        NDC.pop();
      }
      p = getNext(pi);
    }
    return mapBuilder.build();
  }

  /**
   * Compares the version stamps for the given instance.  Notify ChangeListeners
   * of any differences.
   *
   * @param m the key for the in-memory instance
   * @param p the key for the persistent instance
   * @param ms the stamps for the in-memory instance
   * @param ps the stamps for the persistent instance
   * @return possibly modified stamps for the persistent instance
   */
  // TODO: When StoreContext becomes String, we only need one key
  // parameter because we will have m.equals(p). NOTE: This may be
  // false now, if the connector type has changed.
  private ConnectorStamps compareInstancesAndNotifyListeners(
      StoreContext m, StoreContext p, ConnectorStamps ms, ConnectorStamps ps) {

    if (compareStamps(ms.getCheckpointStamp(),
        ps.getCheckpointStamp()) != 0) {
      listener.connectorCheckpointChanged(p.getConnectorName(),
          store.getConnectorState(p));
    }

    if (compareStamps(ms.getScheduleStamp(), ps.getScheduleStamp()) != 0) {
      listener.connectorScheduleChanged(p.getConnectorName(),
          store.getConnectorSchedule(p));
    }

    // Save configuration for last, because it may fail.
    if (compareStamps(ms.getConfigurationStamp(),
        ps.getConfigurationStamp()) != 0) {
      try {
        listener.connectorConfigurationChanged(p.getConnectorName(),
            store.getConnectorConfiguration(p));
      } catch (InstantiatorException e) {
        // Instantiation of the connector failed. Remember a null configuration
        // stamp so we will try the new configuration again next time through.
        // This is an attempt to handle connectors that fail instantiation
        // due to transient causes (such as a server off-line).
        return new ConnectorStamps(ps.getCheckpointStamp(),
            null, ps.getScheduleStamp());
      }
    }

    // Return the original stamps.
    return ps;
  }

  /**
   * Compares two version stamps. Stamps may be {@code null}, in which
   * case they are sorted lower than any non-{@code null} object.
   *
   * @param memoryStamp the stamp for the in-memory object
   * @param persistentStamp the stamp for the persistent object
   * @return a negative integer, zero, or a positive integer as the
   * in-memory stamp is less than, equal to, or greater than the
   * persistent stamp
   * @see java.util.Comparator#compare(Object, Object)
   */
  private int compareStamps(Stamp memoryStamp, Stamp persistentStamp) {
    if (memoryStamp == null && persistentStamp == null) {
      return 0;
    } else if (memoryStamp == null) {
      return -1;
    } else if (persistentStamp == null) {
      return +1;
    } else {
      return memoryStamp.compareTo(persistentStamp);
    }
  }
}
