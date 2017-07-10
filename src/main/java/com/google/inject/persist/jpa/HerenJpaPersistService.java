/**
 * Copyright (C) 2010 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.persist.jpa;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.heren.i0.core.internal.util.ClassScanner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.lang.annotation.*;
import java.util.Map;
import java.util.Set;

import static com.heren.i0.core.internal.util.TypePredicates.isEntity;

/**
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
@Singleton
class HerenJpaPersistService implements Provider<EntityManager>, UnitOfWork, PersistService {
  private final ThreadLocal<EntityManager> entityManager = new ThreadLocal<EntityManager>();

  private final String persistenceUnitName;
  private final Map<?,?> persistenceProperties;
  private final String[] packages;

  @Inject
  public HerenJpaPersistService(@Jpa String persistenceUnitName,
                                @Nullable @Jpa Map<?,?> persistenceProperties, @Jpa String[] packages) {
    this.persistenceUnitName = persistenceUnitName;
    this.persistenceProperties = persistenceProperties;
    this.packages = packages;
  }

  public EntityManager get() {
    if (!isWorking()) {
      begin();
    }

    EntityManager em = entityManager.get();
    Preconditions.checkState(null != em, "Requested EntityManager outside work unit. "
        + "Try calling UnitOfWork.begin() first, or use a PersistFilter if you "
        + "are inside a servlet environment.");

    return em;
  }

  public boolean isWorking() {
    return entityManager.get() != null;
  }

  public void begin() {
    Preconditions.checkState(null == entityManager.get(),
        "Work already begun on this thread. Looks like you have called UnitOfWork.begin() twice"
         + " without a balancing call to end() in between.");

    entityManager.set(emFactory.createEntityManager());
  }

  public void end() {
    EntityManager em = entityManager.get();

    // Let's not penalize users for calling end() multiple times.
    if (null == em) {
      return;
    }

    try {
      em.close();
    }
    finally {
      entityManager.remove();
    }
  }

  private volatile EntityManagerFactory emFactory;

  @VisibleForTesting
  synchronized void start(EntityManagerFactory emFactory) {
    this.emFactory = emFactory;
  }

  public synchronized void start() {
    Preconditions.checkState(null == emFactory, "HerenPersistence service was already initialized.");

    HerenMutablePersistenceUnitInfo persistenceUnitInfo = new HerenMutablePersistenceUnitInfo();
    persistenceUnitInfo.setPersistenceUnitName(persistenceUnitName);
    scanEntityClasses(persistenceUnitInfo, packages);

    if (null != persistenceProperties) {
      this.emFactory = HerenPersistence
          .createEntityManagerFactory(persistenceUnitInfo, persistenceProperties);
    } else {
      this.emFactory = HerenPersistence.createEntityManagerFactory(persistenceUnitInfo);
    }
  }

  public synchronized void stop() {
    Preconditions.checkState(emFactory.isOpen(), "HerenPersistence service was already shut down.");
    emFactory.close();
  }

  @Singleton
  public static class EntityManagerFactoryProvider implements Provider<EntityManagerFactory> {
    private final HerenJpaPersistService emProvider;

    @Inject
    public EntityManagerFactoryProvider(HerenJpaPersistService emProvider) {
      this.emProvider = emProvider;
    }

    public EntityManagerFactory get() {
      assert null != emProvider.emFactory;
      return emProvider.emFactory;
    }
  }
  
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  private @interface Nullable { }

  private void scanEntityClasses(HerenMutablePersistenceUnitInfo persistenceUnitInfo, String... packages) {
    ClassScanner scanner = new ClassScanner(packages);
    Set<Class<?>> entityClasses = scanner.findBy(isEntity);
    for (Class<?> entity : entityClasses) {
      persistenceUnitInfo.addManagedClassName(entity.getName());
    }
  }
}
