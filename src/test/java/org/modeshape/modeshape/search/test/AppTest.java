package org.modeshape.modeshape.search.test;

import java.net.URL;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

public class AppTest {
    private Repository repository;
    private ModeShapeEngine engine;

    @Before
    public void init() throws Exception {
        engine = new ModeShapeEngine();
        engine.start();

        URL url = AppTest.class.getClassLoader().getResource("repository.json");

        RepositoryConfiguration config = RepositoryConfiguration.read(url);
        Problems problems = config.validate();
        if (problems.hasErrors()) {
            System.err.println("Problems starting the engine.");
            System.err.println(problems);
            throw new RuntimeException("init error" + problems);
        }

        repository = engine.deploy(config);
        String repositoryName = config.getName();

        repository = engine.getRepository(repositoryName);

        initData();
    }

    @After
    public void after() throws Exception {
        engine.shutdown().get();
    }

    @Test
    public void testSearchWithJoinOr() throws Exception {
        String sql = "SELECT * "
                + "FROM [test:Parent] as p "
                + "LEFT OUTER JOIN [test:Child] as c ON ISCHILDNODE(c, p) "
                + "WHERE "
                + " contains(p.*, 'name2') or contains(c.*, 'name2')";

        Session session = repository.login();

        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(sql, Query.JCR_SQL2);
            QueryResult result = q.execute();
            RowIterator rit = result.getRows();
            int count = 0;
            while(rit.hasNext()) {
                Row r = rit.nextRow();
                System.out.println(">>" + r.getNode("p"));
                count++;
            }
            // expected rows:
            //    /p1 {jcr:primaryType=test:Parent, test:parentName=name1} /p1/c1 {jcr:primaryType = test:Child, test:childName = name2}
            //    /p2 {jcr:primaryType=test:Parent, test:parentName=name2}
            assertEquals(2, count);
        } finally {
            session.logout();
        }
    }

    @Test
    public void testSearchWithJoinOrMissText() throws Exception {
        String sql = "SELECT * "
                + "FROM [test:Parent] as p "
                + "LEFT OUTER JOIN [test:Child] as c ON ISCHILDNODE(c, p) "
                + "WHERE "
                + " contains(p.*, 'iddqd') or contains(c.*, 'iddqd')";

        Session session = repository.login();

        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(sql, Query.JCR_SQL2);
            QueryResult result = q.execute();
            RowIterator rit = result.getRows();
            int count = 0;
            while(rit.hasNext()) {
                Row r = rit.nextRow();
                System.out.println(">>" + r.getNode("p"));
                count++;
            }

            assertEquals(0, count);
        } finally {
            session.logout();
        }
    }

    @Test
    public void testSearchWithoutJoinOr() throws Exception {
        String sql = "SELECT * "
                + "FROM [test:Parent] as p "
                + "WHERE "
                + " contains(p.*, 'name2')";

        Session session = repository.login();

        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(sql, Query.JCR_SQL2);
            QueryResult result = q.execute();
            RowIterator rit = result.getRows();
            int count = 0;
            while(rit.hasNext()) {
                Row r = rit.nextRow();
                System.out.println(">>" + r.getNode("p"));
                count++;
            }

            assertEquals(1, count);
        } finally {
            session.logout();
        }
    }

    @Test
    public void testSearchWithJoinAnd() throws Exception {
        String sql = "SELECT * "
                + "FROM [test:Parent] as p "
                + "LEFT OUTER JOIN [test:Child] as c ON ISCHILDNODE(c, p) "
                + "WHERE "
                + " contains(p.*, 'name2') and contains(c.*, 'name2')";

        Session session = repository.login();

        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(sql, Query.JCR_SQL2);
            QueryResult result = q.execute();
            RowIterator rit = result.getRows();
            int count = 0;
            while(rit.hasNext()) {
                Row r = rit.nextRow();
                System.out.println(">>" + r.getNode("p"));
                count++;
            }

            assertEquals(0, count);
        } finally {
            session.logout();
        }
    }

    private void initData() throws Exception {
        Session session = repository.login();
        try {
            Node root = session.getRootNode();
            Node p1 = root.addNode("p1", "test:Parent");
            p1.setProperty("test:parentName", "name1");
            Node p2 = root.addNode("p2", "test:Parent");
            p2.setProperty("test:parentName", "name2");
            Node p3 = root.addNode("p3", "test:Parent");
            p3.setProperty("test:parentName", "name3");

            Node c1 = p1.addNode("c1", "test:Child");
            c1.setProperty("test:childName", "name2");

            session.save();
        } finally {
            session.logout();
        }
    }
}
