/*
 * Copyright (c) 2008, 2009, 2011 Oracle, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.  The Eclipse Public License is available
 * at http://www.eclipse.org/legal/epl-v10.html and the Eclipse Distribution License
 * is available at http://www.eclipse.org/org/documents/edl-v10.php.
 */
package com.google.inject.persist.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUtil;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bootstrap class that provides access to an EntityManagerFactory.
 */
public class HerenPersistence {

	@Deprecated
	public static final String PERSISTENCE_PROVIDER = "javax.persistence.spi.PeristenceProvider";

	@Deprecated
	protected static final Set<PersistenceProvider> providers = new HashSet<PersistenceProvider>();

	/**
	 * Create and return an EntityManagerFactory for the named persistence unit.
	 *
	 * @param persistenceUnitInfo The name of the persistence unit
	 *
	 * @return The factory that creates EntityManagers configured according to the specified persistence unit
	 */
	public static EntityManagerFactory createEntityManagerFactory(PersistenceUnitInfo persistenceUnitInfo) {
		return createEntityManagerFactory( persistenceUnitInfo, null );
	}

	/**
	 * Create and return an EntityManagerFactory for the named persistence unit using the given properties.
	 *
	 * @param persistenceUnitInfo The name of the persistence unit
	 * @param properties Additional properties to use when creating the factory. The values of these properties override
	 * any values that may have been configured elsewhere
	 *
	 * @return The factory that creates EntityManagers configured according to the specified persistence unit
	 */
	public static EntityManagerFactory createEntityManagerFactory(PersistenceUnitInfo persistenceUnitInfo, Map properties) {
		EntityManagerFactory emf = null;
		List<PersistenceProvider> providers = getProviders();
		for ( PersistenceProvider provider : providers ) {
			emf = provider.createContainerEntityManagerFactory( persistenceUnitInfo, properties );
			if ( emf != null ) {
				break;
			}
		}
		if ( emf == null ) {
			throw new PersistenceException( "No Persistence provider for EntityManager" );
		}
		return emf;
	}

	private static List<PersistenceProvider> getProviders() {
		return PersistenceProviderResolverHolder
				.getPersistenceProviderResolver()
				.getPersistenceProviders();
	}

	/**
	 * Create database schemas and/or tables and/or create DDL scripts as determined by the supplied properties
	 *
	 * Called when schema generation is to occur as a separate phase from creation of the entity manager factory.
	 *
	 * @param persistenceUnitName the name of the persistence unit
	 * @param properties properties for schema generation; these may also contain provider-specific properties. The
	 * values of these properties override any values that may have been configured elsewhere.
	 *
	 * @throws PersistenceException if insufficient or inconsistent configuration information is provided or if schema
	 * generation otherwise fails.
	 */
	public static void generateSchema(String persistenceUnitName, Map properties) {
		List<PersistenceProvider> providers = getProviders();
		for ( PersistenceProvider provider : providers ) {
			final boolean generated = provider.generateSchema( persistenceUnitName, properties );
			if ( generated ) {
				return;
			}
		}

		throw new PersistenceException(
				"No persistence provider found for schema generation for persistence-unit named " + persistenceUnitName
		);
	}

	/**
	 * @return Returns a <code>PersistenceUtil</code> instance.
	 */
	public static PersistenceUtil getPersistenceUtil() {
		return util;
	}

	private static PersistenceUtil util =
			//TODO add an Hibernate specific optimization
		new PersistenceUtil() {
			public boolean isLoaded(Object entity, String attributeName) {
				List<PersistenceProvider> providers = HerenPersistence.getProviders();
				for ( PersistenceProvider provider : providers ) {
					final LoadState state = provider.getProviderUtil().isLoadedWithoutReference( entity, attributeName );
					if ( state == LoadState.UNKNOWN ) continue;
					return state == LoadState.LOADED;
				}
				for ( PersistenceProvider provider : providers ) {
					final LoadState state = provider.getProviderUtil().isLoadedWithReference( entity, attributeName );
					if ( state == LoadState.UNKNOWN ) continue;
					return state == LoadState.LOADED;
				}
				return true;
			}

			public boolean isLoaded(Object object) {
				List<PersistenceProvider> providers = HerenPersistence.getProviders();
				for ( PersistenceProvider provider : providers ) {
					final LoadState state = provider.getProviderUtil().isLoaded( object );
					if ( state == LoadState.UNKNOWN ) continue;
					return state == LoadState.LOADED;
				}
				return true;
			}
		};
}
