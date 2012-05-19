package org.oxymore.chronix.core.transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.oxymores.chronix.core.transactional.TranscientBase;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestTransacPersistence extends TestCase {

	public void testPers() {
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("TransacUnit");
		EntityManager entityManager = emf.createEntityManager();

		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();

		TranscientBase tb = new TranscientBase();
		entityManager.persist(tb);
		transaction.commit();

		TranscientBase tb2 = entityManager.find(TranscientBase.class,
				tb.getId());

		Assert.assertEquals(tb.getId(), tb2.getId());
	}
}
