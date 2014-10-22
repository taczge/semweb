package path;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@Slf4j
public class Main {

	private static final String DBPEDIA_URL = "http://dbpedia.org/sparql";

	private Model extract(String subject, int depth) {
		val s = ResourceFactory.createResource(subject);
		System.out.println(s + ", " + depth);
		
		return extract(s, depth);
	}
	
	private Model execConstruct(Resource subject) {
		val query = ("" +
				"construct { <@s> ?p ?o . } " + 
				"where { <@s> ?p ?o . filter( isURI(?o) && ?p != <http://www.w3.org/2002/07/owl#sameAs> ) } ").
				replaceAll("@s", subject.toString());
		
		val exec = QueryExecutionFactory.sparqlService(DBPEDIA_URL, query);
		
		return exec.execConstruct();
	}
	
	public Model extract(Resource s, int depth) {
		log.trace( "dep: " + depth + ", " + s);
		val model = execConstruct(s);
		
		if ( depth <= 1 ) {
			return model;
		}

		val union = ModelFactory.createDefaultModel();
		union.add(model);
		for (StmtIterator iter = model.listStatements(); iter.hasNext(); ) {
			val object = iter.next().getObject();
			
			if ( !object.isURIResource() ) {
				continue;
			}
			
			val m = extract(object.asResource(), depth - 1);
			union.add(m);
		}
		
		return union;
	}
	
	private void exec(List<String> languages, int depth) {
		val prefix = "http://dbpedia.org/resource/";
		languages.forEach( language -> {
			val m = extract(prefix + language, depth);
			write(m, "/home/tn/ttl/" + shorten(language) + "_" + depth);
		});
	}
	
	// Java_(programming_language) -> Java
	private String shorten(String language) {
		return language.replace("_(programming_language)", "");
	}
	
	private void write(Model model, String outFile) {
		try( FileWriter out = new FileWriter( outFile ) ) { 
			model.write(out, "N-TRIPLE" );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		val languages = new LinkedList<String>();

		languages.add("Java_(programming_language)");
		languages.add("C_(programming_language)");
		languages.add("C++");
		languages.add("Python_(programming_language)");
		languages.add("C_Sharp_(programming_language)");
		languages.add("PHP");
		languages.add("JavaScript"); // ??
		languages.add("Ruby_(programming_language)");
		languages.add("R_(programming_language)");
		languages.add("MATLAB");
		languages.add("Perl");
		languages.add("SQL");
		languages.add("Assembly_language");
		languages.add("HTML"); // yet
		languages.add("Visual_Basic");
		languages.add("Objective-C");
		languages.add("Scala_(programming_language)");
		languages.add("Shell_scripting_language");
		languages.add("Arduino");
		languages.add("Go_(programming_language)");

		val depth = 1;

		new Main().exec(languages, depth);
	}
}
