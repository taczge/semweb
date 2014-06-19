package core;

import java.util.Arrays;
import java.util.HashSet;
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
	public static final
	Crawler DBPEDIA = new Crawler("http://dbpedia.org/sparql");

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
	
	public Model listInstanceOf(Model model) {
		val subClasses = model.listSubjectsWithProperty(RDFS.subClassOf).toSet();
		val superClasses = model.listObjectsOfProperty(RDFS.subClassOf).toSet().stream()
				.filter ( n -> n.isURIResource() )
				.map    ( n -> n.asResource()    )
				.collect( Collectors.toSet()     );
		
		val classes = new HashSet<Resource>( subClasses.size() + superClasses.size() );
		classes.addAll( subClasses );
		classes.addAll( superClasses );
		
		return classes.stream()
				.map( c -> listInstanceOf(c) )
				.reduce( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );
	}
	
	public Model listInstanceOf(Resource clazz) {
		val query = "construct { ?x rdf:type @c . } where { ?x rdf:type @c . }"  
				.replace("@c", normalize(clazz));

		return executeAsConstruct(query);
	}
	
	public boolean exists(Resource resource) {
		val query = "ask { { @r ?p ?o. } union { ?s @r ?o . } union {?s ?p @r . } }"
				.replace("@r", normalize(resource));

		return createQuery(query).execAsk();
	}

	public Model extractClassInfo(Resource clazz) {
		if ( !exists(clazz) ) {
			log.info("{} does not exist in {}.", normalize(clazz), endpointURL);

			return ModelFactory.createDefaultModel();
		}

		log.info("{} exists in {}.", normalize(clazz), endpointURL);
		
		val classHierarchy   = inferSubClassOf(clazz);
		val explictInstances = listInstanceOf(clazz);
		val implictInstances = listInstanceOf(classHierarchy); 
		
		val info = ModelFactory.createDefaultModel();
		info.add(classHierarchy);
		info.add(explictInstances);
		info.add(implictInstances);

		return info;
	}
	
	public Set<Resource> listInstanceIn(Model model) {
		val subjects = model.listSubjects().toSet();
		val objects  = model.listObjects().toSet().stream()
				.filter ( o -> o.isURIResource() )
				.map    ( o -> o.asResource()    )
				.collect( Collectors.toSet()     );
		
		return Sets.union(subjects, objects);
	}
	
	public Model extractPropertyPath(Model model, int depth) {
		val instances = listInstanceIn(model);
		
		return instances.stream()
				.map   ( i -> tracePropertyPathFrom(i, depth) )
				.reduce( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );
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
	
	public Model listDirectPathFrom(Resource subject) {
		val query = "construct { @s ?p ?o . } where { @s ?p ?o . }"
				.replace("@s", normalize(subject));

		return createQuery(query).execConstruct();
	}

	public Model inferSuperPropertyOf(Resource subProperty) {
		return tracePath(subProperty, RDFS.subPropertyOf);
	}

	public Model inferSubPropertyOf(Resource superProperty) {
		return tracePathReversely(RDFS.subPropertyOf, superProperty);
	}

	public Model inferSuperClassOf(Resource subClass) {
		return tracePath(subClass, RDFS.subClassOf);
	}
	
	public Model tracePath(Resource base, Property property) {
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
	
	public Model tracePathReversely(Property property, Resource base) { 
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
		
	public Model inferSuperClassOfObjectIn(Model model) {
		return model.listObjects().toSet().stream() 
				.filter ( o -> o.isURIResource() )
				.map    ( o -> o.asResource())
				.map    ( o -> inferSuperClassOf(o) )
				.reduce ( ModelFactory.createDefaultModel(), (a, b) -> a.add(b) );
	}
	
	public Model inferDomainOf(Property p) {
		val query = 
				"construct { @p rdfs:domain ?c . } where { @p rdfs:domain ?c . }"
				.replace("@p", normalize(p));
		
		val domains = executeAsConstruct(query);
		val classes = inferSuperClassOfObjectIn(domains);  
		
		return domains.add(classes);
	}
	
	public Model inferRangeOf(Property p) {
		val query = 
				"construct { @p rdfs:range ?c . } where { @p rdfs:range ?c . }"
				.replace("@p", normalize(p));

		val ranges  = executeAsConstruct(query);
		val classes = inferSuperClassOfObjectIn(ranges);
		
		return ranges.add(classes);
	}

	@SuppressWarnings("unchecked")
	public Model extractPropertyInfo(Model model) {
		val properties = model.listStatements().toSet().stream()
				.map( stmt -> stmt.getPredicate() )
				.collect( Collectors.toSet() );

		val subs        = properties.stream().map( p -> inferSubPropertyOf(p) );
		val supers      = properties.stream().map( p -> inferSuperPropertyOf(p) );
		val domains     = properties.stream().map( p -> inferDomainOf(p) );
		val ranges      = properties.stream().map( p -> inferRangeOf(p) );
		
		return toModel(subs, supers, domains, ranges);
	}
	
	@SuppressWarnings("unchecked")
	private Model toModel(Stream<Model>...streams) {
		val all = Arrays.stream(streams)
				.reduce(Stream.empty(), (a, b) -> Stream.concat(a, b));
		
		return all.reduce(ModelFactory.createDefaultModel(), (a, b) -> a.add(b));
	}

}
