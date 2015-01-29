package core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

@AllArgsConstructor
@Slf4j
public class Crawler {

	private static final String LS = System.lineSeparator();
	private static final String PREFIX_LIST = ""
			+ "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + LS
			+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> " + LS;

	private final String endpointURL;

	@VisibleForTesting
	public String insertPrefixList(String query) {
		return PREFIX_LIST + query;
	}

	@VisibleForTesting
	public QueryExecution createQuery(String query) {
		log.trace(query);

		val withPrefix = insertPrefixList(query);

		return QueryExecutionFactory.sparqlService(endpointURL, withPrefix);
	}

	private Model executeAsConstruct(String query) {
		return createQuery(query).execConstruct();
	}

	private String normalize(Resource r) {
		return "<" + r + ">";
	}
	
	public Model inferSubClassOf(Resource superClass) {
		return tracePathReversely(RDFS.subClassOf, superClass);
	}
	
	public boolean exists(Resource resource) {
		val query = "ask { { @r ?p ?o. } union { ?s @r ?o . } union {?s ?p @r . } }"
				.replace("@r", normalize(resource));

		return createQuery(query).execAsk();
	}

	private Set<Resource> listInstanceIn(Model model) {
		val subjects = model.listSubjects().toSet();
		val objects  = model.listObjects().toSet().stream()
				.filter ( o -> o.isURIResource() )
				.map    ( o -> o.asResource()    )
				.collect( Collectors.toSet()     );
		
		return Sets.union(subjects, objects);
	}
	
	public Model tracePropertyPathFrom(Resource subject, int depth) {
		log.debug("depth = {}, {}", depth, subject);

		if (depth <= 0) {
			return ModelFactory.createDefaultModel();
		}

		val result = listDirectPathFrom(subject);
		val rest = result.listObjects().toSet().stream()
				.filter( o -> o.isURIResource() )
				.map   ( o -> o.asResource() )
				.map   ( o -> tracePropertyPathFrom(o, depth - 1) )
				.reduce( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );

		return result.add(rest);
	}
	
	private Model listDirectPathFrom(Resource subject) {
		val query = "construct { @s ?p ?o . } where { @s ?p ?o . filter(isURI(?o)) }"
				.replace("@s", normalize(subject));

		return createQuery(query).execConstruct();
	}

	private Model inferSuperPropertyOf(Resource subProperty) {
		return tracePath(subProperty, RDFS.subPropertyOf);
	}

	private Model inferSuperClassOf(Resource subClass) {
		return tracePath(subClass, RDFS.subClassOf);
	}
	
	// subClass, subProperty の検索だけで使うので，filter(isURI(?o)) は(今のところ)必要ない
	private Model tracePath(Resource base, Property property) {
		val query =
				"construct { @s @p ?o . } where { @s @p ?o . }"
				.replace("@s", normalize(base)    )
				.replace("@p", normalize(property));

		val result = executeAsConstruct(query);
		val rest = result.listObjects().toSet().stream()
				.filter( o -> o.isURIResource() )
				.map   ( o -> o.asResource())
				.map   ( o -> tracePath(o, property) )
				.reduce( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );

		return result.add(rest);
	}
	
	// subClass, subProperty の検索だけで使うので，filter(isURI(?o)) は(今のところ)必要ない
	private Model tracePathReversely(Property property, Resource base) { 
		val query = "construct { ?s @p @o . } where { ?s @p @o . }"
				.replace("@p", normalize(property))
				.replace("@o", normalize(base)    );

		val result = executeAsConstruct(query);
		val rest = result.listSubjects().toSet().stream()
				.filter( s -> s.isURIResource() )
				.map   ( s -> s.asResource() )
				.map   ( s -> tracePathReversely(property, s) )
				.reduce( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );

		return result.add(rest);
	}
	
	private Model inferDomainOf(Resource p) {
		val query = concat(
				"CONSTRUCT {",
				"   @p rdfs:domain ?c",
				"} WHERE {",
				"   @p rdfs:domain ?c",
				"}"
				).replace("@p", normalize(p));
		
		return executeAsConstruct(query);
	}
	
