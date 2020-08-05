package org.mineacademy.fo.collection;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.mineacademy.fo.Valid;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Utility class for quick access methods regarding "Strict Collections"
 *
 * @see StrictCollection
 * @see StrictDataHolder
 */
@UtilityClass
public class StrictCollections {

  @Getter(AccessLevel.PUBLIC) // Messages for collections
  private String defaultCollectionCannotRemove = "Cannot remove '%s' as it is not in the collection!",
          defaultCollectionCannotAdd = "Value '%s' is already in the collection!";

  @Getter(AccessLevel.PUBLIC) // Messages for maps
  private String defaultMapCannotRemove = "Cannot remove '%s' as it is not in the map!",
          defaultMapCannotAdd = "Key '%s' is already in the map --> '%s'";

  public void setDefaultCollectionCannotAdd(String defaultCannotAdd) {
    Valid.checkBoolean(!Valid.isNullOrEmpty(defaultCannotAdd), "Message cannot be empty or null!");
    defaultCollectionCannotAdd = defaultCannotAdd;
  }

  public void setDefaultCollectionCannotRemove(String defaultCannotRemove) {
    Valid.checkBoolean(!Valid.isNullOrEmpty(defaultCannotRemove), "Message cannot be empty or null!");
    defaultCollectionCannotRemove = defaultCannotRemove;
  }
  
  public void setDefaultMapCannotAdd(String defaultCannotAdd) {
    Valid.checkBoolean(!Valid.isNullOrEmpty(defaultCannotAdd), "Message cannot be empty or null!");
    defaultMapCannotAdd = defaultCannotAdd;
  }

  public void setDefaultMapCannotRemove(String defaultCannotRemove) {
    Valid.checkBoolean(!Valid.isNullOrEmpty(defaultCannotRemove), "Message cannot be empty or null!");
    defaultMapCannotRemove = defaultCannotRemove;
  }

  /**
   * Returns a StrictCollection backed by the given collection. Care should be taken that the
   * * backing map is not exposed since strict operations cannot be enforced on
   * * actions taken by the backing collection.
   *
   * @param collection The not-null collection which the StrictCollection will be backed by.
   * @param <E>        The generic type.
   * @return Returns a StrictCollection backed by the given collection.
   */
  public <E> StrictCollection<E> of(Collection<E> collection) {
    return new StrictCollectionImpl<>(collection);
  }

  /**
   * Returns a StrictMap backed by the given map. Care should be taken that the
   * backing map is not exposed since strict operations cannot be enforced on
   * actions taken by the backing map.
   *
   * @param collection The not-null map which the StrictMap will be backed by.
   * @param <K>        The generic type for the Key.
   * @param <V>        The generic type for the Values.
   * @return Returns a StrictMap backed by the given map.
   */
  public <K, V> StrictMap<K, V> of(Map<K, V> collection) {
    return new StrictMapImpl<>(collection);
  }

  @SafeVarargs
  public <E> boolean strictAddAll(Collection<E> collection, E... elements) {
    if (collection instanceof StrictCollection) {
      return collection.addAll(Arrays.asList(elements));
    }
    boolean modified = false;
    for (E e : elements) {
      Valid.checkBoolean(!collection.contains(e), defaultCollectionCannotAdd);
      if (!modified) {
        modified = collection.add(e);
      }
    }
    return modified;
  }

  public <E> boolean strictAddAll(Collection<E> collection, Iterable<E> elements) {
    if (collection instanceof StrictCollection) {
      return ((StrictCollection<E>) collection).addAll0(elements);
    }
    boolean modified = false;
    for (E e : elements) {
      Valid.checkBoolean(!collection.contains(e), defaultCollectionCannotAdd);
      if (!modified) {
        modified = collection.add(e);
      }
    }
    return modified;
  }

  @SafeVarargs
  public <E> boolean strictRemoveAll(Collection<E> collection, E... elements) {
    if (collection instanceof StrictCollection) {
      return collection.removeAll(Arrays.asList(elements));
    }
    boolean modified = false;
    for (E e : elements) {
      Valid.checkBoolean(collection.contains(e), defaultCollectionCannotRemove);
      if (!modified) {
        modified = collection.remove(e);
      }
    }
    return modified;
  }

  public <E> boolean strictRemoveAll(Collection<E> collection, Iterable<E> elements) {
    if (collection instanceof StrictCollection) {
      return ((StrictCollection<E>) collection).removeAll0(elements);
    }
    boolean modified = false;
    for (E e : elements) {
      Valid.checkBoolean(collection.contains(e), defaultCollectionCannotAdd);
      if (!modified) {
        modified = collection.remove(e);
      }
    }
    return modified;
  }

  public <K, V> void strictPutAll(Map<K, V> map, Map<K, V> toPut) {
    if (map instanceof StrictMap) {
      map.putAll(toPut);
      return;
    }
    for (Map.Entry<K, V> entry : toPut.entrySet()) {
      Valid.checkBoolean(!map.containsKey(entry.getKey()), String.format(defaultMapCannotAdd, entry.getKey()));
      map.put(entry.getKey(), entry.getValue());
    }
  }

}
