package org.oxymore.chronix.core.transactional;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.EnvironmentValue;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
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
	
	public void testPers2() {
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("TransacUnit");
		EntityManager entityManager = emf.createEntityManager();

		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();

		CalendarPointer cp1 = new CalendarPointer();
		cp1.setCalendarID(UUID.randomUUID().toString());
		entityManager.persist(cp1);
		transaction.commit();

		CalendarPointer cp2 = entityManager.find(CalendarPointer.class,
				cp1.getId());

		Assert.assertEquals(cp1.getId(), cp2.getId());
		Assert.assertEquals(cp1.getCalendarID(), cp2.getCalendarID());
	}
	
	public void testPers3() {
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("TransacUnit");
		EntityManager entityManager = emf.createEntityManager();

		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();

		EnvironmentValue e1 = new EnvironmentValue();
		e1.setKey("VARNAME");
		e1.setValue("VALUE");
		entityManager.persist(e1);
		transaction.commit();

		EnvironmentValue e2 = entityManager.find(EnvironmentValue.class,
				e1.getId());

		Assert.assertEquals(e1.getId(), e2.getId());
		Assert.assertEquals(e1.getKey(), e2.getKey());
	}
	
	public void testPers4() {
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("TransacUnit");
		EntityManager entityManager = emf.createEntityManager();

		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();

		Event e1 = new Event();
		e1.setLevel0IdU(UUID.randomUUID());
		entityManager.persist(e1);
		transaction.commit();

		Event e2 = entityManager.find(Event.class,
				e1.getId());

		Assert.assertEquals(e1.getId(), e2.getId());
		Assert.assertEquals(e1.getLevel0IdU(), e2.getLevel0IdU());
	}
	
	public void testPers5() {
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("TransacUnit");
		EntityManager entityManager = emf.createEntityManager();

		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();

		PipelineJob e1 = new PipelineJob();
		e1.setLevel0IdU(UUID.randomUUID());
		entityManager.persist(e1);
		transaction.commit();

		TranscientBase e2 = entityManager.find(TranscientBase.class,
				e1.getId());

		Assert.assertEquals(e1.getId(), e2.getId());
		//Assert.assertEquals(e1.getLevel0IdU(), e2.getLevel0IdU());
	}
}