	private Model inferRangeOf(Resource p) {
		val query = concat(
				"CONSTRUCT {",
				"   @p rdfs:range ?c",
				"} WHERE {",
				"   @p rdfs:range ?c",
				"}"
				).replace("@p", normalize(p));

		return executeAsConstruct(query);
	}

	@SuppressWarnings("unchecked")
	public Model extractPropertyInfo(Model model) {
		val properties = listPropertyIn(model);
		log.info("infer property info for {} properties", properties.size());
		
		val domains = properties.stream().map( p -> inferDomainOf(p) );
		val ranges  = properties.stream().map( p -> inferRangeOf(p) );

		return toModel(domains, ranges);
	}
	
	@SuppressWarnings("unchecked")
	private Model toModel(Stream<Model>...streams) {
		val all = Arrays.stream(streams)
				.reduce(Stream.empty(), (a, b) -> Stream.concat(a, b));
		
		return all.reduce(ModelFactory.createDefaultModel(), (a, b) -> a.add(b));
	}
	
	private Set<Resource> listPropertyIn(Model model) {
		val query = concat(
				PREFIX_LIST,
				"SELECT DISTINCT ?p {",
				"   { ?x ?p                 ?y } UNION",
				"   { ?p rdfs:subPropertyOf ?x } UNION",
				"   { ?x rdfs:subPropertyOf ?p } UNION",
				"   { ?p rdfs:domain        ?x } UNION",
				"   { ?p rdfs:range         ?x }      ",

				"   FILTER(",
				"      !strstarts( str(?p), str(rdf:)  ) &&",
				"      !strstarts( str(?p), str(rdfs:) )",
				"   )",
				"}"
				);
				
		
		val results = QueryExecutionFactory.create(query, model).execSelect();
		
		val properties = new LinkedList<Resource>();
		while ( results.hasNext() ) {
			val p = results.next().getResource("?p");
			properties.add(p);
		}

		return new HashSet<>(properties);
	}
	
	public Model inferSuperPropertyIn(Model model) {
		val properties = listPropertyIn(model);
		log.info("infer super property for {} properties.", properties.size());

		return properties.stream()
				.map   ( p -> inferSuperPropertyOf(p) )
				.reduce( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );
	}

	public Model extractTypeIn(Model model) {
		val instances = listInstanceIn(model);
		log.info("extract type for {} instances.", instances.size());
		
		return instances.stream()
				.map   ( i -> extractTypeOf(i) )
				.reduce( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );
	}
	
	private Model extractTypeOf(Resource instance) {
		val query = "construct { @i rdf:type ?c } where { @i rdf:type ?c }"
				.replace("@i", normalize(instance));
		
		return executeAsConstruct(query);		
	}
	
	private String concat(String... lines) {
		val bulider = new StringBuilder();
		
		for ( final String l : lines ) {
			bulider.append(l).append(LS);
		}
		
		return bulider.toString();
	}
	
	private Set<Resource> listClassIn(Model model) {
		val query = concat(
				PREFIX_LIST,
				"SELECT DISTINCT ?c {",
				"   { ?x rdf:type        ?c } UNION",
				"   { ?c rdfs:subClassOf ?x } UNION",
				"   { ?x rdfs:subClassOf ?c } UNION",
				"   { ?x rdfs:domain     ?c } UNION",
				"   { ?x rdfs:range      ?c }      ",
				"}"
				);
		
		val results = QueryExecutionFactory.create(query, model).execSelect();
		
		val classes = new LinkedList<Resource>();
		while ( results.hasNext() ) {
			val c = results.next().getResource("?c");
			classes.add(c);
		}

		return new HashSet<>(classes);
	}
	
	public Model inferSuperClassIn(Model model) {
		val classes = listClassIn(model);
		log.info("infer super class for {} classes.", classes.size());

		return classes.stream()
				.map   ( c -> inferSuperClassOf(c) )
				.reduce( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );
	}

}
