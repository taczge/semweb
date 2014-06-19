package core;

import static core.IsModel.modelOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import lombok.val;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class CrawlerTest {
	
	private Model endpointMock;
	private Crawler sut;

	private static final String NS = "http://www.example.org/";
	
	private static final Resource a = createResource("a");
	private static final Resource b = createResource("b");
	private static final Resource c = createResource("c");
	private static final Resource d = createResource("d");
	private static final Resource e = createResource("e");
	private static final Resource f = createResource("f");
	private static final Resource g = createResource("g");
	
	private static final Resource i = createResource("i");
	private static final Resource j = createResource("j");
	private static final Resource k = createResource("k");
	
	private static final Resource x = createResource("x");
	private static final Resource y = createResource("y");
	private static final Resource z = createResource("z");
	
	private static final Property p = createProperty("p");
	private static final Property q = createProperty("q");
	private static final Property r = createProperty("r");
	private static final Property s = createProperty("s");
	
	@Before
	public void setUp() {
		endpointMock = ModelFactory.createDefaultModel();
		sut = new Crawler("mock of endpoint") {
			@Override
			public QueryExecution createQuery(String query) {
				super.createQuery(query);
				return QueryExecutionFactory.create(
						insertPrefixList(query), endpointMock);
			}
		};
	}

	private static Resource createResource(String str) {
		return ResourceFactory.createResource( NS + str );
	}
	
	private static Property createProperty(String str) {
		return ResourceFactory.createProperty( NS + str );
	}

	@Test
	public void listInstanceOf() throws Exception {
		endpointMock.add(c, RDFS.subClassOf, d);
		endpointMock.add(i, RDF.type, c);
		endpointMock.add(j, RDF.type, c);
		endpointMock.add(k, RDF.type, d);

		val expected = Sets.newHashSet(i, j, k);

		assertThat( sut.listInstanceOf(d), is(expected) );
	}

	@Test
	public void tracePropertyPathFrom() throws Exception {
		endpointMock.add(a, p, b);
		endpointMock.add(b, p, c);
		endpointMock.add(c, p, d);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(a, p, b);
		expected.add(b, p, c);
		
		assertThat( sut.tracePropertyPathFrom(a, 2), is(modelOf(expected)) );
	}

	@Test
	public void tracePropertyPath() throws Exception {
		endpointMock.add(a, p, b);
		endpointMock.add(b, p, c);
		endpointMock.add(c, p, d);
		endpointMock.add(x, q, y);
		endpointMock.add(y, q, z);
		
		val subjects = Sets.newHashSet(a, x);
		val expected = ModelFactory.createDefaultModel();
		expected.add(a, p, b);
		expected.add(b, p, c);
		expected.add(x, q, y);
		expected.add(y, q, z);
		
		assertThat( sut.tracePropertyPath(subjects, 2), is(modelOf(expected)) );
	}

	@Test
	public void existsResource() throws Exception {
		endpointMock.add(a, p, b);
				
		assertThat( sut.exists(a), is(true) );
		assertThat( sut.exists(p), is(true) );
		assertThat( sut.exists(b), is(true) );
		assertThat( sut.exists(x), is(false) );
	}
	
	@Test
	public void inferSuperProperty() throws Exception {
		endpointMock.add(p, RDFS.subPropertyOf, q);
		endpointMock.add(q, RDFS.subPropertyOf, r);
		endpointMock.add(r, RDFS.subPropertyOf, s);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(q, RDFS.subPropertyOf, r);
		expected.add(r, RDFS.subPropertyOf, s);
		
		assertThat( sut.inferSuperPropertyOf(q), is(modelOf(expected)) );
	}
	
	@Test
	public void listPropertyInfo_inferSuperProperties() throws Exception {
		val model = ModelFactory.createDefaultModel();
		model.add(a, p, b);
		
		endpointMock.add(p, RDFS.subPropertyOf, q);
		endpointMock.add(q, RDFS.subPropertyOf, r);
		endpointMock.add(s, RDFS.subPropertyOf, r);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.subPropertyOf, q);
		expected.add(q, RDFS.subPropertyOf, r);

		assertThat( sut.listPropertyInfo(model), is(modelOf(expected)) ); 
	}

	@Test
	public void listPropertyInfo_inferDomains() throws Exception {
		val model = ModelFactory.createDefaultModel();
		model.add(a, p, b);
		
		endpointMock.add(p, RDFS.domain, c);
		endpointMock.add(c, RDFS.subClassOf, d);
		endpointMock.add(d, RDFS.subClassOf, e);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.domain, c);
		expected.add(c, RDFS.subClassOf, d);
		expected.add(d, RDFS.subClassOf, e);

		assertThat( sut.listPropertyInfo(model), is(modelOf(expected)) ); 
	}

	@Test
	public void listPropertyInfo_inferRanges() throws Exception {
		val model = ModelFactory.createDefaultModel();
		model.add(a, p, b);
		
		endpointMock.add(p, RDFS.range, c);
		endpointMock.add(c, RDFS.subClassOf, d);
		endpointMock.add(d, RDFS.subClassOf, e);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.range, c);
		expected.add(c, RDFS.subClassOf, d);
		expected.add(d, RDFS.subClassOf, e);

		assertThat( sut.listPropertyInfo(model), is(modelOf(expected)) ); 
	}

	@Test
	public void inferDomainOf() throws Exception {
		endpointMock.add(p, RDFS.domain, c);
		endpointMock.add(c, RDFS.subClassOf, d);
		endpointMock.add(d, RDFS.subClassOf, e);
		endpointMock.add(p, RDFS.domain, f);
		endpointMock.add(q, RDFS.domain, g);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.domain, c);
		expected.add(p, RDFS.domain, f);
		expected.add(c, RDFS.subClassOf, d);
		expected.add(d, RDFS.subClassOf, e);

		assertThat( sut.inferDomainOf(p), is(modelOf(expected)) ); 
	}
	
	@Test
	public void inferRangeOf() throws Exception {
		endpointMock.add(p, RDFS.range, c);
		endpointMock.add(c, RDFS.subClassOf, d);
		endpointMock.add(d, RDFS.subClassOf, e);
		endpointMock.add(p, RDFS.range, f);
		endpointMock.add(q, RDFS.range, g);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.range, c);
		expected.add(p, RDFS.range, f);
		expected.add(c, RDFS.subClassOf, d);
		expected.add(d, RDFS.subClassOf, e);

		assertThat( sut.inferRangeOf(p), is(modelOf(expected)) ); 
	}
}
