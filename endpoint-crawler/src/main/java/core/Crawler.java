package core;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


@AllArgsConstructor
@Slf4j
public class Crawler {
	
	private static final String PREFIX_LIST = "" + 
			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
			"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
	
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
	
	private Model createEmptyModel() {
		return ModelFactory.createDefaultModel();
	}
	
	private Model executeAsConstruct(String query) {
		return createQuery(query).execConstruct();
	}
	
	private String normalize(Resource r) {
		return "<" + r + ">";
	}
	
	public Set<Resource> listInstanceOf(Resource clazz) {
		if ( !exists(clazz) ) {
			log.info("{} does not exist in {}.", normalize(clazz), endpointURL);
			return Collections.emptySet();
		}
		
		val query = (
				"construct { ?x rdf:type @c . } " + 
				"where { ?x rdf:type/rdfs:subClassOf* @c . }"
				).replace("@c", normalize(clazz) );
		
		return createQuery(query).execConstruct().listSubjects().toSet();
	}
	
	public Model listDirectPathFrom(Resource subject) {
		val query = (
				"construct { @s ?p ?o . } where { @s ?p ?o . }"
				).replace( "@s", normalize(subject) );
		
		return createQuery(query).execConstruct();
	}

	public Model tracePropertyPathFrom(Resource subject, int depth) {
		log.debug("depth = {}, {}", depth, subject);

		if ( depth <= 0 ) {
			return createEmptyModel(); 
		}
		
		val result = listDirectPathFrom( subject );
		val rest   = result.listObjects().toSet().stream()
				.filter( o -> o.isURIResource() )
				.map   ( o -> tracePropertyPathFrom(o.asResource(), depth - 1) )
				.reduce( createEmptyModel(), (a, b) -> a.add(b) );

		return result.add(rest);
	}
	
	public Model tracePropertyPath(Collection<Resource> subjects, int depth) {
		return subjects.stream()
				.map   ( subject -> tracePropertyPathFrom(subject, depth)      )
				.reduce( createEmptyModel(), (a, b) -> a.add(b) );
	}
	
	public boolean exists(Resource resource) {
		val r = normalize( resource );
		val query = (
				"ask { { @r ?p ?o. } union { ?s @r ?o . } union {?s ?p @r . } }"
				).replace("@r", r);
		
		return createQuery(query).execAsk();
	}

	public Set<Property> listProperty(Model model) {
		return model.listStatements().toSet().stream()
		.map    ( stmt -> stmt.getPredicate() )
		.collect( Collectors.toSet() );
	}

	public Model inferSuperProperty(Resource property) {
		log.debug("infer super-property of {}", property);

		val query = (
				"construct { @p rdfs:subPropertyOf ?super . } " + 
				"where { @p rdfs:subPropertyOf ?super . }"
				).replace("@p", normalize(property) );

		val result = executeAsConstruct(query);

		val rest = result.listObjects().toSet().stream()
				.filter( o -> o.isURIResource() )
				.map   ( o -> inferSuperProperty(o.asResource()) )
				.reduce( createEmptyModel(), (a, b) -> a.add(b) );
		
		return result.add(rest);
	}

}
